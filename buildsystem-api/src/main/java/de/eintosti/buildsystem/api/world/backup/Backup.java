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

/**
 * Represents a single backup of a {@link de.eintosti.buildsystem.api.world.BuildWorld}.
 */
public interface Backup {

    /**
     * Returns the {@link BackupProfile} that owns this backup.
     *
     * @return The owner of the backup.
     */
    BackupProfile owner();

    /**
     * Returns the timestamp when this backup was created.
     *
     * @return The creation time in milliseconds since the Unix epoch.
     */
    long creationTime();

    /**
     * Returns a unique key or identifier for this backup.
     *
     * @return The key of the backup.
     */
    String key();
}
