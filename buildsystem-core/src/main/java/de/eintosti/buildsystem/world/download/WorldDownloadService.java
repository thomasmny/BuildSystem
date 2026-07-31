/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.world.download;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serves world exports over HTTP so players can download a world as a single-player save.
 *
 * <p>Off by default. When enabled, an archive is only ever reachable through the unguessable, expiring token it was
 * registered under: the token is the whole URL path, so no request can name a file, and nothing outside the plugin's
 * {@code downloads} directory is served. A link is pinned to the first client that uses it, requests are rate limited
 * per address, and concurrent transfers are capped so downloads cannot crowd each other out. Archives are deleted
 * together with their token — on expiry, on reload and on shutdown.
 */
@NullMarked
public final class WorldDownloadService {

    private static final String CONTEXT_PATH = "/download/";
    private static final int MAX_REQUESTS_PER_WINDOW = 30;
    private static final long RATE_LIMIT_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final long PURGE_INTERVAL_TICKS = Duration.ofMinutes(1).toSeconds() * 20L;

    private final ConfigService configService;
    private final TaskScheduler scheduler;
    private final Logger logger;
    private final File downloadFolder;

    private final DownloadRegistry registry = new DownloadRegistry();
    private final RequestRateLimiter rateLimiter =
            new RequestRateLimiter(MAX_REQUESTS_PER_WINDOW, RATE_LIMIT_WINDOW_MILLIS);

    private @Nullable HttpServer server;
    private @Nullable ExecutorService httpExecutor;
    private @Nullable Semaphore transferSlots;
    private @Nullable BukkitTask purgeTask;

    public WorldDownloadService(ConfigService configService, TaskScheduler scheduler, Logger logger, File dataFolder) {
        this.configService = configService;
        this.scheduler = scheduler;
        this.logger = logger;
        this.downloadFolder = new File(dataFolder, "downloads");
    }

    /**
     * Starts the download server if it is enabled in the config. Any archive left behind by a previous run is deleted:
     * its token did not survive the restart, so the file is unreachable.
     */
    public void start() {
        PluginConfig.World.Download config = config();
        if (!config.enabled()) {
            return;
        }

        clearDownloadFolder();
        if (!downloadFolder.isDirectory() && !downloadFolder.mkdirs()) {
            logger.severe("Failed to create the world download folder: " + downloadFolder.getAbsolutePath());
            return;
        }

        int concurrentDownloads = Math.max(1, config.maxConcurrentDownloads());
        ExecutorService executor = Executors.newFixedThreadPool(concurrentDownloads + 1, threadFactory());
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(config.port()), 0);
            httpServer.createContext(CONTEXT_PATH, this::handle);
            httpServer.setExecutor(executor);
            httpServer.start();
            this.server = httpServer;
            this.httpExecutor = executor;
            this.transferSlots = new Semaphore(concurrentDownloads);
        } catch (IOException e) {
            executor.shutdownNow();
            logger.log(Level.SEVERE, "Failed to start the world download server on port " + config.port(), e);
            return;
        }

        this.purgeTask = scheduler.runTimer(this::purge, PURGE_INTERVAL_TICKS, PURGE_INTERVAL_TICKS);
        logger.info("World downloads are available on port " + config.port());
        warnAboutPlaintext(config);
    }

    /**
     * Stops the download server and deletes every archive it was serving.
     */
    public void stop() {
        if (purgeTask != null) {
            purgeTask.cancel();
            purgeTask = null;
        }
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (httpExecutor != null) {
            httpExecutor.shutdownNow();
            httpExecutor = null;
        }
        transferSlots = null;
        registry.clear();
        rateLimiter.clear();
        clearDownloadFolder();
    }

    /**
     * Applies a changed config by restarting the server, so toggling downloads off takes effect immediately rather
     * than at the next restart.
     */
    public void reload() {
        stop();
        start();
    }

    public boolean isEnabled() {
        return server != null;
    }

    public int getExpirationMinutes() {
        return config().expirationMinutes();
    }

    /**
     * Exports {@code buildWorld} on a background thread and registers it for download.
     *
     * <p>Must be called from the main thread: the world's folder and the main level's are resolved through the server
     * before the export moves off it.
     *
     * @param buildWorld The world to export
     * @return A future completed with the download URL, or completed exceptionally if the export fails
     */
    public CompletableFuture<String> prepare(BuildWorld buildWorld) {
        if (server == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("World downloads are disabled"));
        }

        PluginConfig.World.Download config = config();
        long storageBudget = megabytes(config.maxStorageMb());
        if (registry.totalBytes() >= storageBudget) {
            return CompletableFuture.failedFuture(new StorageFullException());
        }

        File worldFolder = FileUtils.worldFolder(buildWorld.getName());
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No main level is loaded"));
        }
        File defaultLevelFolder = worlds.getFirst().getWorldFolder();

        String worldName = buildWorld.getName();
        long maxArchiveBytes = Math.min(megabytes(config.maxSizeMb()), storageBudget - registry.totalBytes());
        long expiresAt = System.currentTimeMillis()
                + Duration.ofMinutes(config.expirationMinutes()).toMillis();
        Path archive = new File(downloadFolder, worldName.hashCode() + "-" + System.nanoTime() + ".zip").toPath();

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        WorldExporter.export(worldName, worldFolder, defaultLevelFolder, archive, maxArchiveBytes);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    String token = registry.register(archive, WorldExporter.fileName(worldName) + ".zip", expiresAt);
                    return url(token);
                },
                scheduler.background());
    }

    /**
     * Warns when links leave the server unencrypted. A token in a plaintext URL is readable by anything on the path,
     * so the only safe plain-HTTP setup is one that terminates TLS in front of this server.
     */
    private void warnAboutPlaintext(PluginConfig.World.Download config) {
        if (config.url().toLowerCase(Locale.ROOT).startsWith("https://")) {
            return;
        }
        logger.warning("World downloads are served over plain HTTP. Anyone able to observe the traffic can reuse a "
                + "download link. Put the port behind a TLS proxy and point world.download.url at it.");
    }

    private void handle(HttpExchange exchange) throws IOException {
        String clientAddress = clientAddress(exchange);
        try (exchange) {
            if (!rateLimiter.allow(clientAddress)) {
                respondEmpty(exchange, 429);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respondEmpty(exchange, 405);
                return;
            }

            DownloadRegistry.Download download = registry.claim(token(exchange), clientAddress);
            if (download == null || !Files.isRegularFile(download.file())) {
                respondEmpty(exchange, 404);
                return;
            }

            transfer(exchange, download);
        } catch (IOException e) {
            // A client that disconnects mid-download is routine and must not spam the console.
            logger.log(Level.FINE, "World download aborted", e);
        }
    }

    private void transfer(HttpExchange exchange, DownloadRegistry.Download download) throws IOException {
        Semaphore slots = transferSlots;
        if (slots == null || !slots.tryAcquire()) {
            exchange.getResponseHeaders().set("Retry-After", "30");
            respondEmpty(exchange, 503);
            return;
        }

        try {
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders()
                    .set("Content-Disposition", "attachment; filename=\"" + download.fileName() + "\"");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, Files.size(download.file()));

            try (OutputStream out = exchange.getResponseBody()) {
                Files.copy(download.file(), out);
            }
        } finally {
            slots.release();
        }
    }

    /**
     * {@return the requested token, or the empty string for any request shaped differently} The token is the entire
     * path below the context, so a request can neither name a file nor escape the download folder.
     */
    private String token(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        return path.length() > CONTEXT_PATH.length() ? path.substring(CONTEXT_PATH.length()) : "";
    }

    private static String clientAddress(HttpExchange exchange) {
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private void purge() {
        registry.purgeExpired(this::delete);
        rateLimiter.purgeStale();
    }

    private String url(String token) {
        String baseUrl = config().url();
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + CONTEXT_PATH + token;
    }

    private static long megabytes(int megabytes) {
        return megabytes * 1024L * 1024L;
    }

    private PluginConfig.World.Download config() {
        return configService.current().world().download();
    }

    private void clearDownloadFolder() {
        if (!downloadFolder.isDirectory()) {
            return;
        }
        try {
            FileUtils.deleteDirectory(downloadFolder);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to clear the world download folder", e);
        }
    }

    private void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete expired world download " + file, e);
        }
    }

    private static void respondEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    private static ThreadFactory threadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "BuildSystem-Download-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * Thrown when the live archives already fill the configured storage budget.
     */
    public static final class StorageFullException extends IOException {

        StorageFullException() {
            super("The world download storage budget is exhausted");
        }
    }
}
