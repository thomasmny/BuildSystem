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
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.api.world.data.WorldData.Type;
import de.eintosti.buildsystem.config.Config.World;
import de.eintosti.buildsystem.config.Config.World.Backup;
import de.eintosti.buildsystem.config.Config.World.Backup.AutoBackup;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BackupService {

    private static final long UPDATE_INTERVAL = Duration.ofSeconds(5).getSeconds();

    private final BuildSystemPlugin plugin;
    private final BackupStorage backupStorage;
    private final WorldStorage worldStorage;

    private final Cache<BuildWorldCacheKey, BackupProfile> backupProfileCache = CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.MINUTES).build();

    public BackupService(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.backupStorage = Backup.storage;
        this.worldStorage = plugin.getWorldService().getWorldStorage();

        if (AutoBackup.enabled) {
            Bukkit.getScheduler().runTaskTimer(plugin, this::incrementTimeSinceBackup, UPDATE_INTERVAL * 20, UPDATE_INTERVAL * 20);
        }
    }

    public BackupStorage getStorage() {
        return this.backupStorage;
    }

    /**
     * Increments the time since a {@link BuildWorld} was backed-up by {@link #UPDATE_INTERVAL} seconds. If the time has surpassed {@link World.Backup#backupInterval}, a backup
     * will automatically be created.
     */
    private void incrementTimeSinceBackup() {
        Set<BuildWorld> worlds = new HashSet<>();

        if (AutoBackup.onlyActiveWorlds) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                BuildWorld buildWorld = worldStorage.getBuildWorld(pl.getWorld().getName());
                if (buildWorld != null && buildWorld.getPermissions().canModify(pl, () -> true)) {
                    worlds.add(buildWorld);
                }
            }
        } else {
            worlds.addAll(worldStorage.getBuildWorlds());
        }

        worlds.forEach(buildWorld -> {
            Type<Integer> timeSinceBackup = buildWorld.getData().timeSinceBackup();
            timeSinceBackup.set((int) (timeSinceBackup.get() + UPDATE_INTERVAL));

            if (timeSinceBackup.get() > AutoBackup.interval) {
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
                    new BuildWorldCacheKey(buildWorld),
                    () -> new BackupProfileImpl(this.plugin, this.backupStorage, buildWorld)
            );
        } catch (ExecutionException e) {
            BackupProfileImpl profile = new BackupProfileImpl(this.plugin, this.backupStorage, buildWorld);
            this.backupProfileCache.put(new BuildWorldCacheKey(buildWorld), profile);
            return profile;
        }
    }

    private record BuildWorldCacheKey(BuildWorld buildWorld) {

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            BuildWorldCacheKey that = (BuildWorldCacheKey) other;
            return Objects.equals(buildWorld.getUniqueId(), that.buildWorld.getUniqueId());
        }
    }
}
