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

import java.util.Collection;

public class PlayersConfig extends ConfigurationFile {

    public PlayersConfig(BuildSystem plugin) {
        super(plugin, "players.yml");
    }

    public void savePlayers(Collection<BuildPlayer> buildPlayers) {
        buildPlayers.forEach(buildPlayer -> getFile().set("players." + buildPlayer.getUniqueId().toString(), buildPlayer.serialize()));
        saveFile();
    }
}