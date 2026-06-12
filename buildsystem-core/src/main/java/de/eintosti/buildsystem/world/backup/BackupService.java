/*
 * Copyright (c) 2023-2025, Thomas Meaney
 * All rights reserved.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */
package de.eintosti.buildsystem.world.backup;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.data.Type;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.world.backup.storage.LocalBackupStorage;
import de.eintosti.buildsystem.world.backup.storage.S3BackupStorage;
import de.eintosti.buildsystem.world.backup.storage.SftpBackupStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@NullMarked
public class BackupService {

    private static final long UPDATE_PERIOD = Duration.ofSeconds(5).getSeconds();

    private final BuildSystemPlugin plugin;
    private final ExecutorService executor;
    private BackupStorage backupStorage;
    private final WorldStorage worldStorage;

    private final Cache<UUID, BackupProfile> backupProfileCache =
            CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.MINUTES).build();

    @Nullable
    private BukkitTask autoBackupTask;

    public BackupService(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newCachedThreadPool();

        PluginConfig.World.Backup backupConfig =
                plugin.getConfigService().current().world().backup();
        this.backupStorage = createStorage(plugin, executor, backupConfig.storage());
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        if (backupConfig.autoBackup().enabled()) {
            this.autoBackupTask = Bukkit.getScheduler()
                    .runTaskTimer(plugin, this::incrementTimeSinceBackup, UPDATE_PERIOD * 20, UPDATE_PERIOD * 20);
        }
    }

    private static BackupStorage createStorage(
            BuildSystemPlugin plugin, ExecutorService executor, PluginConfig.World.Backup.StorageSettings settings) {
        return switch (settings) {
            case PluginConfig.World.Backup.Local l -> new LocalBackupStorage(plugin, executor);
            case PluginConfig.World.Backup.Sftp s ->
                new SftpBackupStorage(plugin, executor, s.host(), s.port(), s.username(), s.password(), s.path());
            case PluginConfig.World.Backup.S3 s3 ->
                new S3BackupStorage(
                        plugin,
                        executor,
                        s3.url(),
                        s3.accessKey(),
                        s3.secretKey(),
                        s3.region(),
                        s3.bucket(),
                        s3.path());
        };
    }

    /**
     * Reloads the auto-backup scheduler based on the current {@link PluginConfig.World.Backup.AutoBackup} configuration. If auto-backup was previously running and is now disabled, the task is cancelled. If
     * auto-backup was not running and is now enabled, a new task is started.
     */
    public void reload() {
        if (autoBackupTask != null) {
            autoBackupTask.cancel();
            autoBackupTask = null;
        }
        this.backupStorage.close();
        PluginConfig.World.Backup backupConfig =
                plugin.getConfigService().current().world().backup();
        this.backupStorage = createStorage(plugin, executor, backupConfig.storage());
        if (backupConfig.autoBackup().enabled() && plugin.isEnabled()) {
            this.autoBackupTask = Bukkit.getScheduler()
                    .runTaskTimer(plugin, this::incrementTimeSinceBackup, UPDATE_PERIOD * 20, UPDATE_PERIOD * 20);
        }
    }

    public BackupStorage getStorage() {
        return this.backupStorage;
    }

    public void close() {
        this.backupStorage.close();
        this.executor.shutdown();
    }

    /**
     * Increments the time since a {@link BuildWorld} was backed-up by {@link #UPDATE_PERIOD} seconds. If the time has surpassed {@link AutoBackup#interval}, a backup will
     * automatically be created.
     */
    private void incrementTimeSinceBackup() {
        Set<BuildWorld> worlds = new HashSet<>();

        PluginConfig.World.Backup.AutoBackup autoBackup =
                plugin.getConfigService().current().world().backup().autoBackup();
        if (autoBackup.onlyActiveWorlds()) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                BuildWorld buildWorld = worldStorage.getBuildWorld(pl.getWorld().getName());
                if (buildWorld != null && buildWorld.getPermissions().canModify(pl, Type.TRUE)) {
                    worlds.add(buildWorld);
                }
            }
        } else {
            worlds.addAll(worldStorage.getBuildWorlds());
        }

        worlds.forEach(buildWorld -> {
            Type<Integer> timeSinceBackup = buildWorld.getData().timeSinceBackup();
            timeSinceBackup.set((int) (timeSinceBackup.get() + UPDATE_PERIOD));

            if (timeSinceBackup.get() > autoBackup.interval()) {
                getProfile(buildWorld).createBackup();
                timeSinceBackup.set(0);
            }
        });
    }

    /**
     * Performs a backup of the {@link BuildWorld}.
     *
     * @param buildWorld The world to create a backup of
     * @param onSuccess  Action to run if backup was successful
     * @param onFailure  Action to run if backup failed
     */
    public void backup(BuildWorld buildWorld, Runnable onSuccess, Runnable onFailure) {
        getProfile(buildWorld).createBackup().whenComplete((backup, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().log(Level.SEVERE, "Backup failed", throwable);
                Bukkit.getScheduler().runTask(plugin, onFailure);
            } else {
                Bukkit.getScheduler().runTask(plugin, onSuccess);
            }
        });
    }

    /**
     * Gets the backup profile for a {@link BuildWorld}.
     *
     * @param buildWorld The world to get the backup profile for
     * @return The backup profile
     */
    public BackupProfile getProfile(BuildWorld buildWorld) {
        try {
            return this.backupProfileCache.get(
                    buildWorld.getUniqueId(), () -> new BackupProfileImpl(this.plugin, this.backupStorage, buildWorld));
        } catch (ExecutionException e) {
            BackupProfileImpl profile = new BackupProfileImpl(this.plugin, this.backupStorage, buildWorld);
            this.backupProfileCache.put(buildWorld.getUniqueId(), profile);
            return profile;
        }
    }
}
