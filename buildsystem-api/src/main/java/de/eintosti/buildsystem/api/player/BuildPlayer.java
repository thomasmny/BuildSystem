/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.player;

import de.eintosti.buildsystem.api.settings.Settings;

import java.util.UUID;

public interface BuildPlayer {

    /**
     * Gets the unique-id of the player.
     *
     * @return The player's UUID
     */
    UUID getUniqueId();

    /**
     * Gets the player's per-player settings.
     *
     * @return The player's settings
     */
    Settings getSettings();
}