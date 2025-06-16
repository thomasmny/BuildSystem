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
import org.jetbrains.annotations.Nullable;
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
     * Gets a {@link Folder} by its name (case-insensitive).
     *
     * @param folderName The name of the folder to retrieve
     * @return The folder if it exists, or {@code null} if it does not
     */
    @Nullable
    Folder getFolder(String folderName);

    /**
     * Gets a {@link Folder} by its name.
     *
     * @param folderName    The name of the folder to retrieve
     * @param caseSensitive Whether to check the name case-sensitive or not
     * @return The folder if it exists, or {@code null} if it does not
     */
    @Nullable
    Folder getFolder(String folderName, boolean caseSensitive);

    /**
     * Creates a new {@link Folder} with the given name and adds it to the folder storage.
     *
     * @param folderName The name folder to create
     */
    Folder createFolder(String folderName);

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
     * Checks whether the given {@link BuildWorld} is assigned to any {@link Folder}.
     *
     * @param buildWorld The world to check
     * @return {@code true} if the world is in any folder; {@code false} otherwise
     */
    boolean isAssignedToAnyFolder(BuildWorld buildWorld);

    /**
     * Gets the {@link Folder} that contains the specified {@link BuildWorld}.
     *
     * @param buildWorld The world to check
     * @return The folder containing the world, or {@code null} if the world is not in any folder
     */
    @Nullable
    Folder getAssignedFolder(BuildWorld buildWorld);
}
