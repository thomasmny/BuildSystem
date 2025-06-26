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
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import net.lingala.zip4j.ZipFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

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
            return CompletableFuture.supplyAsync(() -> this.storage.listBackups(this, this.buildWorld));
        }
    }

    @Override
    public CompletableFuture<Backup> createBackup() {
        World world = this.buildWorld.getWorld();
        if (world != null) {
            world.save();
        }

        CompletableFuture<Backup> future = new CompletableFuture<>();
        this.listBackups().thenAcceptAsync(backups -> {
            synchronized (this.backupLock) {
                if (backups.size() >= Config.World.Backup.maxBackupsPerWorld) {
                    storage.deleteBackup(backups.getLast());
                }

                this.storage.storeBackup(this, this.buildWorld, future);
            }
        });
        return future;
    }

    @Override
    public void restoreBackup(Backup backup, Player player) {
        File backupFile = this.storage.downloadBackup(backup);
        if (backupFile == null || !Files.exists(backupFile.toPath())) {
            throw new IllegalArgumentException("The specific backup does not exist");
        }

        String worldName = this.buildWorld.getName();
        World world = this.buildWorld.getWorld();
        if (world == null && !this.buildWorld.isLoaded()) {
            this.buildWorld.getLoader().load();
        }

        if (world == null) {
            Messages.sendMessage(player, "worlds_backup_unknown_world");
            return;
        }

        SpawnManager spawnManager = plugin.getSpawnManager();
        Location spawn = spawnManager.getSpawn();
        boolean isSpawn = spawnManager.spawnExists() && spawnManager.getSpawnWorld().equals(world);

        List<Player> removedPlayers = plugin.getWorldService().removePlayersFromWorld(worldName, "worlds_backup_restoration_in_progress");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.buildWorld.getUnloader().forceUnload(false);
        }, 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            FileUtils.deleteDirectory(new File(Bukkit.getWorldContainer(), worldName));

            try (ZipFile zip = new ZipFile(backupFile)) {
                zip.extractAll(Bukkit.getWorldContainer().getAbsolutePath());
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
        }, 60L);
    }

    @Override
    public void destroy() {
        this.listBackups().whenCompleteAsync((backups, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.SEVERE, "Failed to destroy backup", error);
                return;
            }
            backups.forEach(this.storage::deleteBackup);
        });
    }
}
