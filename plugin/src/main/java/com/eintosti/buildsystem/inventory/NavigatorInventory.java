/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * @author einTosti
 */
public class NavigatorInventory {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public NavigatorInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, plugin.getString("old_navigator_title"));
        fillGuiWithGlass(player, inventory);

        inventoryManager.addUrlSkull(inventory, 11, plugin.getString("old_navigator_world_navigator"), "http://textures.minecraft.net/texture/d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622");
        inventoryManager.addUrlSkull(inventory, 12, plugin.getString("old_navigator_world_archive"), "http://textures.minecraft.net/texture/7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8");
        inventoryManager.addSkull(inventory, 13, plugin.getString("old_navigator_private_worlds"), player.getName());

        inventoryManager.addUrlSkull(inventory, 15, plugin.getString("old_navigator_settings"), "http://textures.minecraft.net/texture/1cba7277fc895bf3b673694159864b83351a4d14717e476ebda1c3bf38fcf37");

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 26; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }
}
