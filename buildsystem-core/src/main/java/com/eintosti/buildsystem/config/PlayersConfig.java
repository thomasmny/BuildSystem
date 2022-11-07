/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.config;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.player.BuildPlayer;

import java.util.UUID;

/**
 * @author einTosti
 */
public class PlayersConfig extends ConfigurationFile {

    public PlayersConfig(BuildSystem plugin) {
        super(plugin, "players.yml");
    }

    public void savePlayer(UUID uuid, BuildPlayer buildPlayer) {
        getFile().set("players." + uuid.toString(), buildPlayer.serialize());
        saveFile();
    }
}