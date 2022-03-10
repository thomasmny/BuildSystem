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
import com.eintosti.buildsystem.manager.PlayerManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.data.WorldStatus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author einTosti
 */
public class StatusInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final PlayerManager playerManager;

    public StatusInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.playerManager = plugin.getPlayerManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        String selectedWorldName = playerManager.getSelectedWorldName(player);
        if (selectedWorldName == null) {
            selectedWorldName = "N/A";
        }

        String title = plugin.getString("status_title").replace("%world%", selectedWorldName);
        Inventory inventory = Bukkit.createInventory(null, 27, title);
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

        if (plugin.getPlayerManager().getSelectedWorld().get(player.getUniqueId()).getStatus() == worldStatus) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String selectedWorldName = playerManager.getSelectedWorldName(player);
        if (selectedWorldName == null) {
            return;
        }

        String title = plugin.getString("status_title").replace("%world%", selectedWorldName);
        if (!event.getView().getTitle().equals(title)) {
            return;
        }

        event.setCancelled(true);
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) {
            return;
        }

        Material itemType = itemStack.getType();
        if (itemType == Material.AIR || !itemStack.hasItemMeta()) {
            return;
        }

        BuildWorld buildWorld = playerManager.getSelectedWorld().get(player.getUniqueId());
        if (buildWorld == null) {
            player.closeInventory();
            player.sendMessage(plugin.getString("worlds_setstatus_error"));
            return;
        }

        switch (event.getSlot()) {
            case 10:
                buildWorld.setStatus(WorldStatus.NOT_STARTED);
                break;
            case 11:
                buildWorld.setStatus(WorldStatus.IN_PROGRESS);
                break;
            case 12:
                buildWorld.setStatus(WorldStatus.ALMOST_FINISHED);
                break;
            case 13:
                buildWorld.setStatus(WorldStatus.FINISHED);
                break;
            case 14:
                buildWorld.setStatus(WorldStatus.ARCHIVE);
                break;
            case 16:
                buildWorld.setStatus(WorldStatus.HIDDEN);
                break;
            default:
                XSound.BLOCK_CHEST_OPEN.play(player);
                plugin.getEditInventory().openInventory(player, buildWorld);
                return;
        }

        playerManager.forceUpdateSidebar(buildWorld);
        player.closeInventory();

        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.sendMessage(plugin.getString("worlds_setstatus_set")
                .replace("%world%", buildWorld.getName())
                .replace("%status%", buildWorld.getStatusName())
        );
        playerManager.getSelectedWorld().remove(player.getUniqueId());
    }
}
