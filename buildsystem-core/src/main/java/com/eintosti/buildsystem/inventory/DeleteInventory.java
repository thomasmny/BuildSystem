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

    private Inventory getInventory(BuildWorld buildWorld) {
        Inventory inventory = Bukkit.createInventory(null, 27, plugin.getString("delete_title"));
        fillGuiWithGlass(inventory);

        inventoryManager.addItemStack(inventory, 11, XMaterial.LIME_DYE, plugin.getString("delete_world_confirm"));
        inventoryManager.addItemStack(inventory, 13, XMaterial.FILLED_MAP, plugin.getString("delete_world_name").replace("%world%", buildWorld.getName()), plugin.getStringList("delete_world_name_lore"));
        inventoryManager.addItemStack(inventory, 15, XMaterial.RED_DYE, plugin.getString("delete_world_cancel"));

        return inventory;
    }

    public void openInventory(Player player, BuildWorld buildWorld) {
        player.openInventory(getInventory(buildWorld));
    }

    private void fillGuiWithGlass(Inventory inventory) {
        final int[] greenSlots = new int[]{0, 1, 2, 3, 9, 10, 12, 18, 19, 20, 21};
        final int[] blackSlots = new int[]{4, 22};
        final int[] redSlots = new int[]{5, 6, 7, 8, 14, 16, 17, 23, 24, 25, 26};

        for (int slot : greenSlots) {
            inventoryManager.addItemStack(inventory, slot, XMaterial.LIME_STAINED_GLASS_PANE, "§f");
        }
        for (int slot : blackSlots) {
            inventoryManager.addItemStack(inventory, slot, XMaterial.BLACK_STAINED_GLASS_PANE, "§f");
        }
        for (int slot : redSlots) {
            inventoryManager.addItemStack(inventory, slot, XMaterial.RED_STAINED_GLASS_PANE, "§f");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "delete_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = plugin.getPlayerManager().getBuildPlayer(player).getCachedWorld();
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("worlds_delete_error"));
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();
        if (slot == 11) {
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.closeInventory();
            plugin.getWorldManager().deleteWorld(player, buildWorld);
        } else if (slot == 15) {
            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_delete_canceled").replace("%world%", buildWorld.getName()));

        }
    }
}