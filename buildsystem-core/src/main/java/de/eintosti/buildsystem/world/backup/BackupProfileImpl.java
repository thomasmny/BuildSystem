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
import de.eintosti.buildsystem.util.WorldArchive;
import de.eintosti.buildsystem.util.WorldFlush;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldOperations;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private final WorldServiceImpl worldService;
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
            WorldServiceImpl worldService,
            Supplier<BackupStorage> storage,
            BuildWorld buildWorld) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.configService = configService;
        this.messages = messages;
        this.worldService = worldService;
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
                            ignored -> worldService.operations().runExclusively(this.buildWorld, () -> {
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
     * Deletes any archives over the retention cap, stores the new one, and announces it.
     */
    private CompletableFuture<Backup> storeWithRetention() {
        BackupStorage backupStorage = this.storage.get();
        return backupStorage
                .listBackups(this.buildWorld)
                .thenCompose(backups -> deleteExcess(backupStorage, backups))
                .thenCompose(ignored -> backupStorage.storeBackup(this.buildWorld))
                .thenApply(backup -> {
                    fireEventSync(new BackupCreatedEvent(buildWorld, backup));
                    return backup;
                });
    }

    /**
     * Deletes the oldest archives that the incoming backup would push over
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
        File worldFolder = FileUtils.worldFolder(worldName);
        File staged = restoreFolder(worldFolder, "staged");
        File replaced = restoreFolder(worldFolder, "replaced");
        WorldOperations operations = worldService.operations();
        return operations
                .runExclusively(
                        this.buildWorld,
                        () -> this.storage
                                .get()
                                .downloadBackup(backup)
                                .thenAcceptAsync(
                                        backupFile -> unchecked(() -> stage(backupFile, worldFolder, staged)),
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
                                            unchecked(() -> swap(worldFolder, staged, replaced));
                                            return removedPlayers;
                                        },
                                        scheduler.background()))
                .thenAcceptAsync(
                        removedPlayers -> reloadRestoredWorld(backup, player, removedPlayers), mainThreadExecutor())
                .whenCompleteAsync(
                        (ignored, throwable) -> {
                            scheduler.background().execute(() -> {
                                deleteQuietly(staged);
                                if (throwable == null) {
                                    deleteQuietly(replaced);
                                }
                                // Only succeeds once the last restore in this directory is cleaned up.
                                staged.getParentFile().delete();
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
     * {@return a folder for one step of a restore, next to the world folder so moving it in is a rename} The folders
     * sit inside {@code .buildsystem-restore}, which holds no world data itself, so they are never listed as worlds
     * to import.
     */
    private static File restoreFolder(File worldFolder, String step) {
        return new File(worldFolder.getParentFile(), ".buildsystem-restore/" + worldFolder.getName() + "." + step);
    }

    /**
     * Extracts the backup into {@code staged} while the world is still loaded. A broken
     * archive fails here, before anything happens to the world.
     */
    private static void stage(File backupFile, File worldFolder, File staged) throws IOException {
        WorldArchive.validate(backupFile, worldFolder);
        if (staged.exists()) {
            // Left behind by a restore that was interrupted.
            FileUtils.deleteDirectory(staged);
        }
        Files.createDirectories(staged.toPath());
        WorldArchive.extract(backupFile, staged);
    }

    /**
     * Puts the extracted backup in place of the world folder. Both moves are renames on one file system. If the
     * second one fails, the world folder is put back, so the world is never left half replaced.
     */
    private static void swap(File worldFolder, File staged, File replaced) throws IOException {
        if (replaced.exists()) {
            // A restore whose rollback failed left the only copy of the world there.
            throw new IOException("Aborting restore: " + replaced + " is left from an earlier restore, check it first");
        }
        FileUtils.moveDirectory(worldFolder, replaced);
        try {
            FileUtils.moveDirectory(staged, worldFolder);
        } catch (IOException e) {
            FileUtils.moveDirectory(replaced, worldFolder);
            throw e;
        }
    }

    private void deleteQuietly(File directory) {
        if (!directory.exists()) {
            return;
        }
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not delete " + directory, e);
        }
    }

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
     * Loads the restored world and brings its players back. Runs on the main thread, right after the restore has
     * released the world.
     */
    private void reloadRestoredWorld(Backup backup, Player player, List<Player> removedPlayers) {
        this.buildWorld.getLoader().load();
        WorldTeleporter worldTeleporter = this.buildWorld.getTeleporter();
        removedPlayers.forEach(worldTeleporter::teleport);

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
