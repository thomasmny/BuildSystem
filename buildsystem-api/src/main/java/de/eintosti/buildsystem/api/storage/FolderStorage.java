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
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import java.util.Collection;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interface for managing the storage of {@link Folder} objects.
 *
 * @since 3.0.0
 */
@NullMarked
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
     * @param name The name of the folder to retrieve
     * @return The folder if it exists, or {@code null} if it does not
     */
    @Nullable
    Folder getFolder(String name);

    /**
     * Checks if a {@link Folder} with the given name (case-insensitive) exists.
     *
     * @param name The name of the folder to check
     * @return {@code true} if the folder exists, {@code false} otherwise
     */
    boolean folderExists(String name);

    /**
     * Creates a new {@link Folder} with the given name.
     *
     * @param name     The name folder to create
     * @param category The category in which the folder should be displayed
     * @param creator  The builder who created the folder
     * @return The newly created folder
     */
    Folder createFolder(String name, NavigatorCategory category, Builder creator);

    /**
     * Creates a new nested {@link Folder} with the given name.
     *
     * @param name     The name folder to create
     * @param category The category in which the folder should be displayed
     * @param parent   The parent folder, or {@code null} if this is a top-level folder
     * @param creator  The builder who created the folder
     * @return The newly created folder
     */
    Folder createFolder(String name, NavigatorCategory category, @Nullable Folder parent, Builder creator);

    /**
     * Removes the {@link Folder} with the given name.
     * <p>
     * This operation cascades:
     * <ul>
     *   <li>All subfolders within the specified folder will also be removed.</li>
     *   <li>Any {@link BuildWorld} instances associated with this folder will have their folder reference unset.</li>
     * </ul>
     *
     * @param name The name of the folder to remove
     * @see #removeFolder(Folder)
     */
    void removeFolder(String name);

    /**
     * Removes the given {@link Folder}.
     * <p>
     * This operation cascades:
     * <ul>
     *   <li>All subfolders within the specified folder will also be removed.</li>
     *   <li>Any {@link BuildWorld} instances associated with this folder will have their folder reference unset.</li>
     * </ul>
     *
     * @param folder The folder to remove
     * @see #removeFolder(String)
     */
    void removeFolder(Folder folder);
}
