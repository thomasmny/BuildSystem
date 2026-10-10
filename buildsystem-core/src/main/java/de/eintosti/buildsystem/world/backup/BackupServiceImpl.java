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
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.backup.storage.LocalBackupStorage;
import de.eintosti.buildsystem.world.backup.storage.S3BackupStorage;
import de.eintosti.buildsystem.world.backup.storage.SftpBackupStorage;
import de.eintosti.buildsystem.world.lifecycle.WorldOperationRefusedException;
import de.eintosti.buildsystem.world.lifecycle.WorldOperations;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final TaskScheduler scheduler;
    private final ConfigService configService;
    private final Messages messages;
    private final WorldOperations operations;
    private final ExecutorService executor;
    private final WorldStorage worldStorage;

    /**
     * Kept for as long as the world is registered: a profile holds its world's backup chain, which keeps retention
     * passes from overlapping. A backup still running when its world is deleted keeps its own reference.
     */
    private final Map<UUID, BackupProfile> profiles = new ConcurrentHashMap<>();

    /**
     * Volatile because {@link #reload()} replaces it on the main thread while backup executor threads read it through
     * {@link #getStorage()}.
     */
    private volatile BackupStorage backupStorage;

    private @Nullable BukkitTask autoBackupTask;

    public BackupServiceImpl(
            BuildSystemPlugin plugin,
            TaskScheduler scheduler,
            ConfigService configService,
            Messages messages,
            WorldStorage worldStorage,
            WorldOperations operations) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.configService = configService;
        this.messages = messages;
        this.operations = operations;
        this.executor = Executors.newFixedThreadPool(
                BACKUP_PROFILE_POOL_SIZE,
                Thread.ofPlatform().name("BuildSystem-backup-", 0).daemon().factory());
        this.worldStorage = worldStorage;
        this.backupStorage =
                createStorageOrFallback(configService.current().world().backup().storage());
        plugin.getLogger().info("Storing backups " + describe(this.backupStorage));
        scheduleAutoBackupIfEnabled();
    }

    /**
     * {@return where the given storage puts backups, for the startup log} Says where rather than naming the class, so
     * an operator can tell at a glance whether the configured backend was actually the one that loaded.
     */
    private String describe(BackupStorage storage) {
        PluginConfig current = configService.current();
        PluginConfig.Storage.Type configured = current.world().backup().storage();
        switch (storage) {
            case LocalBackupStorage _ -> {
                String reason = configured == PluginConfig.Storage.Type.LOCAL ? "" : " (configured backend failed)";
                return "locally in plugins/BuildSystem/backups" + reason;
            }
            case S3BackupStorage _ -> {
                PluginConfig.Storage.S3 s3 = current.storage().s3();
                String service = s3.url() == null || s3.url().isBlank() ? "Amazon S3" : s3.url();
                return "on " + service + " in bucket '" + s3.bucket() + "'";
            }
            case SftpBackupStorage _ -> {
                PluginConfig.Storage.Sftp sftp = current.storage().sftp();
                return "over SFTP on " + sftp.host() + ":" + sftp.port();
            }
            default -> {}
        }
        return "using " + storage.getClass().getSimpleName();
    }

    /**
     * Missing credentials never get here: the config parser already falls back to local storage for them.
     */
    private BackupStorage createStorage(PluginConfig.Storage.Type type) {
        PluginConfig current = configService.current();
        String path = current.world().backup().path();
        return switch (type) {
            case LOCAL -> localStorage();
            case SFTP -> {
                PluginConfig.Storage.Sftp sftp = current.storage().sftp();
                String password = sftp.resolvedPassword();
                yield new SftpBackupStorage(
                        plugin.getLogger(),
                        executor,
                        plugin.getDataFolder(),
                        configService,
                        this::getProfile,
                        sftp.host(),
                        sftp.port(),
                        sftp.username(),
                        password,
                        path);
            }
            case S3 -> {
                PluginConfig.Storage.S3 s3 = current.storage().s3();
                String accessKey = s3.resolvedAccessKey();
                String secretKey = s3.resolvedSecretKey();
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
                        path);
            }
        };
    }

    private BackupStorage createStorageOrFallback(PluginConfig.Storage.Type type) {
        try {
            return createStorage(type);
        } catch (IllegalArgumentException e) {
            // A port out of range or a malformed S3 url.
            plugin.getLogger().severe("Backup storage disabled, falling back to local storage: " + e.getMessage());
            return localStorage();
        }
    }

    private LocalBackupStorage localStorage() {
        return new LocalBackupStorage(plugin.getLogger(), executor, plugin.getDataFolder(), this::getProfile);
    }

    private void scheduleAutoBackupIfEnabled() {
        PluginConfig.World.Backup backupConfig = configService.current().world().backup();
        if (backupConfig.autoBackup().enabled() && plugin.isEnabled()) {
            this.autoBackupTask =
                    scheduler.runTimer(this::incrementTimeSinceBackup, UPDATE_PERIOD_TICKS, UPDATE_PERIOD_TICKS);
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
        plugin.getLogger().info("Storing backups " + describe(this.backupStorage));
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
    void incrementTimeSinceBackup() {
        PluginConfig.World.Backup.AutoBackup autoBackup =
                configService.current().world().backup().autoBackup();

        Set<BuildWorld> worlds = new HashSet<>();
        if (autoBackup.onlyActiveWorlds()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
                if (buildWorld != null && buildWorld.getPermissions().canModify(player)) {
                    worlds.add(buildWorld);
                }
            }
        } else {
            worlds.addAll(worldStorage.getBuildWorlds());
        }

        boolean backedUpOneThisTick = false;
        for (BuildWorld buildWorld : worlds) {
            WorldData worldData = buildWorld.getData();
            int elapsed = worldData.get(WorldDataKey.TIME_SINCE_BACKUP) + (int) UPDATE_PERIOD_SECONDS;
            if (elapsed > autoBackup.interval()) {
                // A world another operation holds is backed up on a later tick, once it is free again.
                if (backedUpOneThisTick || operations.isBusy(buildWorld)) {
                    worldData.set(WorldDataKey.TIME_SINCE_BACKUP, elapsed);
                    continue;
                }
                backedUpOneThisTick = true;
                // Reset before starting, since a refusal puts the timer back and may complete right away.
                worldData.set(WorldDataKey.TIME_SINCE_BACKUP, 0);
                autoBackup(buildWorld, autoBackup.interval());
                continue;
            }
            worldData.set(WorldDataKey.TIME_SINCE_BACKUP, elapsed);
        }
    }

    private void autoBackup(BuildWorld buildWorld, int interval) {
        getProfile(buildWorld).createBackup().whenComplete((backup, throwable) -> {
            if (throwable == null) {
                return;
            }
            String worldName = buildWorld.getName();
            if (WorldOperationRefusedException.find(throwable) != null) {
                // Taken by another operation after this tick checked: try again on the next tick.
                buildWorld.getData().set(WorldDataKey.TIME_SINCE_BACKUP, interval);
                plugin.getLogger().info("Skipped the automatic backup of \"" + worldName + "\" while it was busy.");
            } else {
                plugin.getLogger()
                        .log(Level.SEVERE, "Automatic backup failed for world \"" + worldName + "\"", throwable);
            }
        });
    }

    /**
     * Backs up a world for a player and tells them how it went.
     */
    public void backup(Player player, BuildWorld buildWorld) {
        String worldName = buildWorld.getName();
        Placeholders worldPlaceholder = Placeholders.of("%world%", worldName);
        getProfile(buildWorld)
                .createBackup()
                .whenCompleteAsync(
                        (backup, throwable) -> {
                            if (throwable == null) {
                                messages.sendMessage(player, "worlds_backup_created", worldPlaceholder);
                            } else if (!operations.reportRefusal(player, worldName, throwable)) {
                                plugin.getLogger().log(Level.SEVERE, "Backup failed", throwable);
                                messages.sendMessage(player, "worlds_backup_failed", worldPlaceholder);
                            }
                        },
                        scheduler.mainThread());
    }

    @Override
    public BackupProfile getProfile(BuildWorld buildWorld) {
        return profiles.computeIfAbsent(buildWorld.getUniqueId(), uuid -> createProfile(buildWorld));
    }

    /**
     * Drops the profile of a world that was deleted or unimported.
     */
    public void removeProfile(BuildWorld buildWorld) {
        profiles.remove(buildWorld.getUniqueId());
    }

    /**
     * Profiles resolve the storage per operation rather than capturing it, so a {@code /buildsystem reload} that swaps
     * the backend does not leave cached profiles writing into the closed one.
     */
    private BackupProfile createProfile(BuildWorld buildWorld) {
        return new BackupProfileImpl(
                plugin, scheduler, configService, messages, operations, this::getStorage, buildWorld);
    }
}
