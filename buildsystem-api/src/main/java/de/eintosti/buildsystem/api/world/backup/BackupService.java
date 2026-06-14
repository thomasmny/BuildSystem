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
import org.jspecify.annotations.NullMarked;

/**
 * Entry point for working with world backups.
 *
 * <p>Each {@link BuildWorld} has exactly one {@link BackupProfile}, obtained via {@link #getProfile(BuildWorld)},
 * through which backups can be listed, created and restored. The concrete storage backend (local disk, SFTP or S3) is
 * configured by the server owner and is transparent to API consumers.
 *
 * @since TODO
 */
@NullMarked
public interface BackupService {

    /**
     * Returns the {@link BackupProfile} for the given world, creating it if one does not yet exist.
     *
     * @param buildWorld The world whose backup profile is requested
     * @return The backup profile for {@code buildWorld}, never {@code null}
     * @since TODO
     */
    BackupProfile getProfile(BuildWorld buildWorld);
}
