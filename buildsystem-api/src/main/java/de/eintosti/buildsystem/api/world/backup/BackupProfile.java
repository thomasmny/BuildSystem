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
package de.eintosti.buildsystem.api.world.backup;

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a profile for managing backups of a specific {@link BuildWorld}. This interface defines operations related
 * to listing, creating, restoring, and destroying backups.
 *
 * @since TODO
 */
@NullMarked
public interface BackupProfile {

    /**
     * Asynchronously populates a list of available {@link Backup}s under this profile.
     *
     * @return Future that will be completed with available backups
     */
    CompletableFuture<List<Backup>> listBackups();

    /**
     * Creates a backup of the {@link BuildWorld}. If the profile is at the maximum backup capacity, the oldest backup
     * will be deleted.
     *
     * @return Future that completes with the created backup.
     */
    CompletableFuture<Backup> createBackup();

    /**
     * Restores a {@link Backup}, replacing the world's current state. World and file operations run on the server main
     * thread, so the returned future completes there; it completes exceptionally if the backup cannot be downloaded or
     * extracted.
     *
     * @param backup Backup to restore
     * @param player The player restoring the backup
     * @return A future that completes once the restore has finished
     */
    CompletableFuture<Void> restoreBackup(Backup backup, Player player);
}
