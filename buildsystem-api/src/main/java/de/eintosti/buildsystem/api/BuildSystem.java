/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api;

import de.eintosti.buildsystem.api.player.PlayerManager;
import de.eintosti.buildsystem.api.world.WorldManager;

public interface BuildSystem {

    WorldManager getWorldManager();

    PlayerManager getPlayerManager();
}