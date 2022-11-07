/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.event.player.PlayerInventoryClearEvent;
import com.eintosti.buildsystem.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.List;

/**
 * @author einTosti
 */
public class InventoryCreativeListener implements Listener {

    private final BuildSystem plugin;
    private final InventoryUtil inventoryManager;

    public InventoryCreativeListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClearInventory(InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE || event.getSlotType() != InventoryType.SlotType.QUICKBAR || event.getAction() != InventoryAction.PLACE_ALL) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        List<Integer> navigatorSlots = inventoryManager.getNavigatorSlots(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PlayerInventoryClearEvent playerInventoryClearEvent = new PlayerInventoryClearEvent(player, navigatorSlots);
            Bukkit.getServer().getPluginManager().callEvent(playerInventoryClearEvent);
        }, 2L);
    }
}