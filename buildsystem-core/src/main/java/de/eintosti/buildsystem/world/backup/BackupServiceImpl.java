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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.api.world.backup.BackupService;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
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
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BackupServiceImpl implements BackupService {

    private static final long UPDATE_PERIOD_SECONDS = Duration.ofSeconds(5).getSeconds();
    private static final long UPDATE_PERIOD_TICKS = UPDATE_PERIOD_SECONDS * 20;
    private static final int BACKUP_PROFILE_POOL_SIZE = 3;

    private final BuildSystemPlugin plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final WorldServiceImpl worldService;
    private final Supplier<SpawnService> spawnService;
    private final ExecutorService executor;
    private final WorldStorage worldStorage;

    private final Cache<UUID, BackupProfile> backupProfileCache =
            CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.MINUTES).build();

    private BackupStorage backupStorage;
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
        this.executor = Executors.newFixedThreadPool(BACKUP_PROFILE_POOL_SIZE);
        this.worldStorage = worldService.getWorldStorage();
        this.backupStorage =
                createStorageOrFallback(configService.current().world().backup().storage());
        scheduleAutoBackupIfEnabled();
    }

    private BackupStorage createStorage(PluginConfig.World.Backup.StorageSettings settings) {
        return switch (settings) {
            case PluginConfig.World.Backup.Local ignored -> localStorage();
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
                        this::getProfile,
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
                        this::getProfile,
                        s3.url(),
                        accessKey,
                        secretKey,
                        s3.region(),
                        s3.bucket(),
                        s3.path());
            }
        };
    }

    private BackupStorage createStorageOrFallback(PluginConfig.World.Backup.StorageSettings settings) {
        try {
            return createStorage(settings);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Backup storage disabled, falling back to local storage: " + e.getMessage());
            return localStorage();
        }
    }

    private LocalBackupStorage localStorage() {
        return new LocalBackupStorage(plugin.getLogger(), executor, plugin.getDataFolder(), this::getProfile);
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

    private void scheduleAutoBackupIfEnabled() {
        PluginConfig.World.Backup backupConfig = configService.current().world().backup();
        if (backupConfig.autoBackup().enabled() && plugin.isEnabled()) {
            this.autoBackupTask = Bukkit.getScheduler()
                    .runTaskTimer(plugin, this::incrementTimeSinceBackup, UPDATE_PERIOD_TICKS, UPDATE_PERIOD_TICKS);
        }
    }

    /**
     * Rebuilds the backup storage and auto-backup task from the current config, so a {@code /buildsystem reload} picks
     * up storage and schedule changes without a restart.
     */
    public void reload() {
        if (autoBackupTask != null) {
            autoBackupTask.cancel();
            autoBackupTask = null;
        }
        this.backupStorage.close();
        this.backupStorage =
                createStorageOrFallback(configService.current().world().backup().storage());
        scheduleAutoBackupIfEnabled();
    }

    public BackupStorage getStorage() {
        return this.backupStorage;
    }

    public void close() {
        this.backupStorage.close();
        this.executor.shutdown();
    }

    /**
     * Adds {@link #UPDATE_PERIOD_SECONDS} to every tracked world's backup timer, backing up and resetting any world that
     * has passed its {@link PluginConfig.World.Backup.AutoBackup#interval() interval}. With {@code onlyActiveWorlds} the
     * tracked set is the worlds players are currently building in, otherwise every world.
     */
    private void incrementTimeSinceBackup() {
        PluginConfig.World.Backup.AutoBackup autoBackup =
                configService.current().world().backup().autoBackup();

        Set<BuildWorld> worlds = new HashSet<>();
        if (autoBackup.onlyActiveWorlds()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                BuildWorld buildWorld =
                        worldStorage.getBuildWorld(player.getWorld().getName());
                if (buildWorld != null && buildWorld.getPermissions().canModify(player)) {
                    worlds.add(buildWorld);
                }
            }
        } else {
            worlds.addAll(worldStorage.getBuildWorlds());
        }

        worlds.forEach(buildWorld -> {
            WorldData worldData = buildWorld.getData();
            int elapsed = worldData.get(WorldDataKey.TIME_SINCE_BACKUP) + (int) UPDATE_PERIOD_SECONDS;
            if (elapsed > autoBackup.interval()) {
                getProfile(buildWorld).createBackup();
                elapsed = 0;
            }
            worldData.set(WorldDataKey.TIME_SINCE_BACKUP, elapsed);
        });
    }

    /**
     * Backs up a world off the main thread, then runs {@code onSuccess} or {@code onFailure} back on it.
     *
     * @param buildWorld The world to back up
     * @param onSuccess Run on the main thread once the backup completes
     * @param onFailure Run on the main thread if the backup fails
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

    @Override
    public BackupProfile getProfile(BuildWorld buildWorld) {
        try {
            return this.backupProfileCache.get(buildWorld.getUniqueId(), () -> createProfile(buildWorld));
        } catch (ExecutionException e) {
            // The loader does not throw, so this is only reached if the cache itself fails; build the profile directly.
            BackupProfile profile = createProfile(buildWorld);
            this.backupProfileCache.put(buildWorld.getUniqueId(), profile);
            return profile;
        }
    }

    private BackupProfile createProfile(BuildWorld buildWorld) {
        return new BackupProfileImpl(
                plugin, configService, messages, worldService, spawnService.get(), this.backupStorage, buildWorld);
    }
}
