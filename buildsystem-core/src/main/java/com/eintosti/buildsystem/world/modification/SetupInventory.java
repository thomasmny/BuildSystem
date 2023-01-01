/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.world.modification;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.world.data.WorldStatus;
import com.eintosti.buildsystem.world.data.WorldType;
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
    private final InventoryUtil inventoryUtil;

    public SetupInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryUtil = plugin.getInventoryUtil();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, Messages.getString("setup_title"));
        fillGuiWithGlass(player, inventory);

        inventoryUtil.addUrlSkull(inventory, 10, Messages.getString("setup_create_item_name"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", Messages.getStringList("setup_create_item_lore"));
        inventoryUtil.addUrlSkull(inventory, 19, Messages.getString("setup_default_item_name"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", Messages.getStringList("setup_default_item_lore"));
        inventoryUtil.addUrlSkull(inventory, 28, Messages.getString("setup_status_item_name"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", Messages.getStringList("setup_status_item_name_lore"));

        inventoryUtil.addItemStack(inventory, 11, inventoryUtil.getCreateItem(WorldType.NORMAL), Messages.getString("setup_normal_world"));
        inventoryUtil.addItemStack(inventory, 12, inventoryUtil.getCreateItem(WorldType.FLAT), Messages.getString("setup_flat_world"));
        inventoryUtil.addItemStack(inventory, 13, inventoryUtil.getCreateItem(WorldType.NETHER), Messages.getString("setup_nether_world"));
        inventoryUtil.addItemStack(inventory, 14, inventoryUtil.getCreateItem(WorldType.END), Messages.getString("setup_end_world"));
        inventoryUtil.addItemStack(inventory, 15, inventoryUtil.getCreateItem(WorldType.VOID), Messages.getString("setup_void_world"));

        inventoryUtil.addItemStack(inventory, 20, inventoryUtil.getDefaultItem(WorldType.NORMAL), Messages.getString("setup_normal_world"));
        inventoryUtil.addItemStack(inventory, 21, inventoryUtil.getDefaultItem(WorldType.FLAT), Messages.getString("setup_flat_world"));
        inventoryUtil.addItemStack(inventory, 22, inventoryUtil.getDefaultItem(WorldType.NETHER), Messages.getString("setup_nether_world"));
        inventoryUtil.addItemStack(inventory, 23, inventoryUtil.getDefaultItem(WorldType.END), Messages.getString("setup_end_world"));
        inventoryUtil.addItemStack(inventory, 24, inventoryUtil.getDefaultItem(WorldType.VOID), Messages.getString("setup_void_world"));
        inventoryUtil.addItemStack(inventory, 25, inventoryUtil.getDefaultItem(WorldType.IMPORTED), Messages.getString("setup_imported_world"));

        inventoryUtil.addItemStack(inventory, 29, inventoryUtil.getStatusItem(WorldStatus.NOT_STARTED), Messages.getString("status_not_started"));
        inventoryUtil.addItemStack(inventory, 30, inventoryUtil.getStatusItem(WorldStatus.IN_PROGRESS), Messages.getString("status_in_progress"));
        inventoryUtil.addItemStack(inventory, 31, inventoryUtil.getStatusItem(WorldStatus.ALMOST_FINISHED), Messages.getString("status_almost_finished"));
        inventoryUtil.addItemStack(inventory, 32, inventoryUtil.getStatusItem(WorldStatus.FINISHED), Messages.getString("status_finished"));
        inventoryUtil.addItemStack(inventory, 33, inventoryUtil.getStatusItem(WorldStatus.ARCHIVE), Messages.getString("status_archive"));
        inventoryUtil.addItemStack(inventory, 34, inventoryUtil.getStatusItem(WorldStatus.HIDDEN), Messages.getString("status_hidden"));

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            inventoryUtil.addGlassPane(plugin, player, inventory, i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtil.checkIfValidClick(event, "setup_title")) {
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