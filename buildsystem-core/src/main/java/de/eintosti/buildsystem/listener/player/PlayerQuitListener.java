/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
package de.eintosti.buildsystem.listener.player;

import de.eintosti.buildsystem.api.player.PlayerService;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerQuitListener implements Listener {

    private final PlayerService playerManager;
    private final NavigatorService navigatorService;
    private final NavigatorEditorService navigatorEditorService;
    private final NoClipService noClipService;
    private final SettingsService settingsManager;
    private final ConfigService configService;
    private final Messages messages;

    public PlayerQuitListener(
            PlayerService playerManager,
            NavigatorService navigatorService,
            NavigatorEditorService navigatorEditorService,
            NoClipService noClipService,
            SettingsService settingsManager,
            ConfigService configService,
            Messages messages) {
        this.playerManager = playerManager;
        this.navigatorService = navigatorService;
        this.navigatorEditorService = navigatorEditorService;
        this.noClipService = noClipService;
        this.settingsManager = settingsManager;
        this.configService = configService;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void sendPlayerQuitMessage(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String message = configService.current().settings().joinQuitMessages()
                ? messages.getString("player_quit", player, Map.entry("%player%", player.getName()))
                : null;
        event.setQuitMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Restore the real inventory first if the navigator layout editor took it over, so later quit handling
        // (e.g. clear-inventory) and the server's player-data save see the genuine contents.
        navigatorEditorService.restore(player);
        navigatorService.closeNewNavigator(player);

        Settings settings = settingsManager.getSettings(player);
        if (settings.isNoClip()) {
            noClipService.stopNoClip(player.getUniqueId());
        }

        if (settings.isScoreboard()) {
            settingsManager.hideScoreboard(player);
        }

        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }

        BuildPlayerImpl buildPlayer =
                BuildPlayerImpl.of(playerManager.getPlayerStorage().getBuildPlayer(player));
        buildPlayer.setLogoutLocation(new LogoutLocation(player.getWorld().getName(), player.getLocation()));

        CachedValues cachedValues = buildPlayer.getCachedValues();
        cachedValues.resetGameModeIfPresent(player);
        cachedValues.resetInventoryIfPresent(player);
        playerManager.leaveBuildMode(player.getUniqueId());

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
