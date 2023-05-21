/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.player.PlayerManager;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.world.BuildWorld;
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

import java.util.AbstractMap;

public class StatusInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryUtils inventoryUtils;
    private final PlayerManager playerManager;

    public StatusInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtil();
        this.playerManager = plugin.getPlayerManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        String selectedWorldName = playerManager.getSelectedWorldName(player);
        if (selectedWorldName == null) {
            selectedWorldName = "N/A";
        }

        String title = Messages.getString("status_title", new AbstractMap.SimpleEntry<>("%world%", selectedWorldName));
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        fillGuiWithGlass(player, inventory);

        addItem(player, inventory, 10, inventoryUtils.getStatusItem(WorldStatus.NOT_STARTED), Messages.getString("status_not_started"), WorldStatus.NOT_STARTED);
        addItem(player, inventory, 11, inventoryUtils.getStatusItem(WorldStatus.IN_PROGRESS), Messages.getString("status_in_progress"), WorldStatus.IN_PROGRESS);
        addItem(player, inventory, 12, inventoryUtils.getStatusItem(WorldStatus.ALMOST_FINISHED), Messages.getString("status_almost_finished"), WorldStatus.ALMOST_FINISHED);
        addItem(player, inventory, 13, inventoryUtils.getStatusItem(WorldStatus.FINISHED), Messages.getString("status_finished"), WorldStatus.FINISHED);
        addItem(player, inventory, 14, inventoryUtils.getStatusItem(WorldStatus.ARCHIVE), Messages.getString("status_archive"), WorldStatus.ARCHIVE);
        addItem(player, inventory, 16, inventoryUtils.getStatusItem(WorldStatus.HIDDEN), Messages.getString("status_hidden"), WorldStatus.HIDDEN);

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 9; i++) {
            inventoryUtils.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 17; i <= 26; i++) {
            inventoryUtils.addGlassPane(plugin, player, inventory, i);
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

        BuildWorld cachedWorld = playerManager.getBuildPlayer(player).getCachedWorld();
        if (cachedWorld != null && cachedWorld.getData().status().get() == worldStatus) {
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

        String title = Messages.getString("status_title", new AbstractMap.SimpleEntry<>("%world%", selectedWorldName));
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

        BuildWorld buildWorld = playerManager.getBuildPlayer(player).getCachedWorld();
        if (buildWorld == null) {
            player.closeInventory();
            Messages.sendMessage(player, "worlds_setstatus_error");
            return;
        }

        WorldData worldData = buildWorld.getData();
        switch (event.getSlot()) {
            case 10:
                worldData.status().set(WorldStatus.NOT_STARTED);
                break;
            case 11:
                worldData.status().set(WorldStatus.IN_PROGRESS);
                break;
            case 12:
                worldData.status().set(WorldStatus.ALMOST_FINISHED);
                break;
            case 13:
                worldData.status().set(WorldStatus.FINISHED);
                break;
            case 14:
                worldData.status().set(WorldStatus.ARCHIVE);
                break;
            case 16:
                worldData.status().set(WorldStatus.HIDDEN);
                break;
            default:
                XSound.BLOCK_CHEST_OPEN.play(player);
                plugin.getEditInventory().openInventory(player, buildWorld);
                return;
        }

        playerManager.forceUpdateSidebar(buildWorld);
        player.closeInventory();

        XSound.ENTITY_CHICKEN_EGG.play(player);
        Messages.sendMessage(player, "worlds_setstatus_set",
                new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()),
                new AbstractMap.SimpleEntry<>("%status%", buildWorld.getData().status().get().getName())
        );
        playerManager.getBuildPlayer(player).setCachedWorld(null);
    }
}