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
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.util.WorldFlush;
import de.eintosti.buildsystem.world.lifecycle.WorldOperations;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BackupProfileImpl implements BackupProfile {

    private final BuildSystemPlugin plugin;
    private final TaskScheduler scheduler;
    private final ConfigService configService;
    private final Messages messages;
    private final WorldOperations operations;
    private final Supplier<BackupStorage> storage;
    private final BuildWorld buildWorld;

    /**
     * Guards {@link #pendingCreation}. Only the hand-off is synchronized; the backup itself runs off-lock, serialized by
     * the future chain instead.
     */
    private final Object creationLock = new Object();

    /**
     * The tail of this world's backup-creation chain. Retention trims the oldest archives from a listing taken at the
     * start of a run, so two overlapping runs would read the same listing, delete the same archives twice and both
     * overshoot the cap. Chaining makes a world's backups strictly sequential.
     */
    private CompletableFuture<@Nullable Backup> pendingCreation = CompletableFuture.completedFuture(null);

    public BackupProfileImpl(
            BuildSystemPlugin plugin,
            TaskScheduler scheduler,
            ConfigService configService,
            Messages messages,
            WorldOperations operations,
            Supplier<BackupStorage> storage,
            BuildWorld buildWorld) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.configService = configService;
        this.messages = messages;
        this.operations = operations;
        this.storage = storage;
        this.buildWorld = buildWorld;
    }

    @Override
    public CompletableFuture<List<Backup>> listBackups() {
        return this.storage.get().listBackups(this.buildWorld);
    }

    @Override
    public CompletableFuture<Backup> createBackup() {
        synchronized (this.creationLock) {
            // handle() before the compose: a failed backup must not poison every later backup of this world.
            // Saving is main-thread-only and this is public API, so it must not run on the caller's thread. The save
            // also waits for the chunk writer: without that the archive can catch region files mid-write.
            CompletableFuture<Backup> next = this.pendingCreation
                    .handle((backup, throwable) -> null)
                    .thenComposeAsync(
                            ignored -> operations.runExclusively(this.buildWorld, () -> {
                                Optional<World> world = this.buildWorld.getWorld();
                                world.ifPresent(WorldFlush::saveAndPauseWrites);
                                return storeWithRetention()
                                        .whenCompleteAsync(
                                                (backup, throwable) -> world.ifPresent(WorldFlush::resumeWrites),
                                                mainThreadExecutor());
                            }),
                            mainThreadExecutor());
            this.pendingCreation = next.handle((backup, throwable) -> backup);
            return next;
        }
    }

    /**
     * Stores the new archive, announces it, then deletes the archives it pushed over the retention cap. A backup that
     * fails to store deletes nothing.
     */
    private CompletableFuture<Backup> storeWithRetention() {
        BackupStorage backupStorage = this.storage.get();
        return backupStorage
                .listBackups(this.buildWorld)
                .thenCompose(backups -> backupStorage
                        .storeBackup(this.buildWorld)
                        .thenCompose(backup -> {
                            fireEventSync(new BackupCreatedEvent(buildWorld, backup));
                            return deleteExcess(backupStorage, backups).thenApply(ignored -> backup);
                        }));
    }

    /**
     * Deletes the oldest archives in {@code backups}, taken before the new one was stored, that it pushed over
     * {@link PluginConfig.World.Backup#maxBackupsPerWorld() the per-world cap}.
     */
    private CompletableFuture<Void> deleteExcess(BackupStorage backupStorage, List<Backup> backups) {
        int maxBackups = configService.current().world().backup().maxBackupsPerWorld();
        int excess = backups.size() - maxBackups + 1;
        if (excess <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] deletions = backups.stream()
                .sorted(Comparator.comparingLong(Backup::creationTime))
                .limit(excess)
                .map(backup -> backupStorage
                        .deleteBackup(backup)
                        .thenRun(() -> fireEventSync(new BackupDeletedEvent(buildWorld, backup))))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(deletions);
    }

    /**
     * Backup futures complete on async threads, but Bukkit events must be fired on the main thread.
     */
    private void fireEventSync(Event event) {
        scheduler.run(() -> Bukkit.getPluginManager().callEvent(event));
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

        // Restoring wipes the world folder before extracting. For the server's default world that folder is the
        // level-name directory, which since Paper 26.1 holds every other world under dimensions/, so the
        // wipe would take the whole server with it. Bukkit also refuses to unload the default world, so the unload
        // that is supposed to precede the wipe silently does nothing.
        if (Bukkit.getWorlds().getFirst().equals(world)) {
            messages.sendMessage(player, "worlds_backup_restore_default_world");
            return CompletableFuture.completedFuture(null);
        }

        // Only taking the world offline and loading it again happen on the main thread. The download, the
        // extraction and the folder swap run in the background, so a large world does not freeze the server.
        WorldRestore restore = new WorldRestore(FileUtils.worldFolder(worldName));
        return operations
                .runExclusively(
                        this.buildWorld,
                        () -> this.storage
                                .get()
                                .downloadBackup(backup)
                                .thenAcceptAsync(
                                        backupFile -> unchecked(() -> restore.stage(backupFile)),
                                        scheduler.background())
                                // The players are only moved once the backup is here and extracted, so a failed
                                // download leaves them in place.
                                .thenApplyAsync(
                                        ignored -> operations.takeOffline(
                                                this.buildWorld,
                                                "worlds_backup_restoration_in_progress",
                                                SaveBehavior.DISCARD),
                                        mainThreadExecutor())
                                .thenApplyAsync(
                                        removedPlayers -> {
                                            try {
                                                restore.swap();
                                                return new Swapped(removedPlayers, null, true);
                                            } catch (IOException e) {
                                                return new Swapped(removedPlayers, e, restore.worldIsBack());
                                            }
                                        },
                                        scheduler.background()))
                .thenAcceptAsync(swapped -> reloadRestoredWorld(backup, player, restore, swapped), mainThreadExecutor())
                .whenCompleteAsync(
                        (ignored, throwable) -> {
                            scheduler.background().execute(() -> {
                                try {
                                    restore.cleanUp();
                                } catch (IOException e) {
                                    plugin.getLogger().log(Level.WARNING, "Could not clean up after a restore", e);
                                }
                            });
                            if (throwable != null && !operations.reportRefusal(player, worldName, throwable)) {
                                plugin.getLogger()
                                        .log(
                                                Level.SEVERE,
                                                "Failed to restore backup for world " + worldName,
                                                throwable);
                                messages.sendMessage(player, "worlds_backup_restoration_failed");
                            }
                        },
                        mainThreadExecutor());
    }

    /**
     * The outcome of moving a backup into place, taken back to the main thread.
     *
     * @param failure Why the swap failed, or {@code null} if the backup is in place
     * @param worldIsBack Whether the world folder holds a whole world, the backup or the old one put back
     */
    private record Swapped(
            List<Player> removedPlayers, @Nullable IOException failure, boolean worldIsBack) {}

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static void unchecked(IoAction action) {
        try {
            action.run();
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    /**
     * Loads the world again and brings its players back, unless a failed swap could not put the old world back. Runs on
     * the main thread, right after the restore has released the world.
     */
    private void reloadRestoredWorld(Backup backup, Player player, WorldRestore restore, Swapped swapped) {
        @Nullable IOException failure = swapped.failure();
        if (!swapped.worldIsBack()) {
            // Loading now would generate an empty world in the old world's place.
            throw new CompletionException(
                    new IOException("The world stays unloaded, its old folder is " + restore.replaced(), failure));
        }

        this.buildWorld.getLoader().load();
        WorldTeleporter worldTeleporter = this.buildWorld.getTeleporter();
        swapped.removedPlayers().forEach(worldTeleporter::teleport);
        if (failure != null) {
            throw new CompletionException(failure);
        }

        Bukkit.getPluginManager().callEvent(new BackupRestoredEvent(this.buildWorld, backup));

        messages.sendMessage(
                player,
                "worlds_backup_restoration_successful",
                Placeholders.of("%timestamp%", messages.formatDateTime(backup.creationTime())));
    }

    /**
     * Returns an {@link Executor} that runs tasks on the server main thread, where Bukkit world and event operations
     * must happen.
     */
    private Executor mainThreadExecutor() {
        return scheduler.mainThread();
    }
}
