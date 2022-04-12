/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.PlayerManager;
import com.eintosti.buildsystem.manager.SettingsManager;
import com.eintosti.buildsystem.object.navigator.NavigatorType;
import com.eintosti.buildsystem.object.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * @author einTosti
 */
public class PlayerMoveListener implements Listener {

    private final BuildSystem plugin;
    private final PlayerManager playerManager;
    private final SettingsManager settingsManager;

    public PlayerMoveListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.settingsManager = plugin.getSettingsManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!playerManager.getOpenNavigator().contains(player)) {
            return;
        }

        Settings settings = settingsManager.getSettings(player);
        if (!settings.getNavigatorType().equals(NavigatorType.NEW)) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Location from = event.getFrom();
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> playerManager.closeNavigator(player), 5L);
        }
    }
}