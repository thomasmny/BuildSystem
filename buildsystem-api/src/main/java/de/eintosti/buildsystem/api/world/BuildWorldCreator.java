/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world;

import de.eintosti.buildsystem.api.world.data.WorldType;
import de.eintosti.buildsystem.api.world.generator.CustomGenerator;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface BuildWorldCreator {

    BuildWorldCreator setName(String name);

    BuildWorldCreator setCreator(Builder creator);

    BuildWorldCreator setTemplate(String template);

    BuildWorldCreator setType(WorldType type);

    BuildWorldCreator setCustomGenerator(CustomGenerator customGenerator);

    BuildWorldCreator setPrivate(boolean privateWorld);

    BuildWorldCreator setDifficulty(Difficulty difficulty);

    BuildWorldCreator setCreationDate(long creationDate);

    /**
     * Depending on the {@link BuildWorld}'s {@link WorldType}, the corresponding {@link World} will be generated in a different way.
     * Then, if the creation of the world was successful and the config is set accordingly, the player is teleported to the world.
     *
     * @param player The player who is creating the world
     */
    void createWorld(Player player);

    /**
     * Imports an existing world as a {@link BuildWorld}.
     *
     * @param player   The player who is importing the world
     * @param teleport Should the player be teleported to the world after importing is finished
     */
    void importWorld(Player player, boolean teleport);

    @Nullable
    World generateBukkitWorld();

    /**
     * Generate the {@link World} linked to a {@link BuildWorld}.
     *
     * @param checkVersion Should the world version be checked
     * @return The world object
     */
    @Nullable
    World generateBukkitWorld(boolean checkVersion);
}