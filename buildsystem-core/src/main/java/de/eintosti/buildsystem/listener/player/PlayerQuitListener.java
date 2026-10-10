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

import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.world.WorldNames;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerQuitListener implements Listener {

    private final PlayerServiceImpl playerManager;
    private final NavigatorService navigatorService;
    private final NavigatorEditorService navigatorEditorService;
    private final NoClipService noClipService;
    private final SettingsService settingsManager;
    private final ConfigService configService;
    private final Messages messages;

    public PlayerQuitListener(
            PlayerServiceImpl playerManager,
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
                ? messages.getString("player_quit", player, Placeholders.of("%player%", player.getName()))
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

        BuildPlayerImpl buildPlayer =
                BuildPlayerImpl.of(playerManager.getPlayerStorage().getBuildPlayer(player));
        // Hand the snapshots back before the server saves the player. Build mode is unwound first, as on a world
        // change: inside an archive world its snapshot is the emptied inventory.
        playerManager.endBuildSession(player);
        boolean leftArchive = ArchiveMode.exit(player, buildPlayer.getCachedValues());

        if (settings.isClearInventory()) {
            player.getInventory().clear();
        }

        buildPlayer.setLogoutLocation(new LogoutLocation(WorldNames.of(player.getWorld()), player.getLocation()));
        if (leftArchive) {
            playerManager.getPlayerStorage().save(buildPlayer);
        }
    }
}
