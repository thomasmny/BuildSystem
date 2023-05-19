/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world.modification;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.data.WorldStatus;
import de.eintosti.buildsystem.world.data.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SetupInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryUtils inventoryUtils;

    public SetupInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtil();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, Messages.getString("setup_title"));
        fillGuiWithGlass(player, inventory);

        inventoryUtils.addUrlSkull(inventory, 10, Messages.getString("setup_create_item_name"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", Messages.getStringList("setup_create_item_lore"));
        inventoryUtils.addUrlSkull(inventory, 19, Messages.getString("setup_default_item_name"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", Messages.getStringList("setup_default_item_lore"));
        inventoryUtils.addUrlSkull(inventory, 28, Messages.getString("setup_status_item_name"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158", Messages.getStringList("setup_status_item_name_lore"));

        inventoryUtils.addItemStack(inventory, 11, inventoryUtils.getCreateItem(WorldType.NORMAL), Messages.getString("setup_normal_world"));
        inventoryUtils.addItemStack(inventory, 12, inventoryUtils.getCreateItem(WorldType.FLAT), Messages.getString("setup_flat_world"));
        inventoryUtils.addItemStack(inventory, 13, inventoryUtils.getCreateItem(WorldType.NETHER), Messages.getString("setup_nether_world"));
        inventoryUtils.addItemStack(inventory, 14, inventoryUtils.getCreateItem(WorldType.END), Messages.getString("setup_end_world"));
        inventoryUtils.addItemStack(inventory, 15, inventoryUtils.getCreateItem(WorldType.VOID), Messages.getString("setup_void_world"));

        inventoryUtils.addItemStack(inventory, 20, inventoryUtils.getDefaultItem(WorldType.NORMAL), Messages.getString("setup_normal_world"));
        inventoryUtils.addItemStack(inventory, 21, inventoryUtils.getDefaultItem(WorldType.FLAT), Messages.getString("setup_flat_world"));
        inventoryUtils.addItemStack(inventory, 22, inventoryUtils.getDefaultItem(WorldType.NETHER), Messages.getString("setup_nether_world"));
        inventoryUtils.addItemStack(inventory, 23, inventoryUtils.getDefaultItem(WorldType.END), Messages.getString("setup_end_world"));
        inventoryUtils.addItemStack(inventory, 24, inventoryUtils.getDefaultItem(WorldType.VOID), Messages.getString("setup_void_world"));
        inventoryUtils.addItemStack(inventory, 25, inventoryUtils.getDefaultItem(WorldType.IMPORTED), Messages.getString("setup_imported_world"));

        inventoryUtils.addItemStack(inventory, 29, inventoryUtils.getStatusItem(WorldStatus.NOT_STARTED), Messages.getString("status_not_started"));
        inventoryUtils.addItemStack(inventory, 30, inventoryUtils.getStatusItem(WorldStatus.IN_PROGRESS), Messages.getString("status_in_progress"));
        inventoryUtils.addItemStack(inventory, 31, inventoryUtils.getStatusItem(WorldStatus.ALMOST_FINISHED), Messages.getString("status_almost_finished"));
        inventoryUtils.addItemStack(inventory, 32, inventoryUtils.getStatusItem(WorldStatus.FINISHED), Messages.getString("status_finished"));
        inventoryUtils.addItemStack(inventory, 33, inventoryUtils.getStatusItem(WorldStatus.ARCHIVE), Messages.getString("status_archive"));
        inventoryUtils.addItemStack(inventory, 34, inventoryUtils.getStatusItem(WorldStatus.HIDDEN), Messages.getString("status_hidden"));

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            inventoryUtils.addGlassPane(plugin, player, inventory, i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtils.checkIfValidClick(event, "setup_title")) {
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