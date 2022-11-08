/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.player.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * @author einTosti
 */
public class PlayerDropItemListener implements Listener {

    private final ConfigValues configValues;
    private final PlayerManager playerManager;

    public PlayerDropItemListener(BuildSystem plugin) {
        this.configValues = plugin.getConfigValues();
        this.playerManager = plugin.getPlayerManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (configValues.isBuildModeDropItems()) {
            return;
        }

        if (playerManager.getBuildModePlayers().contains(event.getPlayer().getUniqueId())) {
            event.getItemDrop().remove();
        }
    }
}