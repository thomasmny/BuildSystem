/*
 * Copyright (c) 2023-2025, Thomas Meaney
 * All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.eintosti.buildsystem.world.backup;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.api.world.util.WorldTeleporter;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.world.SpawnManager;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import net.lingala.zip4j.ZipFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BackupProfileImpl implements BackupProfile {

    private final BuildSystemPlugin plugin;
    private final BackupStorage storage;
    private final BuildWorld buildWorld;
    protected final Object backupLock;

    public BackupProfileImpl(BuildSystemPlugin plugin, BackupStorage storage, BuildWorld buildWorld) {
        this.plugin = plugin;
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
        World world = this.buildWorld.getWorld();
        if (world != null) {
            world.save();
        }

        CompletableFuture<Backup> resultFuture = new CompletableFuture<>();
        this.listBackups()
                .thenComposeAsync(backups -> {
                    synchronized (this.backupLock) {
                        int maxBackups = Config.World.Backup.maxBackupsPerWorld;
                        int excess = backups.size() - maxBackups + 1;

                        List<CompletableFuture<Void>> deleteFutures = Collections.emptyList();

                        if (excess > 0) {
                            deleteFutures = backups.stream()
                                    .sorted(Comparator.comparingLong(Backup::creationTime))
                                    .limit(excess)
                                    .map(storage::deleteBackup)
                                    .toList();
                        }

                        return CompletableFuture
                                .allOf(deleteFutures.toArray(new CompletableFuture[0]))
                                .thenCompose(v -> storage.storeBackup(this.buildWorld));
                    }
                })
                .whenComplete((backup, throwable) -> {
                    if (throwable != null) {
                        resultFuture.completeExceptionally(throwable);
                    } else {
                        resultFuture.complete(backup);
                    }
                });

        return resultFuture;
    }

    @Override
    public void restoreBackup(Backup backup, Player player) {
        String worldName = this.buildWorld.getName();
        World world = this.buildWorld.getWorld();
        if (world == null) {
            Messages.sendMessage(player, "worlds_backup_unknown_world");
            return;
        }

        List<@Nullable Player> removedPlayers = plugin.getWorldService().removePlayersFromWorld(worldName, "worlds_backup_restoration_in_progress");

        File backupFile;
        try {
            backupFile = this.storage.downloadBackup(backup).get();
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while downloading backup", e);
            return;
        }

        SpawnManager spawnManager = plugin.getSpawnManager();
        Location spawn = spawnManager.getSpawn();
        boolean isSpawn = spawnManager.spawnExists() && spawnManager.getSpawnWorld().equals(world);

        this.buildWorld.getUnloader().forceUnload(false);
        FileUtils.deleteDirectory(new File(Bukkit.getWorldContainer(), worldName));

        try (ZipFile zip = new ZipFile(backupFile)) {
            zip.extractAll(FileUtils.resolve(Bukkit.getWorldContainer().toPath(), this.buildWorld.getName()).toString());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to restore backup at: " + backupFile.getAbsolutePath());
            return;
        }

        this.buildWorld.getLoader().load();
        WorldTeleporter worldTeleporter = this.buildWorld.getTeleporter();
        removedPlayers.stream().filter(Objects::nonNull).forEach(worldTeleporter::teleport);

        if (isSpawn) {
            spawn.setWorld(Bukkit.getWorld(worldName));
            spawnManager.set(spawn, worldName);
        }

        Messages.sendMessage(player, "worlds_backup_restoration_successful",
                Map.entry("%timestamp%", StringUtils.formatTime(backup.creationTime()))
        );
    }
}
