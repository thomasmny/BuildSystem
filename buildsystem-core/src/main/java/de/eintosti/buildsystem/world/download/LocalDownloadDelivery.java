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
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Serves archives from the game server itself, over an HTTP server on the configured port.
 *
 * <p>An archive is only ever reachable through the unguessable, expiring token it was registered under: the token is
 * the whole URL path, so no request can name a file, and nothing outside the plugin's {@code downloads} directory is
 * served. A link is pinned to the first client that uses it, requests are rate limited per address, and concurrent
 * transfers are capped so downloads cannot crowd each other out.
 */
@NullMarked
final class LocalDownloadDelivery implements DownloadDelivery {

    private static final String CONTEXT_PATH = "/download/";
    private static final int MAX_REQUESTS_PER_WINDOW = 30;
    private static final long RATE_LIMIT_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    private final ConfigService configService;
    private final Logger logger;

    private final DownloadRegistry registry = new DownloadRegistry();
    private final RequestRateLimiter rateLimiter =
            new RequestRateLimiter(MAX_REQUESTS_PER_WINDOW, RATE_LIMIT_WINDOW_MILLIS);

    private final HttpServer server;
    private final ExecutorService httpExecutor;
    private final Semaphore transferSlots;

    /**
     * Budget claimed by exports that are still packing, and so have no file for the registry to count yet.
     */
    private long reserved;

    private LocalDownloadDelivery(
            ConfigService configService,
            Logger logger,
            HttpServer server,
            ExecutorService httpExecutor,
            int concurrentDownloads) {
        this.configService = configService;
        this.logger = logger;
        this.server = server;
        this.httpExecutor = httpExecutor;
        this.transferSlots = new Semaphore(concurrentDownloads);
    }

    /**
     * {@return a delivery serving on the configured port, or {@code null} if the port could not be bound} The reason
     * is logged, so a failure to start reads as a configuration problem rather than a missing feature.
     *
     * @param configService The live configuration
     * @param logger The plugin logger
     */
    static @Nullable LocalDownloadDelivery open(ConfigService configService, Logger logger) {
        PluginConfig.World.Download config = configService.current().world().download();
        int concurrentDownloads = Math.max(1, config.maxConcurrentDownloads());
        ExecutorService executor = Executors.newFixedThreadPool(concurrentDownloads + 1, threadFactory());

        LocalDownloadDelivery delivery;
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
            delivery = new LocalDownloadDelivery(configService, logger, server, executor, concurrentDownloads);
            server.createContext(CONTEXT_PATH, delivery::handle);
            server.setExecutor(executor);
            server.start();
        } catch (IOException e) {
            executor.shutdownNow();
            logger.log(Level.SEVERE, "Failed to start the world download server on port " + config.port(), e);
            return null;
        }

        logger.info("World downloads are available on port " + config.port());
        warnAboutPlaintext(config, logger);
        return delivery;
    }

    @Override
    public String publish(Path archive, String fileName, Duration lifetime, ProgressListener progress) {
        return url(registry.register(archive, fileName, System.currentTimeMillis() + lifetime.toMillis()));
    }

    /**
     * {@return what is left of the storage budget, capped by the largest single export allowed} Both limits exist
     * because every live archive sits on the game server's own disk until its link expires.
     *
     * <p>Synchronized, and counting reservations that have not produced a file yet, so exports running at the same
     * time divide the budget instead of each being promised all of it.
     */
    @Override
    public synchronized long reserve() {
        PluginConfig.World.Download config = config();
        long remaining = megabytes(config.maxStorageMb()) - registry.totalBytes() - reserved;
        long granted = Math.min(megabytes(config.maxSizeMb()), remaining);
        if (granted > 0) {
            reserved += granted;
        }
        return granted;
    }

    @Override
    public synchronized void release(long amount) {
        if (amount > 0) {
            reserved = Math.max(0L, reserved - amount);
        }
    }

    /**
     * {@return false} Registering a token is instant; the transfer itself happens later, when the player clicks.
     */
    @Override
    public boolean reportsPublishProgress() {
        return false;
    }

    @Override
    public void purgeExpired() {
        registry.purgeExpired(this::delete);
        rateLimiter.purgeStale();
    }

    @Override
    public void close() {
        server.stop(0);
        httpExecutor.shutdownNow();
        registry.clear();
        rateLimiter.clear();
    }

    /**
     * Warns when links leave the server unencrypted. A token in a plaintext URL is readable by anything on the path,
     * so the only safe plain-HTTP setup is one that terminates TLS in front of this server.
     */
    private static void warnAboutPlaintext(PluginConfig.World.Download config, Logger logger) {
        if (config.url().toLowerCase(Locale.ROOT).startsWith("https://")) {
            return;
        }
        logger.warning("World downloads are served over plain HTTP."
                + " Anyone able to observe the traffic can reuse a download link."
                + " Put the port behind a TLS proxy and point world.download.url at it.");
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
            logger.log(Level.FINE, "World download aborted", e);
        }
    }

    private void transfer(HttpExchange exchange, DownloadRegistry.Download download) throws IOException {
        if (!transferSlots.tryAcquire()) {
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
            transferSlots.release();
        }
    }

    /**
     * {@return the requested token, or the empty string for any request shaped differently} The token is the entire
     * path below the context, so a request can neither name a file nor escape the download folder.
     */
    private static String token(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        return path.length() > CONTEXT_PATH.length() ? path.substring(CONTEXT_PATH.length()) : "";
    }

    /**
     * {@return the address a request is attributed to, for pinning and rate limiting}
     *
     * <p>Behind a reverse proxy every request arrives from the proxy, which would pin all links to one identity and
     * pool every player into one rate limit. {@code behind-proxy} switches to the last {@code X-Forwarded-For} entry:
     * the last is the one the proxy itself appended, so unlike the earlier entries a client cannot forge it. Off by
     * default, because trusting the header when the port is reachable directly would let anyone claim any address.
     */
    private String clientAddress(HttpExchange exchange) {
        if (!config().behindProxy()) {
            return exchange.getRemoteAddress().getAddress().getHostAddress();
        }

        List<String> forwarded = exchange.getRequestHeaders().get("X-Forwarded-For");
        if (forwarded != null) {
            for (int i = forwarded.size() - 1; i >= 0; i--) {
                String[] hops = forwarded.get(i).split(",");
                for (int hop = hops.length - 1; hop >= 0; hop--) {
                    String address = hops[hop].trim();
                    if (!address.isEmpty()) {
                        return address;
                    }
                }
            }
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private String url(String token) {
        String baseUrl = config().url();
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + CONTEXT_PATH + token;
    }

    private void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete expired world download " + file, e);
        }
    }

    private PluginConfig.World.Download config() {
        return configService.current().world().download();
    }

    private static long megabytes(int megabytes) {
        return megabytes * 1024L * 1024L;
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
}
