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
package de.eintosti.buildsystem.api.storage;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Folder;
import java.util.Collection;
import org.jetbrains.annotations.Unmodifiable;

public interface FolderStorage extends Storage<Folder> {

    /**
     * Gets a list of all {@link Folder}s.
     *
     * @return An unmodifiable list of all folders
     */
    @Unmodifiable
    Collection<Folder> getFolders();

    /**
     * Adds a {@link Folder} and updates the world-to-folder mapping for all worlds contained in the folder.
     *
     * @param folder the folder to add
     */
    void addFolder(Folder folder);

    /**
     * Removes a folder and all of its worlds from the world-to-folder mapping.
     *
     * @param folderName the name of the folder to remove
     */
    void removeFolder(String folderName);

    /**
     * Checks if a {@link Folder} with the given name (case-insensitive) exists.
     *
     * @param folderName The name of the folder to check
     * @return {@code true} if the folder exists, {@code false} otherwise
     */
    boolean folderExists(String folderName);

    /**
     * Checks if a {@link Folder} with the given name exists.
     *
     * @param folderName    The name of the folder to check
     * @param caseSensitive Whether to check the name case-sensitive or not
     * @return {@code true} if the folder exists, {@code false} otherwise
     */
    boolean folderExists(String folderName, boolean caseSensitive);

    /**
     * Adds a {@link BuildWorld} to the specified {@link Folder} and updates the folder and world-to-folder mappings.
     *
     * @param worldName  the name of the world to add
     * @param folderName the name of the folder to add the world to
     */
    void addWorldToFolder(String worldName, String folderName);

    /**
     * Removes a world from the specified {@link Folder} and updates the world-to-folder mapping.
     *
     * @param worldName  the name of the world to remove
     * @param folderName the name of the folder to remove the world from
     */
    void removeWorldFromFolder(String worldName, String folderName);

    /**
     * Checks whether the given {@link BuildWorld} is assigned to any {@link Folder}.
     *
     * @param buildWorld the build world to check
     * @return {@code true} if the world is in any folder; {@code false} otherwise
     */
    boolean isWorldInAnyFolder(BuildWorld buildWorld);
}
