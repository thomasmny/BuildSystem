/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.config;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.player.CraftBuildPlayer;

import java.util.Collection;

public class PlayersConfig extends ConfigurationFile {

    public PlayersConfig(BuildSystemPlugin plugin) {
        super(plugin, "players.yml");
    }

    public void savePlayers(Collection<CraftBuildPlayer> buildPlayers) {
        buildPlayers.forEach(buildPlayer -> getFile().set("players." + buildPlayer.getUniqueId().toString(), buildPlayer.serialize()));
        saveFile();
    }
}