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
package de.eintosti.buildsystem.listener.navigator;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerMoveListener implements Listener {

    private final BuildSystemPlugin plugin;
    private final NavigatorService navigatorService;
    private final SettingsService settingsManager;

    public PlayerMoveListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.navigatorService = plugin.getNavigatorService();
        this.settingsManager = plugin.getSettingsService();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!navigatorService.getOpenNavigator().contains(player)) {
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
            Bukkit.getServer()
                    .getScheduler()
                    .runTaskLater(plugin, () -> navigatorService.closeNewNavigator(player), 5L);
        }
    }
}
