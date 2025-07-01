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
package de.eintosti.buildsystem.api.world;

import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.display.Displayable;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.util.WorldLoader;
import de.eintosti.buildsystem.api.world.util.WorldPermissions;
import de.eintosti.buildsystem.api.world.util.WorldTeleporter;
import de.eintosti.buildsystem.api.world.util.WorldUnloader;
import java.util.UUID;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a world managed by the BuildSystem plugin, extending the {@link Displayable} interface. This interface provides comprehensive access to world-specific properties,
 * data, and utility methods.
 *
 * @since 3.0.0
 */
@NullMarked
public interface BuildWorld extends Displayable {

    /**
     * Gets the Bukkit {@link World} associated with this {@link BuildWorld}.
     *
     * @return The Bukkit world, or {@code null} if not loaded
     */
    @Nullable
    World getWorld();

    /**
     * Gets the unique identifier of this world.
     * <p>
     * Not equivalent to {@link World#getUID()}.
     *
     * @return The uuid of this world
     */
    UUID getUniqueId();

    /**
     * Sets the name of this world.
     *
     * @param name The name of the world
     */
    void setName(String name);

    /**
     * Gets the {@link Profileable} representation of this build world which is applied when {@link WorldData#material()} is set to {@link Material#PLAYER_HEAD}.
     *
     * @return The {@link Profileable} representation of this build world
     */
    Profileable asProfilable();

    /**
     * Gets this world's {@link BuildWorldType}.
     *
     * @return The type of this world
     */
    BuildWorldType getType();

    /**
     * Gets this world's {@link WorldData}.
     *
     * @return The data of the world
     */
    WorldData getData();

    /**
     * Gets the custom chunk generator used to generate this world.
     * <p>
     * Only set when the world type is {@link BuildWorldType#CUSTOM} or {@link BuildWorldType#IMPORTED}.
     *
     * @return The custom chunk generator used to generate this world, or {@code null} if not set
     */
    @Nullable
    CustomGenerator getCustomGenerator();

    /**
     * Cycles to the next {@link Difficulty} for this world.
     * <p>
     * The cycle order is: {@link Difficulty#PEACEFUL} -> {@link Difficulty#EASY} -> {@link Difficulty#NORMAL} -> {@link Difficulty#HARD} -> {@link Difficulty#PEACEFUL}.
     *
     * @return The new difficulty after cycling
     */
    Difficulty cycleDifficulty();

    /**
     * Gets the {@link Builders} object, which manages all players allowed to modify this world.
     *
     * @return The {@link Builders} instance for this world
     */
    Builders getBuilders();

    /**
     * Gets the time of day in the {@link World} linked to this build world as a formatted string.
     *
     * @return This world time as a string (e.g., "Day", "Night")
     */
    String getWorldTime();

    /**
     * Gets whether this world is currently loaded into server memory, allowing players to enter it.
     *
     * @return {@code true} if this world is loaded, otherwise {@code false}
     */
    boolean isLoaded();

    /**
     * Sets whether this world is currently loaded into server memory.
     *
     * @param loaded {@code true} if this world is to be loaded, {@code false} if it should be unloaded
     */
    void setLoaded(boolean loaded);

    /**
     * Gets the {@link WorldLoader} utility used to manage loading operations for this world.
     *
     * @return The {@link WorldLoader} instance
     */
    WorldLoader getLoader();

    /**
     * Gets the {@link WorldUnloader} utility used to manage unloading operations for this world.
     *
     * @return The {@link WorldUnloader} instance
     */
    WorldUnloader getUnloader();

    /**
     * Gets the {@link WorldTeleporter} utility used to manage teleportation of players to this world.
     *
     * @return The {@link WorldTeleporter} instance
     */
    WorldTeleporter getTeleporter();

    /**
     * Gets the {@link WorldPermissions} associated with this world, which define access and modification rules.
     *
     * @return The {@link WorldPermissions} instance for this world
     */
    WorldPermissions getPermissions();

    /**
     * Gets the {@link Folder} this world is assigned to.
     *
     * @return The folder this world is assigned to, or {@code null} if not assigned
     */
    @Nullable
    Folder getFolder();

    /**
     * Checks whether this world is assigned to a {@link Folder}.
     *
     * @return {@code true} if this world is in any folder, {@code false} otherwise
     */
    boolean isAssignedToFolder();

    /**
     * Sets the {@link Folder} this world is assigned to.
     *
     * @param folder The folder to assign this world to, or {@code null} to remove the assignment
     */
    void setFolder(@Nullable Folder folder);
}