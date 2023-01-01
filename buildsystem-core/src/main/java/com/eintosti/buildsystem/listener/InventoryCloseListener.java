/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.world.data.WorldStatus;
import com.eintosti.buildsystem.world.data.WorldType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 */
public class InventoryCloseListener implements Listener {

    private final InventoryUtil inventoryUtil;

    public InventoryCloseListener(BuildSystem plugin) {
        this.inventoryUtil = plugin.getInventoryUtil();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSetupInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(Messages.getString("setup_title"))) {
            return;
        }
        setNewItems(event);
    }

    private void setNewItems(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        ItemStack normalCreateItem = inventory.getItem(11);
        ItemStack flatCreateItem = inventory.getItem(12);
        ItemStack netherCreateItem = inventory.getItem(13);
        ItemStack endCreateItem = inventory.getItem(14);
        ItemStack voidCreateItem = inventory.getItem(15);

        inventoryUtil.setCreateItem(WorldType.NORMAL, normalCreateItem != null ? XMaterial.matchXMaterial(normalCreateItem) : null);
        inventoryUtil.setCreateItem(WorldType.FLAT, flatCreateItem != null ? XMaterial.matchXMaterial(flatCreateItem) : null);
        inventoryUtil.setCreateItem(WorldType.NETHER, netherCreateItem != null ? XMaterial.matchXMaterial(netherCreateItem) : null);
        inventoryUtil.setCreateItem(WorldType.END, endCreateItem != null ? XMaterial.matchXMaterial(endCreateItem) : null);
        inventoryUtil.setCreateItem(WorldType.VOID, voidCreateItem != null ? XMaterial.matchXMaterial(voidCreateItem) : null);

        ItemStack normalDefaultItem = inventory.getItem(20);
        ItemStack flatDefaultItem = inventory.getItem(21);
        ItemStack netherDefaultItem = inventory.getItem(22);
        ItemStack endDefaultItem = inventory.getItem(23);
        ItemStack voidDefaultItem = inventory.getItem(24);
        ItemStack importedDefaultItem = inventory.getItem(25);

        inventoryUtil.setDefaultItem(WorldType.NORMAL, normalDefaultItem != null ? XMaterial.matchXMaterial(normalDefaultItem) : null);
        inventoryUtil.setDefaultItem(WorldType.FLAT, flatDefaultItem != null ? XMaterial.matchXMaterial(flatDefaultItem) : null);
        inventoryUtil.setDefaultItem(WorldType.NETHER, netherDefaultItem != null ? XMaterial.matchXMaterial(netherDefaultItem) : null);
        inventoryUtil.setDefaultItem(WorldType.END, endDefaultItem != null ? XMaterial.matchXMaterial(endDefaultItem) : null);
        inventoryUtil.setDefaultItem(WorldType.VOID, voidDefaultItem != null ? XMaterial.matchXMaterial(voidDefaultItem) : null);
        inventoryUtil.setDefaultItem(WorldType.IMPORTED, importedDefaultItem != null ? XMaterial.matchXMaterial(importedDefaultItem) : null);

        ItemStack notStartedStatusItem = inventory.getItem(29);
        ItemStack inProgressStatusItem = inventory.getItem(30);
        ItemStack almostFinishedStatusItem = inventory.getItem(31);
        ItemStack finishedStatusItem = inventory.getItem(32);
        ItemStack archiveStatusItem = inventory.getItem(33);
        ItemStack hiddenStatusItem = inventory.getItem(34);

        inventoryUtil.setStatusItem(WorldStatus.NOT_STARTED, notStartedStatusItem != null ? XMaterial.matchXMaterial(notStartedStatusItem) : null);
        inventoryUtil.setStatusItem(WorldStatus.IN_PROGRESS, inProgressStatusItem != null ? XMaterial.matchXMaterial(inProgressStatusItem) : null);
        inventoryUtil.setStatusItem(WorldStatus.ALMOST_FINISHED, almostFinishedStatusItem != null ? XMaterial.matchXMaterial(almostFinishedStatusItem) : null);
        inventoryUtil.setStatusItem(WorldStatus.FINISHED, finishedStatusItem != null ? XMaterial.matchXMaterial(finishedStatusItem) : null);
        inventoryUtil.setStatusItem(WorldStatus.ARCHIVE, archiveStatusItem != null ? XMaterial.matchXMaterial(archiveStatusItem) : null);
        inventoryUtil.setStatusItem(WorldStatus.HIDDEN, hiddenStatusItem != null ? XMaterial.matchXMaterial(hiddenStatusItem) : null);
    }
}