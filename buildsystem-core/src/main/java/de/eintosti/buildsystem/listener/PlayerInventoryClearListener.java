/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.event.player.PlayerInventoryClearEvent;
import de.eintosti.buildsystem.settings.Settings;
import de.eintosti.buildsystem.settings.SettingsManager;
import de.eintosti.buildsystem.util.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerInventoryClearListener implements Listener {

    private final BuildSystem plugin;
    private final InventoryUtils inventoryUtils;
    private final SettingsManager settingsManager;

    public PlayerInventoryClearListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtil();
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
        ItemStack navigatorItem = inventoryUtils.getItemStack(plugin.getConfigValues().getNavigatorItem(), Messages.getString("navigator_item", player));

        event.getNavigatorSlots().forEach(slot -> playerInventory.setItem(slot, navigatorItem));
    }
}