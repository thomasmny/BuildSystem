/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api;

import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.PlayerManager;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldManager;

public interface BuildSystem {

    /**
     * Gets the {@link WorldManager}, responsible for managing {@link BuildWorld} instances.
     *
     * @return The world manager
     */
    WorldManager getWorldManager();

    /**
     * Gets the {@link PlayerManager}, responsible for managing {@link BuildPlayer} instances.
     *
     * @return The player manager
     */
    PlayerManager getPlayerManager();
}