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
package de.eintosti.buildsystem.api.world.display;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a folder within the BuildSystem's world navigation structure. Folders can contain {@link BuildWorld}s and other nested folders, organizing them for easier access.
 *
 * @since 3.0.0
 */
@NullMarked
public interface Folder extends Displayable {

    /**
     * Gets the {@link Builder} who originally created this folder.
     *
     * @return The {@link Builder} instance representing the folder's creator
     */
    Builder getCreator();

    /**
     * Gets the {@link NavigatorCategory} in which this folder is displayed.
     *
     * @return The {@link NavigatorCategory} of the folder
     */
    NavigatorCategory getCategory();

    /**
     * Gets the parent {@link Folder} of this folder, if it is nested.
     *
     * @return The parent {@link Folder}, or {@code null} if this is a top-level folder
     */
    @Nullable
    Folder getParent();

    /**
     * Sets the parent {@link Folder} for this folder. Setting it to {@code null} will make this a top-level folder.
     * <p>
     * The parent folder must belong to the same {@link NavigatorCategory} as this folder. If the categories differ, an {@link IllegalArgumentException} is thrown.
     *
     * @param parent The new parent {@link Folder}, or {@code null} to remove the current parent
     * @throws IllegalArgumentException if the parent has a different {@link NavigatorCategory}
     */
    void setParent(@Nullable Folder parent);

    /**
     * Checks if this folder has a parent {@link Folder}.
     *
     * @return {@code true} if this folder is nested under another, {@code false} otherwise
     */
    boolean hasParent();

    /**
     * Gets an unmodifiable list of UUIDs for all {@link BuildWorld}s contained directly within this folder.
     *
     * @return An {@link Unmodifiable} {@link List} of {@link BuildWorld} UUIDs
     */
    @Unmodifiable
    List<UUID> getWorldUUIDs();

    /**
     * Checks if this folder contains the specified {@link BuildWorld}.
     *
     * @param buildWorld The {@link BuildWorld} to check for
     * @return {@code true} if the folder contains the world, {@code false} otherwise
     */
    boolean containsWorld(BuildWorld buildWorld);

    /**
     * Checks if this folder contains the {@link BuildWorld} with the specified UUID.
     *
     * @param uuid The unique identifier of the {@link BuildWorld} to check for
     * @return {@code true} if the folder contains the world, {@code false} otherwise
     */
    boolean containsWorld(UUID uuid);

    /**
     * Adds a {@link BuildWorld} to this folder.
     *
     * @param buildWorld The {@link BuildWorld} to add
     */
    void addWorld(BuildWorld buildWorld);

    /**
     * Removes a {@link BuildWorld} from this folder.
     *
     * @param buildWorld The {@link BuildWorld} to remove
     */
    void removeWorld(BuildWorld buildWorld);

    /**
     * Removes a {@link BuildWorld} with the specified UUID from this folder.
     *
     * @param uuid The unique identifier of the {@link BuildWorld} to remove
     */
    void removeWorld(UUID uuid);

    /**
     * Returns an unmodifiable list of all immediate subfolders contained within this folder.
     * <p>
     * This includes only direct children—folders whose {@link #getParent()} is exactly this folder. Nested subfolders (i.e., deeper levels of the folder hierarchy) are not
     * included.
     *
     * @return A list of immediate subfolders
     */
    @Unmodifiable
    List<Folder> getSubFolders();

    /**
     * Gets the total number of {@link BuildWorld}s contained in this folder and all of its subfolders.
     * <p>
     * This includes both the worlds directly assigned to this folder and those assigned to any nested subfolders.
     *
     * @return The total number of worlds in this folder and its subfolders
     */
    int getWorldCount();

    /**
     * Gets the permission string required for players to access or view this folder. Returns "-" if no specific permission is required.
     *
     * @return The permission string, or "-" if none is set
     */
    String getPermission();

    /**
     * Sets the permission string required for players to access or view this folder. Setting to "-" will remove any permission requirement.
     *
     * @param permission The permission string to set, or "-" to remove
     */
    void setPermission(String permission);

    /**
     * Gets the project name associated with this {@link Folder}. This can be used for categorization or informational purposes.
     *
     * @return The project name as a string
     */
    String getProject();

    /**
     * Sets the project name for this {@link Folder}.
     *
     * @param project The new project name to set
     */
    void setProject(String project);

    /**
     * Checks if the given {@link Player} has permission to view this folder in the navigator.
     *
     * @param player The {@link Player} to check
     * @return {@code true} if the player can view the folder, {@code false} otherwise
     */
    boolean canView(Player player);
}