/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api;

import com.eintosti.buildsystem.api.settings.Settings;
import com.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * @author einTosti
 */
public interface BuildAPI {

    /**
     * Gets a list of all build worlds.
     *
     * @return the server name
     */
    List<BuildWorld> getBuildWorlds();

    /**
     * Gets a build-world by its name
     *
     * @param name The name of the build world
     * @return The build world with the given name, or {@code null} if none found
     */
    @Nullable BuildWorld getBuildWorld(String name);

    /**
     * Gets the settings belonging to the given player
     *
     * @param player The player object
     * @return The settings belonging to the given player
     */
    Settings getSettings(Player player);
}
