/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.player.BuildPlayer;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.PlayerManager;
import de.eintosti.buildsystem.settings.Settings;
import de.eintosti.buildsystem.settings.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.AbstractMap;

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

        BuildPlayer buildPlayer = playerManager.getBuildPlayer(player);
        buildPlayer.setLogoutLocation(new LogoutLocation(
                player.getWorld().getName(),
                player.getLocation()
        ));

        CachedValues cachedValues = buildPlayer.getCachedValues();
        cachedValues.resetGameModeIfPresent(player);
        cachedValues.resetInventoryIfPresent(player);
        playerManager.getBuildModePlayers().remove(player.getUniqueId());

        manageHidePlayer(player);
    }

    @SuppressWarnings("deprecation")
    private void manageHidePlayer(Player player) {
        // Show all hidden players to player
        if (settingsManager.getSettings(player).isHidePlayers()) {
            Bukkit.getOnlinePlayers().forEach(player::showPlayer);
        }

        // Show player to all players who had him/her hidden
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (!settingsManager.getSettings(pl).isHidePlayers()) {
                continue;
            }
            pl.showPlayer(player);
        }
    }
}