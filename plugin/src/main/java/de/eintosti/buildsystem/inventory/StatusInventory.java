/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author einTosti
 */
public class StatusInventory {
    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public StatusInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, plugin.getString("status_title").replace("%world%", inventoryManager.selectedWorld(player)));
        fillGuiWithGlass(player, inventory);

        addItem(player, inventory, 10, inventoryManager.getStatusItem(WorldStatus.NOT_STARTED), plugin.getString("status_not_started"), WorldStatus.NOT_STARTED);
        addItem(player, inventory, 11, inventoryManager.getStatusItem(WorldStatus.IN_PROGRESS), plugin.getString("status_in_progress"), WorldStatus.IN_PROGRESS);
        addItem(player, inventory, 12, inventoryManager.getStatusItem(WorldStatus.ALMOST_FINISHED), plugin.getString("status_almost_finished"), WorldStatus.ALMOST_FINISHED);
        addItem(player, inventory, 13, inventoryManager.getStatusItem(WorldStatus.FINISHED), plugin.getString("status_finished"), WorldStatus.FINISHED);
        addItem(player, inventory, 14, inventoryManager.getStatusItem(WorldStatus.ARCHIVE), plugin.getString("status_archive"), WorldStatus.ARCHIVE);
        addItem(player, inventory, 16, inventoryManager.getStatusItem(WorldStatus.HIDDEN), plugin.getString("status_hidden"), WorldStatus.HIDDEN);

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 9; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 17; i <= 26; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void addItem(Player player, Inventory inventory, int position, XMaterial material, String displayName, WorldStatus worldStatus) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        itemStack.setItemMeta(itemMeta);

        if (plugin.selectedWorld.get(player.getUniqueId()).getStatus() == worldStatus) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }
}
