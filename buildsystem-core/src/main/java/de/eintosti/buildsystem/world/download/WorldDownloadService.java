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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Packs a world into a single-player save and hands the player a link to it.
 *
 * <p>Off by default, since either way of serving the archive exposes it outside the game. The packing is the same
 * whichever way that is; where the archive then goes is the {@link DownloadDelivery}'s business —
 * {@link LocalDownloadDelivery the built-in HTTP server} or {@link S3DownloadDelivery a pre-signed S3 link}. Archives
 * are cleared on reload and on shutdown, and each is dropped when its link expires.
 */
@NullMarked
public final class WorldDownloadService {

    private static final long PURGE_INTERVAL_TICKS = Duration.ofMinutes(1).toSeconds() * 20L;

    /**
     * Free space kept clear of the staging archive. The game server writes its own worlds and player data to the same
     * disk, so an export must not be allowed to consume the last byte of it.
     */
    private static final long RESERVED_DISK_BYTES = 1024L * 1024 * 1024;

    private final ConfigService configService;
    private final TaskScheduler scheduler;
    private final Logger logger;
    private final File downloadFolder;

    /**
     * Guards against a slow purge overlapping the next tick of the timer.
     */
    private final AtomicBoolean purging = new AtomicBoolean();

    private @Nullable DownloadDelivery delivery;
    private @Nullable BukkitTask purgeTask;
    private @Nullable ExecutorService exportExecutor;

    /**
     * Bumped by every {@link #stop()}. An export carries the value it started under, so one that outlives a reload is
     * discarded instead of publishing to a delivery that is already closed.
     */
    private volatile int epoch;

    public WorldDownloadService(ConfigService configService, TaskScheduler scheduler, Logger logger, File dataFolder) {
        this.configService = configService;
        this.scheduler = scheduler;
        this.logger = logger;
        this.downloadFolder = new File(dataFolder, "downloads");
    }

    /**
     * Starts world downloads if they are enabled in the config. Any archive left behind by a previous run is deleted:
     * its link did not survive the restart, so the file is unreachable.
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

        this.delivery = switch (config.storage()) {
            case LOCAL -> LocalDownloadDelivery.open(configService, logger);
            case S3 -> S3DownloadDelivery.open(configService, logger, scheduler.background());
            case SFTP -> {
                // Unreachable: the config parser downgrades a backend the feature cannot use to local.
                logger.severe("World downloads cannot be served over SFTP; downloads are disabled.");
                yield null;
            }
        };
        if (delivery == null) {
            return;
        }

        // Exports get their own thread rather than the shared background pool: packing a world holds a thread for
        // minutes, and on that pool it would starve backups and world saves. One at a time also keeps two exports
        // from thrashing the same disk.
        this.exportExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "BuildSystem-Export");
            thread.setDaemon(true);
            return thread;
        });
        this.purgeTask = scheduler.runTimer(this::purge, PURGE_INTERVAL_TICKS, PURGE_INTERVAL_TICKS);
    }

    /**
     * Stops world downloads and drops every archive being served.
     */
    public void stop() {
        epoch++;
        if (purgeTask != null) {
            purgeTask.cancel();
            purgeTask = null;
        }
        if (exportExecutor != null) {
            // shutdown(), not shutdownNow(): a queued export that got dropped would leave its future hanging, and the
            // player who asked for it stuck behind their own "already preparing" guard forever. Queued exports run
            // instead, see the bumped epoch immediately, and fail.
            exportExecutor.shutdown();
            exportExecutor = null;
        }
        if (delivery != null) {
            delivery.close();
            delivery = null;
        }
        clearDownloadFolder();
    }

    /**
     * Applies a changed config by restarting, so toggling downloads off takes effect immediately rather than at the
     * next restart.
     */
    public void reload() {
        stop();
        start();
    }

    public boolean isEnabled() {
        return delivery != null;
    }

    public int getExpirationMinutes() {
        return config().expirationMinutes();
    }

    /**
     * Exports {@code buildWorld} on a background thread and makes it downloadable.
     *
     * <p>Must be called from the main thread: the world's folder and the main level's are resolved through the server
     * before the export moves off it.
     *
     * @param buildWorld The world to export
     * @param progress Notified as the world is packed and published, for showing the player how far along it is
     * @return A future completed with the download URL, or completed exceptionally if the export fails
     */
    public CompletableFuture<String> prepare(BuildWorld buildWorld, DownloadProgress progress) {
        DownloadDelivery target = delivery;
        ExecutorService executor = exportExecutor;
        if (target == null || executor == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("World downloads are disabled"));
        }

        // The archive is staged on this disk whichever delivery takes it, so the free space bounds every export -
        // including one the delivery itself would not limit.
        long usableDisk = downloadFolder.getUsableSpace() - RESERVED_DISK_BYTES;
        if (usableDisk <= 0) {
            logger.warning("Refusing a world export: less than 1 GB is free on the world download disk.");
            return CompletableFuture.failedFuture(new StorageFullException());
        }

        long reserved = target.reserve();
        if (reserved <= 0) {
            return CompletableFuture.failedFuture(new StorageFullException());
        }
        long capacity = Math.min(reserved, usableDisk);

        File worldFolder = FileUtils.worldFolder(buildWorld.getName());
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            target.release(reserved);
            return CompletableFuture.failedFuture(new IllegalStateException("No main level is loaded"));
        }
        File defaultLevelFolder = worlds.getFirst().getWorldFolder();

        String worldName = buildWorld.getName();
        String fileName = WorldExporter.fileName(worldName) + ".zip";
        Path archive = new File(downloadFolder, worldName.hashCode() + "-" + System.nanoTime() + ".zip").toPath();
        int startedUnder = epoch;

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        // Queued behind another export when downloads were stopped: there is nothing to pack for.
                        if (startedUnder != epoch) {
                            throw new IOException("World downloads were restarted before the export began");
                        }

                        WorldExporter.export(
                                worldName,
                                worldFolder,
                                defaultLevelFolder,
                                archive,
                                capacity,
                                (packed, total) -> progress.update(DownloadProgress.Phase.PACKING, packed, total));

                        // Downloads may have been stopped or reloaded while this ran. The delivery captured above is
                        // closed by now and the staging folder has been wiped, so there is nothing left to publish to.
                        if (startedUnder != epoch) {
                            deleteQuietly(archive);
                            throw new IOException("World downloads were restarted while the export was running");
                        }

                        if (target.reportsPublishProgress()) {
                            progress.update(DownloadProgress.Phase.PUBLISHING, 0L, 0L);
                        }
                        // A lifetime, not a deadline: the delivery starts the clock when it hands the link out, so
                        // neither packing nor uploading eats into the window the player was promised.
                        return target.publish(
                                archive,
                                fileName,
                                Duration.ofMinutes(config().expirationMinutes()),
                                (published, total) ->
                                        progress.update(DownloadProgress.Phase.PUBLISHING, published, total));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        target.release(reserved);
                    }
                },
                executor);
    }

    /**
     * Hands the purge to a background thread. Dropping an S3 upload is an HTTPS round trip per object, which on the
     * main thread would stall the tick for as long as the bucket takes to answer.
     */
    private void purge() {
        DownloadDelivery target = delivery;
        if (target == null || !purging.compareAndSet(false, true)) {
            return;
        }
        scheduler.background().execute(() -> {
            try {
                target.purgeExpired();
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Failed to purge expired world downloads", e);
            } finally {
                purging.set(false);
            }
        });
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete the abandoned world export " + file, e);
        }
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

    /**
     * Thrown when the live archives already fill the configured storage budget.
     */
    public static final class StorageFullException extends IOException {

        StorageFullException() {
            super("The world download storage budget is exhausted");
        }
    }
}
