/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.api.world.WorldStatus;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.google.common.collect.Sets;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

/**
 * @author einTosti
 */
public class ArchiveInventory extends FilteredWorldsInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public ArchiveInventory(BuildSystem plugin) {
        super(plugin, "archive_title", "archive_no_worlds", false, Sets.newHashSet(WorldStatus.ARCHIVE));

        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    protected Inventory createInventory(Player player) {
        Inventory inventory = super.createInventory(player);
        inventoryManager.addGlassPane(plugin, player, inventory, 49);
        return inventory;
    }
}
