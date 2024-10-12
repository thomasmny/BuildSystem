/*
 * Copyright (c) 2018-2024, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import java.util.AbstractMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

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
        Player player = event.getPlayer();
        String message = plugin.getConfigValues().isJoinQuitMessages()
                ? Messages.getString("player_quit", player, new AbstractMap.SimpleEntry<>("%player%", player.getName()))
                : null;
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