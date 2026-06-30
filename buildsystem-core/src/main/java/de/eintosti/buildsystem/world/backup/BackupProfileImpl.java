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
package de.eintosti.buildsystem.world.backup;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.event.backup.BackupCreatedEvent;
import de.eintosti.buildsystem.api.event.backup.BackupDeletedEvent;
import de.eintosti.buildsystem.api.event.backup.BackupRestoredEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.api.world.lifecycle.SaveBehavior;
import de.eintosti.buildsystem.api.world.lifecycle.WorldTeleporter;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BackupProfileImpl implements BackupProfile {

    private final BuildSystemPlugin plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final WorldServiceImpl worldService;
    private final SpawnService spawnService;
    private final BackupStorage storage;
    private final BuildWorld buildWorld;
    protected final Object backupLock;

    public BackupProfileImpl(
            BuildSystemPlugin plugin,
            ConfigService configService,
            Messages messages,
            WorldServiceImpl worldService,
            SpawnService spawnService,
            BackupStorage storage,
            BuildWorld buildWorld) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.worldService = worldService;
        this.spawnService = spawnService;
        this.storage = storage;
        this.buildWorld = buildWorld;
        this.backupLock = new Object();
    }

    @Override
    public CompletableFuture<List<Backup>> listBackups() {
        synchronized (this.backupLock) {
            return this.storage.listBackups(this.buildWorld);
        }
    }

    @Override
    public CompletableFuture<Backup> createBackup() {
        this.buildWorld.getWorld().ifPresent(World::save);

        CompletableFuture<Backup> resultFuture = new CompletableFuture<>();
        this.listBackups()
                .thenComposeAsync(backups -> {
                    synchronized (this.backupLock) {
                        int maxBackups =
                                configService.current().world().backup().maxBackupsPerWorld();
                        int excess = backups.size() - maxBackups + 1;

                        List<CompletableFuture<Void>> deleteFutures = Collections.emptyList();

                        if (excess > 0) {
                            deleteFutures = backups.stream()
                                    .sorted(Comparator.comparingLong(Backup::creationTime))
                                    .limit(excess)
                                    .map(b -> storage.deleteBackup(b)
                                            .thenRun(() -> fireEventSync(new BackupDeletedEvent(buildWorld, b))))
                                    .toList();
                        }

                        return CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]))
                                .thenCompose(v -> storage.storeBackup(this.buildWorld));
                    }
                })
                .whenComplete((backup, throwable) -> {
                    if (throwable != null) {
                        resultFuture.completeExceptionally(throwable);
                    } else {
                        fireEventSync(new BackupCreatedEvent(buildWorld, backup));
                        resultFuture.complete(backup);
                    }
                });

        return resultFuture;
    }

    /**
     * Backup futures complete on async threads, but Bukkit events must be fired on the main thread.
     */
    private void fireEventSync(Event event) {
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
    }

    @Override
    public CompletableFuture<Void> restoreBackup(Backup backup, Player player) {
        String worldName = this.buildWorld.getName();
        Optional<World> optionalWorld = this.buildWorld.getWorld();
        if (optionalWorld.isEmpty()) {
            messages.sendMessage(player, "worlds_backup_unknown_world");
            return CompletableFuture.completedFuture(null);
        }
        World world = optionalWorld.get();

        List<@Nullable Player> removedPlayers =
                worldService.removePlayersFromWorld(worldName, "worlds_backup_restoration_in_progress");

        // Download off the main thread, then apply the restore back on the main thread. Blocking the
        // download here would freeze the entire server for the duration of a remote (S3/SFTP) fetch.
        return this.storage
                .downloadBackup(backup)
                .thenCompose(backupFile -> CompletableFuture.runAsync(
                        () -> {
                            try {
                                applyRestore(backup, player, world, worldName, removedPlayers, backupFile);
                            } catch (IOException e) {
                                throw new CompletionException(e);
                            }
                        },
                        mainThreadExecutor()))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger()
                                .log(Level.SEVERE, "Failed to restore backup for world " + worldName, throwable);
                    }
                });
    }

    /**
     * Applies a downloaded backup to the world. Must run on the main thread: it unloads, wipes and reloads the world
     * and fires Bukkit events.
     */
    private void applyRestore(
            Backup backup,
            Player player,
            World world,
            String worldName,
            List<@Nullable Player> removedPlayers,
            File backupFile)
            throws IOException {
        Location spawn = spawnService.getSpawn();
        boolean isSpawn = spawn != null && Objects.equals(spawn.getWorld(), world);

        this.buildWorld.getUnloader().forceUnload(SaveBehavior.DISCARD);
        File targetDirectory = FileUtils.worldFolder(worldName);
        try {
            FileUtils.deleteDirectory(targetDirectory);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while deleting world directory before restore", e);
        }

        if (!targetDirectory.isDirectory() && !targetDirectory.mkdirs()) {
            throw new IOException("Failed to create world directory for restore: " + targetDirectory.getAbsolutePath());
        }
        extractBackup(backupFile, targetDirectory);

        this.buildWorld.getLoader().load();
        WorldTeleporter worldTeleporter = this.buildWorld.getTeleporter();
        removedPlayers.stream().filter(Objects::nonNull).forEach(worldTeleporter::teleport);

        if (isSpawn) {
            spawn.setWorld(Bukkit.getWorld(worldName));
            spawnService.set(spawn, worldName);
        }

        Bukkit.getPluginManager().callEvent(new BackupRestoredEvent(this.buildWorld, backup));

        messages.sendMessage(
                player,
                "worlds_backup_restoration_successful",
                Map.entry(
                        "%timestamp%",
                        StringUtils.formatTime(
                                backup.creationTime(),
                                configService.current().settings().dateFormat())));
    }

    /**
     * Extracts a backup archive into {@code targetDirectory}, rejecting any entry whose resolved path escapes that
     * directory (zip-slip / path traversal) before anything is written to disk.
     */
    private void extractBackup(File backupFile, File targetDirectory) throws IOException {
        try (ZipFile zip = new ZipFile(backupFile)) {
            for (FileHeader header : zip.getFileHeaders()) {
                File resolved = new File(targetDirectory, header.getFileName());
                if (StringCleaner.isPathEscape(targetDirectory, resolved)) {
                    throw new IOException("Refusing to restore backup: archive entry escapes the world directory: "
                            + header.getFileName());
                }
            }
            zip.extractAll(targetDirectory.getPath());
        }
    }

    /**
     * Returns an {@link Executor} that runs tasks on the server main thread, where Bukkit world and event operations
     * must happen.
     */
    private Executor mainThreadExecutor() {
        return task -> Bukkit.getScheduler().runTask(plugin, task);
    }
}
