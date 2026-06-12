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
package de.eintosti.buildsystem.world.backup.storage;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.backup.BackupImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class LocalBackupStorage extends AbstractBackupStorage {

    private final Path backupPath;
    private final Function<BuildWorld, BackupProfile> profileProvider;

    public LocalBackupStorage(BuildSystemPlugin plugin, Executor executor) {
        super(plugin, executor);
        this.profileProvider = bw -> plugin.getBackupService().getProfile(bw);
        this.backupPath = plugin.getDataFolder().toPath().resolve("backups");
        if (!Files.exists(backupPath)) {
            try {
                Files.createDirectory(backupPath);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Unable to create backup folder", e);
            }
        }
    }

    /**
     * Package-private constructor for unit tests (no BuildSystemPlugin required).
     */
    LocalBackupStorage(Logger logger, Executor executor, Path backupRoot, Function<BuildWorld, BackupProfile> profileProvider) {
        super(logger, executor);
        this.profileProvider = profileProvider;
        this.backupPath = backupRoot;
    }

    @Override
    protected List<Backup> doListBackups(BuildWorld buildWorld) throws IOException {
        Path dir = getBackupDirectory(buildWorld);
        if (!Files.exists(dir)) {
            return new ArrayList<>();
        }
        List<Backup> backups = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(LocalBackupStorage::isZip).forEach(path -> {
                long creationTime = creationTimeOf(path);
                backups.add(new BackupImpl(profileProvider.apply(buildWorld), creationTime, path.toAbsolutePath().toString()));
            });
        }
        return backups;
    }

    private long creationTimeOf(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toMillis();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read attributes from " + path, e);
            return System.currentTimeMillis();
        }
    }

    @Override
    public CompletableFuture<Backup> storeBackup(BuildWorld buildWorld) {
        return supply("store backup for " + buildWorld.getName(), () -> {
            long timestamp = System.currentTimeMillis();
            File storage = new File(getBackupDirectory(buildWorld).toFile(), backupName(timestamp));
            File zip = FileUtils.zipWorld(storage, buildWorld);
            if (zip == null) {
                throw new IOException("Failed to complete the backup for " + buildWorld.getName());
            }
            logDuration(buildWorld, timestamp);
            return new BackupImpl(profileProvider.apply(buildWorld), timestamp, zip.getAbsolutePath());
        });
    }

    @Override
    public CompletableFuture<File> downloadBackup(Backup backup) {
        return CompletableFuture.completedFuture(new File(backup.key()));
    }

    @Override
    protected void doDeleteBackup(Backup backup) throws IOException {
        Files.deleteIfExists(Path.of(backup.key()));
    }

    @Override
    public void close() {
        // Nothing to release for local storage
    }

    public Path getBackupDirectory(BuildWorld buildWorld) {
        return this.backupPath.resolve(buildWorld.getUniqueId().toString());
    }

    private static boolean isZip(Path path) {
        return path.getFileName().toString().endsWith(".zip");
    }
}
