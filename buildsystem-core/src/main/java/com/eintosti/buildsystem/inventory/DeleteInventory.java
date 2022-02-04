/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * @author einTosti
 */
public class DeleteInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public DeleteInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "delete_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = plugin.getPlayerManager().getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("worlds_delete_error"));
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case 29:
                XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
                player.sendMessage(plugin.getString("worlds_delete_canceled").replace("%world%", buildWorld.getName()));
                break;
            case 33:
                XSound.ENTITY_PLAYER_LEVELUP.play(player);
                plugin.getWorldManager().deleteWorld(player, buildWorld);
                break;
            default:
                return;
        }

        player.closeInventory();
    }
}
