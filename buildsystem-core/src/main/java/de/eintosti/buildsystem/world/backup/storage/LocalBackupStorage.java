/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.backup.BackupImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

public class LocalBackupStorage implements BackupStorage {

    private final BuildSystemPlugin plugin;
    private final Path backupPath;

    public LocalBackupStorage(BuildSystemPlugin plugin) {
        this.plugin = plugin;

        this.backupPath = plugin.getDataFolder().toPath().resolve("backups");
        if (!Files.exists(backupPath)) {
            try {
                Files.createDirectory(backupPath);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Unable to create backup folder", e);
            }
        }
    }

    @Override
    public List<Backup> listBackups(BackupProfile owner, BuildWorld buildWorld) {
        List<Backup> backups = new ArrayList<>(Config.World.Backup.maxBackupsPerWorld);

        try (Stream<Path> walk = Files.walk(getBackupDirectory(buildWorld))) {
            walk.filter(LocalBackupStorage::isValidFile).forEach(path -> backups.add(
                    new BackupImpl(owner, FileUtils.getDirectoryCreation(path.toFile()), path.toAbsolutePath().toString())
            ));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while listing backups", e);
            return Collections.emptyList();
        }

        backups.sort(Comparator.comparingLong(Backup::creationTime).reversed());
        return backups;
    }

    @Override
    public void storeBackup(BackupProfile owner, BuildWorld buildWorld, CompletableFuture<Backup> future) {
        long currentTime = System.currentTimeMillis();
        File storage = new File(getBackupDirectory(buildWorld).toFile(), getBackupName(currentTime));
        File zip = FileUtils.zipWorld(storage, buildWorld);
        if (zip == null) {
            future.completeExceptionally(new RuntimeException("Failed to complete the backup for " + buildWorld.getName()));
            return;
        }
        future.complete(new BackupImpl(owner, currentTime, zip.getAbsolutePath()));
    }

    @Override
    public File downloadBackup(Backup backup) {
        return new File(backup.key());
    }

    @Override
    public void deleteBackup(Backup backup) {
        File file = new File(backup.key());
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to delete backup " + backup.key(), e);
        }
    }

    /**
     * Gets the directory containing the backups for this profile. This directory may not exist.
     *
     * @return Folder that contains the backups for this profile
     */
    public Path getBackupDirectory(BuildWorld buildWorld) {
        return FileUtils.resolve(this.backupPath, buildWorld.getUniqueId().toString());
    }

    private static boolean isValidFile(Path path) {
        return path.getFileName().toString().endsWith(".zip");
    }
}
