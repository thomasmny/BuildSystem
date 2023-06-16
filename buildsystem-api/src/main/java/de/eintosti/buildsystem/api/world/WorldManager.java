/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world;

import de.eintosti.buildsystem.api.world.generator.Generator;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;

public interface WorldManager {

    /**
     * Gets the {@link BuildWorld} by the given name.
     *
     * @param worldName The name of the world
     * @return The world object if one was found, {@code null} otherwise
     */
    BuildWorld getBuildWorld(String worldName);

    /**
     * Gets the {@link BuildWorld} by the given {@link World}.
     *
     * @param world The bukkit world object
     * @return The world object if one was found, {@code null} otherwise
     */
    BuildWorld getBuildWorld(World world);

    /**
     * Gets a list of all {@link BuildWorld}s.
     *
     * @return A list of all worlds
     */
    @Unmodifiable
    Collection<BuildWorld> getBuildWorlds();

    /**
     * Gets a list of all {@link BuildWorld}s created by the given player.
     *
     * @param player The player who created the world
     * @return A list of all worlds created by the given player.
     */
    List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player);

    /**
     * Gets a list of all {@link BuildWorld}s created by the given player.
     *
     * @param player     The player who created the world
     * @param visibility The visibility the world should have
     * @return A list of all worlds created by the given player.
     */
    List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player, Visibility visibility);

    /**
     * Checks if a world with the given name already exists.
     *
     * @param worldName The name of the world
     * @return Whether if a world with the given name already exists
     */
    boolean worldExists(String worldName);

    /**
     * Change the name of a {@link BuildWorld} to a given name.
     *
     * @param buildWorld The build world object
     * @param newName    The name the world should be renamed to
     */
    void renameWorld(BuildWorld buildWorld, String newName);

    /**
     * Import a {@link BuildWorld} from a world directory.
     *
     * @param player        The player who is creating the world
     * @param worldName     Name of the world that the chunk generator should be applied to.
     * @param creator       The builder who should be set as the creator
     * @param generator     The generator type used by the world
     * @param generatorName The name of the custom generator if generator type is {@link Generator#CUSTOM}
     * @return {@code true} if the world was successfully imported, otherwise {@code false}
     */
    boolean importWorld(Player player, String worldName, Builder creator, Generator generator, String generatorName);

    /**
     * Delete an existing {@link BuildWorld}.
     * In comparison to {@link #unimportWorld(BuildWorld, boolean)}, deleting a world deletes the world's directory.
     *
     * @param buildWorld The world to be deleted
     */
    void deleteWorld(Player player, BuildWorld buildWorld);

    /**
     * Unimport an existing {@link BuildWorld}.
     * In comparison to {@link #deleteWorld(Player, BuildWorld)}, unimporting a world does not delete the world's directory.
     *
     * @param buildWorld The build world object
     * @param save       Should the world be saved before unimporting
     */
    void unimportWorld(BuildWorld buildWorld, boolean save);

    /**
     * Teleport a player to a {@link BuildWorld}.
     *
     * @param player    The player to be teleported
     * @param worldName The name of the world
     */
    void teleport(Player player, String worldName);

    boolean canEnter(Player player, BuildWorld buildWorld);
}