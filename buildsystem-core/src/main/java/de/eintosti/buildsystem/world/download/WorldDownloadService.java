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
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * {@code downloads} directory is served. Archives and their tokens are dropped together — on expiry, on reload and on
 * shutdown — so an export never outlives its link.
 */
@NullMarked
public final class WorldDownloadService {

    private static final String CONTEXT_PATH = "/download/";
    private static final int TOKEN_BYTES = 32;
    private static final int HTTP_THREADS = 2;
    private static final long PURGE_INTERVAL_TICKS = Duration.ofMinutes(1).toSeconds() * 20L;

    private final ConfigService configService;
    private final TaskScheduler scheduler;
    private final Logger logger;
    private final File downloadFolder;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Download> downloads = new ConcurrentHashMap<>();

    private @Nullable HttpServer server;
    private @Nullable ExecutorService httpExecutor;
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

        ExecutorService executor = Executors.newFixedThreadPool(HTTP_THREADS, threadFactory());
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(config.port()), 0);
            httpServer.createContext(CONTEXT_PATH, this::handle);
            httpServer.setExecutor(executor);
            httpServer.start();
            this.server = httpServer;
            this.httpExecutor = executor;
        } catch (IOException e) {
            executor.shutdownNow();
            logger.log(Level.SEVERE, "Failed to start the world download server on port " + config.port(), e);
            return;
        }

        this.purgeTask = scheduler.runTimer(this::purgeExpired, PURGE_INTERVAL_TICKS, PURGE_INTERVAL_TICKS);
        logger.info("World downloads are available on port " + config.port());
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
        downloads.clear();
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

        File worldFolder = FileUtils.worldFolder(buildWorld.getName());
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No main level is loaded"));
        }
        File defaultLevelFolder = worlds.getFirst().getWorldFolder();

        String worldName = buildWorld.getName();
        String token = generateToken();
        Path archive = new File(downloadFolder, token + ".zip").toPath();
        long expiresAt = System.currentTimeMillis()
                + Duration.ofMinutes(getExpirationMinutes()).toMillis();

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        WorldExporter.export(worldName, worldFolder, defaultLevelFolder, archive);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    downloads.put(token, new Download(archive, WorldExporter.fileName(worldName) + ".zip", expiresAt));
                    return url(token);
                },
                scheduler.background());
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respondEmpty(exchange, 405);
                return;
            }

            Download download = downloads.get(token(exchange));
            if (download == null || download.isExpired() || !Files.isRegularFile(download.file())) {
                respondEmpty(exchange, 404);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders()
                    .set("Content-Disposition", "attachment; filename=\"" + download.fileName() + "\"");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, Files.size(download.file()));

            try (OutputStream out = exchange.getResponseBody()) {
                Files.copy(download.file(), out);
            }
        } catch (IOException e) {
            // A client that disconnects mid-download is routine and must not spam the console.
            logger.log(Level.FINE, "World download aborted", e);
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

    private void purgeExpired() {
        downloads.values().removeIf(download -> {
            if (!download.isExpired()) {
                return false;
            }
            delete(download.file());
            return true;
        });
    }

    private String url(String token) {
        String baseUrl = config().url();
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + CONTEXT_PATH + token;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
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

    private record Download(Path file, String fileName, long expiresAt) {

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
