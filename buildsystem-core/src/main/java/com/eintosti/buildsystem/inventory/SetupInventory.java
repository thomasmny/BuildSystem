/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.object.world.data.WorldStatus;
import com.eintosti.buildsystem.object.world.data.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 */
public class SetupInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public SetupInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, plugin.getString("setup_title"));
        fillGuiWithGlass(player, inventory);

        inventoryManager.addUrlSkull(inventory, 10, plugin.getString("setup_create_item_name"), "https://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", plugin.getStringList("setup_create_item_lore"));
        inventoryManager.addUrlSkull(inventory, 19, plugin.getString("setup_default_item_name"), "https://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", plugin.getStringList("setup_default_item_lore"));
        inventoryManager.addUrlSkull(inventory, 28, plugin.getString("setup_status_item_name"), "https://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", plugin.getStringList("setup_status_item_name_lore"));

        inventoryManager.addItemStack(inventory, 11, inventoryManager.getCreateItem(WorldType.NORMAL), plugin.getString("setup_normal_world"));
        inventoryManager.addItemStack(inventory, 12, inventoryManager.getCreateItem(WorldType.FLAT), plugin.getString("setup_flat_world"));
        inventoryManager.addItemStack(inventory, 13, inventoryManager.getCreateItem(WorldType.NETHER), plugin.getString("setup_nether_world"));
        inventoryManager.addItemStack(inventory, 14, inventoryManager.getCreateItem(WorldType.END), plugin.getString("setup_end_world"));
        inventoryManager.addItemStack(inventory, 15, inventoryManager.getCreateItem(WorldType.VOID), plugin.getString("setup_void_world"));

        inventoryManager.addItemStack(inventory, 20, inventoryManager.getDefaultItem(WorldType.NORMAL), plugin.getString("setup_normal_world"));
        inventoryManager.addItemStack(inventory, 21, inventoryManager.getDefaultItem(WorldType.FLAT), plugin.getString("setup_flat_world"));
        inventoryManager.addItemStack(inventory, 22, inventoryManager.getDefaultItem(WorldType.NETHER), plugin.getString("setup_nether_world"));
        inventoryManager.addItemStack(inventory, 23, inventoryManager.getDefaultItem(WorldType.END), plugin.getString("setup_end_world"));
        inventoryManager.addItemStack(inventory, 24, inventoryManager.getDefaultItem(WorldType.VOID), plugin.getString("setup_void_world"));
        inventoryManager.addItemStack(inventory, 25, inventoryManager.getDefaultItem(WorldType.IMPORTED), plugin.getString("setup_imported_world"));

        inventoryManager.addItemStack(inventory, 29, inventoryManager.getStatusItem(WorldStatus.NOT_STARTED), plugin.getString("status_not_started"));
        inventoryManager.addItemStack(inventory, 30, inventoryManager.getStatusItem(WorldStatus.IN_PROGRESS), plugin.getString("status_in_progress"));
        inventoryManager.addItemStack(inventory, 31, inventoryManager.getStatusItem(WorldStatus.ALMOST_FINISHED), plugin.getString("status_almost_finished"));
        inventoryManager.addItemStack(inventory, 32, inventoryManager.getStatusItem(WorldStatus.FINISHED), plugin.getString("status_finished"));
        inventoryManager.addItemStack(inventory, 33, inventoryManager.getStatusItem(WorldStatus.ARCHIVE), plugin.getString("status_archive"));
        inventoryManager.addItemStack(inventory, 34, inventoryManager.getStatusItem(WorldStatus.HIDDEN), plugin.getString("status_hidden"));

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "setup_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        InventoryType type = event.getInventory().getType();
        int slot = event.getRawSlot();

        switch (action) {
            case PICKUP_ALL:
            case PICKUP_ONE:
            case PICKUP_SOME:
            case PICKUP_HALF:
            case PLACE_ALL:
            case PLACE_SOME:
            case PLACE_ONE:
            case SWAP_WITH_CURSOR:
                if (type != InventoryType.CHEST) {
                    return;
                }

                event.setCancelled(slot < 45 || slot > 80);
                if (action != InventoryAction.SWAP_WITH_CURSOR) {
                    return;
                }

                if (!(slot >= 45 && slot <= 80)) {
                    if ((slot >= 11 && slot <= 15) || (slot >= 20 && slot <= 25) || (slot >= 29 && slot <= 34)) {
                        ItemStack itemStack = event.getCursor();
                        event.setCurrentItem(itemStack);
                        player.setItemOnCursor(null);
                    }
                }
                break;
            default:
                event.setCancelled(true);
                break;
        }
    }
}
