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
import com.eintosti.buildsystem.object.player.LogoutLocation;
import com.eintosti.buildsystem.object.settings.Settings;
import com.eintosti.buildsystem.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.AbstractMap;

/**
 * @author einTosti
 */
public class PlayerQuitListener implements Listener {

    private final BuildSystem plugin;
    private final PlayerManager playerManager;
    private final SettingsManager settingsManager;

    public PlayerQuitListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.settingsManager = plugin.getSettingsManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendPlayerQuitMessage(PlayerQuitEvent event) {
        boolean isQuitMessage = plugin.getConfigValues().isJoinQuitMessages();
        String message = isQuitMessage ? Messages.getString("player_quit", new AbstractMap.SimpleEntry<>("%player%", event.getPlayer().getName())) : null;
        event.setQuitMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerManager.closeNavigator(player);

        Settings settings = settingsManager.getSettings(player);
        if (settings.isNoClip()) {
            plugin.getNoClipManager().stopNoClip(player.getUniqueId());
        }

        if (settings.isScoreboard()) {
            settingsManager.stopScoreboard(player);
        }

        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }

        playerManager.getBuildPlayer(player).setLogoutLocation(new LogoutLocation(
                player.getWorld().getName(),
                player.getLocation()
        ));

        manageHidePlayer(player);
    }

    @SuppressWarnings("deprecation")
    private void manageHidePlayer(Player player) {
        if (settingsManager.getSettings(player).isHidePlayers()) { // Show all hidden players to player
            Bukkit.getOnlinePlayers().forEach(player::showPlayer);
        }

        for (Player pl : Bukkit.getOnlinePlayers()) { // Show player to all players who had him/her hidden
            if (!settingsManager.getSettings(pl).isHidePlayers()) {
                continue;
            }
            pl.showPlayer(player);
        }
    }
}