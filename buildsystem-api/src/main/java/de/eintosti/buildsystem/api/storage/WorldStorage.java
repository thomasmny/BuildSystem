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
import de.eintosti.buildsystem.api.world.creation.BuildWorldCreator;
import de.eintosti.buildsystem.api.world.data.Visibility;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interface for managing the storage of {@link BuildWorld} objects.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldStorage extends Storage<BuildWorld> {

    /**
     * Gets the {@link BuildWorld} by the given name.
     *
     * @param name The name of the world
     * @return The world object if one was found, {@code null} otherwise
     */
    @Nullable
    BuildWorld getBuildWorld(String name);

    /**
     * Gets the {@link BuildWorld} by the given {@link World}.
     *
     * @param world The bukkit world object
     * @return The world object if one was found, {@code null} otherwise
     */
    @Nullable
    BuildWorld getBuildWorld(World world);

    /**
     * Gets the {@link BuildWorld} by the given {@link UUID}.
     *
     * @param uuid The build world's unique identifier
     * @return The world object if one was found, {@code null} otherwise
     */
    @Nullable
    BuildWorld getBuildWorld(UUID uuid);

    /**
     * Gets a list of all {@link BuildWorld}s.
     *
     * @return An unmodifiable list of all worlds
     */
    @Unmodifiable
    Collection<BuildWorld> getBuildWorlds();

    /**
     * Creates a new {@link BuildWorldCreator} for the given name.
     *
     * @param name The name of the world to create
     * @return A new {@link BuildWorldCreator} instance for the specified world name
     */
    BuildWorldCreator createBuildWorld(String name);

    /**
     * Adds a {@link BuildWorld} to the world map.
     *
     * @param buildWorld The {@link BuildWorld} to add
     */
    void addBuildWorld(BuildWorld buildWorld);

    /**
     * Removes a {@link BuildWorld} from the world map.
     *
     * @param buildWorld The {@link BuildWorld} to remove
     */
    void removeBuildWorld(BuildWorld buildWorld);

    /**
     * Checks if a {@link BuildWorld} with the given name (case-insensitive) exists.
     *
     * @param worldName The name of the world to check
     * @return {@code true} if the world exists, {@code false} otherwise
     */
    boolean worldExists(String worldName);

    /**
     * Checks if a {@link BuildWorld} with the given name exists.
     *
     * @param worldName     The name of the world to check
     * @param caseSensitive Whether to check the name case-sensitive or not
     * @return {@code true} if the world exists, {@code false} otherwise
     */
    boolean worldExists(String worldName, boolean caseSensitive);

    /**
     * Checks if a {@link BuildWorld} exists and if the world folder exists on disk.
     *
     * @param worldName The name of the world to check
     * @return {@code true} if the world exists in the map or on disk, {@code false} otherwise
     */
    boolean worldAndFolderExist(String worldName);

    /**
     * Gets a list of {@link BuildWorld}s created by the given player.
     *
     * @param player The player who created the worlds
     * @return A list of worlds created by the player
     */
    List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player);

    /**
     * Gets a list of {@link BuildWorld}s created by the given player with the given visibility.
     *
     * @param player     The player who created the worlds
     * @param visibility The visibility of the worlds
     * @return A list of worlds created by the player with the given visibility
     */
    List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player, Visibility visibility);
}
