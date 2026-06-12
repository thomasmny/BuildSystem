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
package de.eintosti.buildsystem.api.world;

import de.eintosti.buildsystem.api.storage.FolderStorage;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.creation.WorldBuilder;
import de.eintosti.buildsystem.api.world.creation.WorldImporter;
import de.eintosti.buildsystem.api.world.display.Folder;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;

/**
 * Provides a service for managing world-related operations and data. This interface offers methods to access and
 * interact with world storage and folder management.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldService {

    /**
     * Gets the {@link FolderStorage} implementation for managing {@link Folder} persistence.
     *
     * @return The folder storage instance
     */
    FolderStorage getFolderStorage();

    /**
     * Gets the {@link WorldStorage} implementation for managing {@link BuildWorld} persistence.
     *
     * @return The world storage instance
     */
    WorldStorage getWorldStorage();

    /**
     * Opens a {@link WorldBuilder} to generate a brand-new world with the given name.
     *
     * <pre>{@code
     * BuildWorld world = worldService.newWorld("Lobby")
     *         .type(BuildWorldType.NORMAL)
     *         .creator(builder)
     *         .build();
     * }</pre>
     *
     * @param name The name of the world to create
     * @return A new {@link WorldBuilder} for the specified world name
     * @since 3.1.0
     */
    WorldBuilder newWorld(String name);

    /**
     * Opens a {@link WorldImporter} to adopt an existing world directory (located under the server's world container by
     * the given name) as a {@link BuildWorld}.
     *
     * @param name The name of the existing world directory to import
     * @return A new {@link WorldImporter} for the specified world name
     * @since 3.1.0
     */
    WorldImporter importWorld(String name);

    /**
     * Unimport an existing {@link BuildWorld}. In comparison to {@link #deleteWorld(BuildWorld)}, unimporting a world
     * does not delete the world's directory.
     *
     * @param buildWorld The world to unimport
     * @param save Whether to save the world before unloading
     * @return A future that completes when the unimport operation is finished
     */
    CompletableFuture<Void> unimportWorld(BuildWorld buildWorld, boolean save);

    /**
     * Delete an existing {@link BuildWorld}. In comparison to {@link #unimportWorld(BuildWorld, boolean)}, deleting a
     * world deletes the world's directory.
     *
     * @param buildWorld The world to be deleted
     * @return A future that completes when the delete operation is finished
     */
    CompletableFuture<Void> deleteWorld(BuildWorld buildWorld);
}
