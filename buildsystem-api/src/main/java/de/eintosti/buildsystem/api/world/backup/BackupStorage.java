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
package de.eintosti.buildsystem.api.world.backup;

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a storage mechanism for managing world backups.
 */
public interface BackupStorage {

    /**
     * Generates a unique backup name based on a given timestamp.
     *
     * @param timestamp The timestamp to use for the backup name.
     * @return A string representing the backup name (e.g., "1678886400000.zip").
     */
    default String getBackupName(long timestamp) {
        return timestamp + ".zip";
    }

    /**
     * Lists all available {@link Backup}s for a specific {@link BuildWorld}.
     *
     * @param buildWorld The world for which to list backups.
     * @return A list of backup objects associated with the specified world.
     */
    List<Backup> listBackups(BuildWorld buildWorld);

    /**
     * Creates and stores a new {@link Backup} for a given {@link BuildWorld}. The result of the operation is communicated via the provided {@link CompletableFuture}.
     * <p>
     * In comparison to {@link BackupProfile#createBackup()}, a backup will always be created and no older backups will be deleted. This method is intended for immediate backup
     * creation and storage, rather than profile management.
     *
     * @param buildWorld The world to be backed up.
     * @param future     A future that will be completed with the backup object upon successful storage, or exceptionally if an error occurs.
     */
    void storeBackup(BuildWorld buildWorld, CompletableFuture<Backup> future);

    /**
     * Downloads a specific {@link Backup} file.
     *
     * @param backup The backup object representing the backup to be downloaded
     * @return A file object pointing to the downloaded backup
     */
    File downloadBackup(Backup backup);

    /**
     * Deletes a specific {@link Backup}.
     *
     * @param backup The backup object representing the backup to be deleted
     */
    void deleteBackup(Backup backup);

    /**
     * Closes the backup storage, releasing any resources.
     */
    void close();
}
