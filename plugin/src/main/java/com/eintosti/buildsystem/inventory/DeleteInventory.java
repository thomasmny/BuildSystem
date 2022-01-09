/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * @author einTosti
 */
public class DeleteInventory {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public DeleteInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
    }

    private Inventory getInventory(Player player, BuildWorld buildWorld) {
        Inventory inventory = Bukkit.createInventory(null, 45, plugin.getString("delete_title"));
        fillGuiWithGlass(player, inventory);

        inventoryManager.addItemStack(inventory, 13, XMaterial.FILLED_MAP, plugin.getString("delete_world_name").replace("%world%", buildWorld.getName()), plugin.getStringList("delete_world_name_lore"));
        inventoryManager.addItemStack(inventory, 29, XMaterial.RED_DYE, plugin.getString("delete_world_cancel"));
        inventoryManager.addItemStack(inventory, 33, XMaterial.LIME_DYE, plugin.getString("delete_world_confirm"));

        return inventory;
    }

    public void openInventory(Player player, BuildWorld buildWorld) {
        player.openInventory(getInventory(player, buildWorld));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }
}
