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
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.util.WorldFlush;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.logging.Level;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BackupProfileImpl implements BackupProfile {

    private final BuildSystemPlugin plugin;
    private final ConfigService configService;
    private final Messages messages;
    private final WorldServiceImpl worldService;
    private final SpawnService spawnService;
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
            ConfigService configService,
            Messages messages,
            WorldServiceImpl worldService,
            SpawnService spawnService,
            Supplier<BackupStorage> storage,
            BuildWorld buildWorld) {
        this.plugin = plugin;
        this.configService = configService;
        this.messages = messages;
        this.worldService = worldService;
        this.spawnService = spawnService;
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
                            ignored -> {
                                this.buildWorld.getWorld().ifPresent(WorldFlush::saveAndFlush);
                                return storeWithRetention();
                            },
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
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(event));
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
        // level-name directory, which since Paper 26.1 holds every other world under dimensions/minecraft, so the
        // wipe would take the whole server with it. Bukkit also refuses to unload the default world, so the unload
        // that is supposed to precede the wipe silently does nothing.
        if (Bukkit.getWorlds().getFirst().equals(world)) {
            messages.sendMessage(player, "worlds_backup_restore_default_world");
            return CompletableFuture.completedFuture(null);
        }

        List<@Nullable Player> removedPlayers =
                worldService.removePlayersFromWorld(worldName, "worlds_backup_restoration_in_progress");

        // Download off the main thread, then apply the restore back on the main thread. Blocking the
        // download here would freeze the entire server for the duration of a remote (S3/SFTP) fetch.
        return this.storage
                .get()
                .downloadBackup(backup)
                .thenCompose(backupFile -> CompletableFuture.runAsync(
                        () -> {
                            try {
                                applyRestore(backup, player, world, worldName, removedPlayers, backupFile);
                            } catch (IOException e) {
                                throw new CompletionException(e);
                            }
                        },
                        mainThreadExecutor()))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger()
                                .log(Level.SEVERE, "Failed to restore backup for world " + worldName, throwable);
                    }
                });
    }

    /**
     * Applies a downloaded backup to the world. Must run on the main thread: it unloads, wipes and reloads the world
     * and fires Bukkit events.
     */
    private void applyRestore(
            Backup backup,
            Player player,
            World world,
            String worldName,
            List<@Nullable Player> removedPlayers,
            File backupFile)
            throws IOException {
        Location spawn = spawnService.getSpawn();
        boolean isSpawn = spawn != null && Objects.equals(spawn.getWorld(), world);

        File targetDirectory = FileUtils.worldFolder(worldName);

        // Must happen before the world is deleted: a corrupt archive would otherwise only be detected once there
        // was nothing left to restore.
        validateBackup(backupFile, targetDirectory);

        this.buildWorld.getUnloader().forceUnload(SaveBehavior.DISCARD);
        try {
            FileUtils.deleteDirectory(targetDirectory);
        } catch (IOException e) {
            // Extracting over a half-deleted world would produce a corrupt mix of both.
            throw new IOException(
                    "Aborting restore: failed to delete world directory %s".formatted(targetDirectory), e);
        }

        if (!targetDirectory.isDirectory() && !targetDirectory.mkdirs()) {
            throw new IOException("Failed to create world directory for restore: " + targetDirectory.getAbsolutePath());
        }
        extractBackup(backupFile, targetDirectory);

        this.buildWorld.getLoader().load();
        WorldTeleporter worldTeleporter = this.buildWorld.getTeleporter();
        removedPlayers.stream().filter(Objects::nonNull).forEach(worldTeleporter::teleport);

        if (isSpawn) {
            spawn.setWorld(Bukkit.getWorld(worldName));
            spawnService.set(spawn, worldName);
        }

        Bukkit.getPluginManager().callEvent(new BackupRestoredEvent(this.buildWorld, backup));

        messages.sendMessage(
                player,
                "worlds_backup_restoration_successful",
                Placeholders.of("%timestamp%", messages.formatDateTime(backup.creationTime())));
    }

    /**
     * Checks that {@code backupFile} is a readable archive and that no entry's resolved path escapes
     * {@code targetDirectory} (zip-slip / path traversal).
     *
     * <p>Called before the world is deleted, so a corrupt archive fails the restore while the world is still
     * intact. Reading the central directory is what detects truncation.
     *
     * @param backupFile The downloaded archive
     * @param targetDirectory The directory the archive would be extracted into
     * @throws IOException If the archive cannot be read or an entry escapes the target directory
     */
    private void validateBackup(File backupFile, File targetDirectory) throws IOException {
        try (ZipFile zip = new ZipFile(backupFile)) {
            List<FileHeader> headers = zip.getFileHeaders();
            if (headers.isEmpty()) {
                throw new IOException(
                        "Refusing to restore backup: archive contains no entries: %s".formatted(backupFile));
            }

            for (FileHeader header : headers) {
                File resolved = new File(targetDirectory, header.getFileName());
                if (StringCleaner.isPathEscape(targetDirectory, resolved)) {
                    throw new IOException("Refusing to restore backup: archive entry escapes the world directory: %s"
                            .formatted(header.getFileName()));
                }
            }
        }
    }

    /**
     * Extracts a backup archive into {@code targetDirectory}. Entries are validated by
     * {@link #validateBackup(File, File)} before the world is deleted.
     */
    private void extractBackup(File backupFile, File targetDirectory) throws IOException {
        try (ZipFile zip = new ZipFile(backupFile)) {
            zip.extractAll(targetDirectory.getPath());
        }
    }

    /**
     * Returns an {@link Executor} that runs tasks on the server main thread, where Bukkit world and event operations
     * must happen.
     */
    private Executor mainThreadExecutor() {
        return task -> Bukkit.getScheduler().runTask(plugin, task);
    }
}
