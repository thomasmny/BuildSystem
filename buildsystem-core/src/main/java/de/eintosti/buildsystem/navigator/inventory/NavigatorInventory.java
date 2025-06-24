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
package de.eintosti.buildsystem.navigator.inventory;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.util.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class NavigatorInventory implements Listener {

    private final BuildSystemPlugin plugin;

    public NavigatorInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Messages.getString("old_navigator_title", player));
        fillGuiWithGlass(player, inventory);

        inventory.setItem(11, InventoryUtils.createSkull(Messages.getString("old_navigator_world_navigator", player), Profileable.detect("d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622")));
        inventory.setItem(12, InventoryUtils.createSkull(Messages.getString("old_navigator_world_archive", player), Profileable.detect("7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8")));
        inventory.setItem(13, InventoryUtils.createSkull(Messages.getString("old_navigator_private_worlds", player), Profileable.of(player)));

        inventory.setItem(15, InventoryUtils.createSkull(Messages.getString("old_navigator_settings", player), Profileable.detect("1cba7277fc895bf3b673694159864b83351a4d14717e476ebda1c3bf38fcf37")));

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 26; i++) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!InventoryUtils.isValidClick(event, "old_navigator_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 11:
                plugin.getWorldsInventory().openInventory(player);
                break;
            case 12:
                plugin.getArchiveInventory().openInventory(player);
                break;
            case 13:
                plugin.getPrivateInventory().openInventory(player);
                break;
            case 15:
                if (!player.hasPermission("buildsystem.settings")) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                plugin.getSettingsInventory().openInventory(player);
                break;
            default:
                return;
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
    }
}