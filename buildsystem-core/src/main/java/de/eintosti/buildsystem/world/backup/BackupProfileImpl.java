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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.api.world.lifecycle.WorldTeleporter;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringUtils;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import net.lingala.zip4j.ZipFile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

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
                        int maxBackups = plugin.getConfigService()
                                .current()
                                .world()
                                .backup()
                                .maxBackupsPerWorld();
                        int excess = backups.size() - maxBackups + 1;

                        List<CompletableFuture<Void>> deleteFutures = Collections.emptyList();

                        if (excess > 0) {
                            deleteFutures = backups.stream()
                                    .sorted(Comparator.comparingLong(Backup::creationTime))
                                    .limit(excess)
                                    .map(storage::deleteBackup)
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
                        resultFuture.complete(backup);
                    }
                });

        return resultFuture;
    }

    @Override
    public CompletableFuture<Void> restoreBackup(Backup backup, Player player) {
        String worldName = this.buildWorld.getName();
        World world = this.buildWorld.getWorld();
        if (world == null) {
            plugin.getMessages().sendMessage(player, "worlds_backup_unknown_world");
            return CompletableFuture.completedFuture(null);
        }

        List<@Nullable Player> removedPlayers =
                plugin.getWorldService().removePlayersFromWorld(worldName, "worlds_backup_restoration_in_progress");

        File backupFile;
        try {
            backupFile = this.storage.downloadBackup(backup).get();
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while downloading backup", e);
            return CompletableFuture.failedFuture(e);
        }

        SpawnService spawnService = plugin.getSpawnService();
        Location spawn = spawnService.getSpawn();
        boolean isSpawn = spawn != null && Objects.equals(spawn.getWorld(), world);

        this.buildWorld.getUnloader().forceUnload(false);
        try {
            FileUtils.deleteDirectory(new File(Bukkit.getWorldContainer(), worldName));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while deleting backup file", e);
        }

        try (ZipFile zip = new ZipFile(backupFile)) {
            zip.extractAll(FileUtils.resolve(Bukkit.getWorldContainer(), this.buildWorld.getName())
                    .toString());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to restore backup at: " + backupFile.getAbsolutePath());
            return CompletableFuture.failedFuture(e);
        }

        this.buildWorld.getLoader().load();
        WorldTeleporter worldTeleporter = this.buildWorld.getTeleporter();
        removedPlayers.stream().filter(Objects::nonNull).forEach(worldTeleporter::teleport);

        if (isSpawn) {
            spawn.setWorld(Bukkit.getWorld(worldName));
            spawnService.set(spawn, worldName);
        }

        plugin.getMessages()
                .sendMessage(
                        player,
                        "worlds_backup_restoration_successful",
                        Map.entry(
                                "%timestamp%",
                                StringUtils.formatTime(
                                        backup.creationTime(),
                                        plugin.getConfigService()
                                                .current()
                                                .settings()
                                                .dateFormat())));
        return CompletableFuture.completedFuture(null);
    }
}
