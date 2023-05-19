/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.config;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.player.BuildPlayer;

import java.util.UUID;

public class PlayersConfig extends ConfigurationFile {

    public PlayersConfig(BuildSystem plugin) {
        super(plugin, "players.yml");
    }

    public void savePlayer(UUID uuid, BuildPlayer buildPlayer) {
        getFile().set("players." + uuid.toString(), buildPlayer.serialize());
        saveFile();
    }
}