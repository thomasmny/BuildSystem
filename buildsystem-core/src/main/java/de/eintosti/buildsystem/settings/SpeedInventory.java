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
package de.eintosti.buildsystem.settings;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.util.InventoryUtils;
import java.util.AbstractMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class SpeedInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryUtils inventoryUtils;

    public SpeedInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtil();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Messages.getString("speed_title", player));
        fillGuiWithGlass(player, inventory);

        inventoryUtils.addSkull(inventory, 11, Messages.getString("speed_1", player), Profileable.detect("71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530"));
        inventoryUtils.addSkull(inventory, 12, Messages.getString("speed_2", player), Profileable.detect("4cd9eeee883468881d83848a46bf3012485c23f75753b8fbe8487341419847"));
        inventoryUtils.addSkull(inventory, 13, Messages.getString("speed_3", player), Profileable.detect("1d4eae13933860a6df5e8e955693b95a8c3b15c36b8b587532ac0996bc37e5"));
        inventoryUtils.addSkull(inventory, 14, Messages.getString("speed_4", player), Profileable.detect("d2e78fb22424232dc27b81fbcb47fd24c1acf76098753f2d9c28598287db5"));
        inventoryUtils.addSkull(inventory, 15, Messages.getString("speed_5", player), Profileable.detect("6d57e3bc88a65730e31a14e3f41e038a5ecf0891a6c243643b8e5476ae2"));

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 26; i++) {
            inventoryUtils.addGlassPane(plugin, player, inventory, i);
        }
    }

    @EventHandler
    public void oInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtils.checkIfValidClick(event, "speed_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("buildsystem.speed")) {
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case 11:
                setSpeed(player, 0.2f, 1);
                break;
            case 12:
                setSpeed(player, 0.4f, 2);
                break;
            case 13:
                setSpeed(player, 0.6f, 3);
                break;
            case 14:
                setSpeed(player, 0.8f, 4);
                break;
            case 15:
                setSpeed(player, 1.0f, 5);
                break;
            default:
                return;
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.closeInventory();
    }

    private void setSpeed(Player player, float speed, int num) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            Messages.sendMessage(player, "speed_set_flying", new AbstractMap.SimpleEntry<>("%speed%", num));
        } else {
            player.setWalkSpeed(speed);
            Messages.sendMessage(player, "speed_set_walking", new AbstractMap.SimpleEntry<>("%speed%", num));
        }
    }
}