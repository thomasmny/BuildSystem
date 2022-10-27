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
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.SettingsManager;
import com.eintosti.buildsystem.object.settings.Settings;
import com.eintosti.buildsystem.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * @author einTosti
 */
public class PlayerInventoryClearListener implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;

    public PlayerInventoryClearListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInventoryClear(PlayerInventoryClearEvent event) {
        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);
        if (!settings.isKeepNavigator() || !player.hasPermission("buildsystem.navigator.item")) {
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        ItemStack navigatorItem = inventoryManager.getItemStack(plugin.getConfigValues().getNavigatorItem(), Messages.getString("navigator_item"));

        event.getNavigatorSlots().forEach(slot -> playerInventory.setItem(slot, navigatorItem));
    }
}
