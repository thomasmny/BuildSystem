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
import de.eintosti.buildsystem.api.world.backup.BackupService;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.storage.LocalBackupStorage;
import de.eintosti.buildsystem.world.backup.storage.S3BackupStorage;
import de.eintosti.buildsystem.world.backup.storage.SftpBackupStorage;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BackupServiceImpl implements BackupService {

    private static final long UPDATE_PERIOD = Duration.ofSeconds(5).getSeconds();

    private final BuildSystemPlugin plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final WorldServiceImpl worldService;
    private final Supplier<SpawnService> spawnService;
    private final ExecutorService executor;
    private BackupStorage backupStorage;
    private final WorldStorage worldStorage;

    private final Cache<UUID, BackupProfile> backupProfileCache =
            CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.MINUTES).build();

    private @Nullable BukkitTask autoBackupTask;

    public BackupServiceImpl(
            BuildSystemPlugin plugin,
            ConfigService configService,
            Messages messages,
            WorldServiceImpl worldService,
            Supplier<SpawnService> spawnService) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.worldService = worldService;
        this.spawnService = spawnService;
        this.executor = Executors.newFixedThreadPool(3);

        PluginConfig.World.Backup backupConfig = configService.current().world().backup();
        this.backupStorage = createStorageOrFallback(backupConfig.storage());
        this.worldStorage = worldService.getWorldStorage();

        if (backupConfig.autoBackup().enabled()) {
            this.autoBackupTask = Bukkit.getScheduler()
                    .runTaskTimer(plugin, this::incrementTimeSinceBackup, UPDATE_PERIOD * 20, UPDATE_PERIOD * 20);
        }
    }

    private BackupStorage createStorage(PluginConfig.World.Backup.StorageSettings settings) {
        Function<BuildWorld, BackupProfile> profileProvider = this::getProfile;
        return switch (settings) {
            case PluginConfig.World.Backup.Local l ->
                new LocalBackupStorage(plugin.getLogger(), executor, plugin.getDataFolder(), profileProvider);
            case PluginConfig.World.Backup.Sftp s -> {
                String password = envOrConfig("BUILDSYSTEM_SFTP_PASSWORD", s.password());
                requireNonBlank(s.host(), "backup.sftp.host");
                requireNonBlank(s.username(), "backup.sftp.username");
                requireNonBlank(password, "backup.sftp.password (or BUILDSYSTEM_SFTP_PASSWORD)");
                yield new SftpBackupStorage(
                        plugin.getLogger(),
                        executor,
                        plugin.getDataFolder(),
                        configService,
                        profileProvider,
                        s.host(),
                        s.port(),
                        s.username(),
                        password,
                        s.path());
            }
            case PluginConfig.World.Backup.S3 s3 -> {
                String accessKey = envOrConfig("AWS_ACCESS_KEY_ID", s3.accessKey());
                String secretKey = envOrConfig("AWS_SECRET_ACCESS_KEY", s3.secretKey());
                requireNonBlank(accessKey, "backup.s3.access-key (or AWS_ACCESS_KEY_ID)");
                requireNonBlank(secretKey, "backup.s3.secret-key (or AWS_SECRET_ACCESS_KEY)");
                requireNonBlank(s3.region(), "backup.s3.region");
                requireNonBlank(s3.bucket(), "backup.s3.bucket");
                yield new S3BackupStorage(
                        plugin.getLogger(),
                        executor,
                        plugin.getDataFolder(),
                        configService,
                        profileProvider,
                        s3.url(),
                        accessKey,
                        secretKey,
                        s3.region(),
                        s3.bucket(),
                        s3.path());
            }
        };
    }

    /**
     * Prefers the environment variable over the config value so operators can keep secrets out of config.yml.
     */
    private static @Nullable String envOrConfig(String envKey, @Nullable String configValue) {
        String env = System.getenv(envKey);
        return env != null && !env.isBlank() ? env : configValue;
    }

    private static void requireNonBlank(@Nullable String value, String configKey) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Backup storage configuration is incomplete: '" + configKey + "' must be set in config.yml");
        }
    }

    private BackupStorage createStorageOrFallback(PluginConfig.World.Backup.StorageSettings settings) {
        try {
            return createStorage(settings);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Backup storage disabled, falling back to local storage: " + e.getMessage());
            return new LocalBackupStorage(plugin.getLogger(), executor, plugin.getDataFolder(), this::getProfile);
        }
    }

    /**
     * Reloads the auto-backup scheduler based on the current {@link PluginConfig.World.Backup.AutoBackup}
     * configuration. If auto-backup was previously running and is now disabled, the task is cancelled. If auto-backup
     * was not running and is now enabled, a new task is started.
     */
    public void reload() {
        if (autoBackupTask != null) {
            autoBackupTask.cancel();
            autoBackupTask = null;
        }
        this.backupStorage.close();
        PluginConfig.World.Backup backupConfig = configService.current().world().backup();
        this.backupStorage = createStorageOrFallback(backupConfig.storage());
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
     * Increments the time since a {@link BuildWorld} was backed-up by {@link #UPDATE_PERIOD} seconds. If the time has
     * surpassed {@link PluginConfig.World.Backup.AutoBackup#interval()}, a backup will automatically be created.
     */
    private void incrementTimeSinceBackup() {
        Set<BuildWorld> worlds = new HashSet<>();

        PluginConfig.World.Backup.AutoBackup autoBackup =
                configService.current().world().backup().autoBackup();
        if (autoBackup.onlyActiveWorlds()) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                BuildWorld buildWorld = worldStorage.getBuildWorld(pl.getWorld().getName());
                if (buildWorld != null && buildWorld.getPermissions().canModify(pl)) {
                    worlds.add(buildWorld);
                }
            }
        } else {
            worlds.addAll(worldStorage.getBuildWorlds());
        }

        worlds.forEach(buildWorld -> {
            WorldData worldData = buildWorld.getData();
            worldData.setTimeSinceBackup((int) (worldData.getTimeSinceBackup() + UPDATE_PERIOD));

            if (worldData.getTimeSinceBackup() > autoBackup.interval()) {
                getProfile(buildWorld).createBackup();
                worldData.setTimeSinceBackup(0);
            }
        });
    }

    /**
     * Performs a backup of the {@link BuildWorld}.
     *
     * @param buildWorld The world to create a backup of
     * @param onSuccess Action to run if backup was successful
     * @param onFailure Action to run if backup failed
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
    @Override
    public BackupProfile getProfile(BuildWorld buildWorld) {
        try {
            return this.backupProfileCache.get(
                    buildWorld.getUniqueId(),
                    () -> new BackupProfileImpl(
                            plugin,
                            configService,
                            messages,
                            worldService,
                            spawnService.get(),
                            this.backupStorage,
                            buildWorld));
        } catch (ExecutionException e) {
            BackupProfileImpl profile = new BackupProfileImpl(
                    plugin, configService, messages, worldService, spawnService.get(), this.backupStorage, buildWorld);
            this.backupProfileCache.put(buildWorld.getUniqueId(), profile);
            return profile;
        }
    }
}
