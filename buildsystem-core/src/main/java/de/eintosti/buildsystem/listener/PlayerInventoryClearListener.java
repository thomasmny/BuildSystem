/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.config.Config.Settings.Navigator;
import de.eintosti.buildsystem.event.player.PlayerInventoryClearEvent;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerInventoryClearListener implements Listener {

    private final SettingsManager settingsManager;

    public PlayerInventoryClearListener(BuildSystemPlugin plugin) {
        this.settingsManager = plugin.getSettingsManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInventoryClear(PlayerInventoryClearEvent event) {
        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);
        if (!settings.isKeepNavigator() || !player.hasPermission("buildsystem.navigator.item")) {
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        ItemStack navigatorItem = InventoryUtils.createItem(
                Navigator.item, Messages.getString("navigator_item", player)
        );
        event.getNavigatorSlots().forEach(slot -> playerInventory.setItem(slot, navigatorItem));
    }
}