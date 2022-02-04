/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.SettingsManager;
import com.eintosti.buildsystem.object.settings.Color;
import com.eintosti.buildsystem.object.settings.Settings;
import org.bukkit.Bukkit;
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
public class DesignInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public DesignInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, plugin.getString("design_title"));
        fillGuiWithGlass(inventory, player);

        setItem(player, inventory, 10, XMaterial.RED_STAINED_GLASS, plugin.getString("design_red"), Color.RED);
        setItem(player, inventory, 11, XMaterial.ORANGE_STAINED_GLASS, plugin.getString("design_orange"), Color.ORANGE);
        setItem(player, inventory, 12, XMaterial.YELLOW_STAINED_GLASS, plugin.getString("design_yellow"), Color.YELLOW);
        setItem(player, inventory, 13, XMaterial.PINK_STAINED_GLASS, plugin.getString("design_pink"), Color.PINK);
        setItem(player, inventory, 14, XMaterial.MAGENTA_STAINED_GLASS, plugin.getString("design_magenta"), Color.MAGENTA);
        setItem(player, inventory, 15, XMaterial.PURPLE_STAINED_GLASS, plugin.getString("design_purple"), Color.PURPLE);
        setItem(player, inventory, 16, XMaterial.BROWN_STAINED_GLASS, plugin.getString("design_brown"), Color.BROWN);

        setItem(player, inventory, 18, XMaterial.LIME_STAINED_GLASS, plugin.getString("design_lime"), Color.LIME);
        setItem(player, inventory, 19, XMaterial.GREEN_STAINED_GLASS, plugin.getString("design_green"), Color.GREEN);
        setItem(player, inventory, 20, XMaterial.BLUE_STAINED_GLASS, plugin.getString("design_blue"), Color.BLUE);
        setItem(player, inventory, 21, XMaterial.CYAN_STAINED_GLASS, plugin.getString("design_aqua"), Color.CYAN);
        setItem(player, inventory, 22, XMaterial.LIGHT_BLUE_STAINED_GLASS, plugin.getString("design_light_blue"), Color.LIGHT_BLUE);
        setItem(player, inventory, 23, XMaterial.WHITE_STAINED_GLASS, plugin.getString("design_white"), Color.WHITE);
        setItem(player, inventory, 24, XMaterial.LIGHT_GRAY_STAINED_GLASS, plugin.getString("design_grey"), Color.LIGHT_GRAY);
        setItem(player, inventory, 25, XMaterial.GRAY_STAINED_GLASS, plugin.getString("design_dark_grey"), Color.GRAY);
        setItem(player, inventory, 26, XMaterial.BLACK_STAINED_GLASS, plugin.getString("design_black"), Color.BLACK);

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Inventory inventory, Player player) {
        for (int i = 0; i <= 8; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 27; i <= 35; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void setItem(Player player, Inventory inventory, int position, XMaterial material, String displayName, Color color) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        Settings settings = settingsManager.getSettings(player);

        ItemStack itemStack = inventoryManager.getItemStack(material, settings.getGlassColor() == color ? "§a" + displayName : "§7" + displayName);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        itemStack.setItemMeta(itemMeta);
        if (settings.getGlassColor() == color) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "design_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Settings settings = plugin.getSettingsManager().getSettings(player);
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack.getType().toString().contains("STAINED_GLASS_PANE")) {
            plugin.getSettingsInventory().openInventory(player);
            return;
        }

        switch (event.getSlot()) {
            case 10:
                settings.setGlassColor(Color.RED);
                break;
            case 11:
                settings.setGlassColor(Color.ORANGE);
                break;
            case 12:
                settings.setGlassColor(Color.YELLOW);
                break;
            case 13:
                settings.setGlassColor(Color.PINK);
                break;
            case 14:
                settings.setGlassColor(Color.MAGENTA);
                break;
            case 15:
                settings.setGlassColor(Color.PURPLE);
                break;
            case 16:
                settings.setGlassColor(Color.BROWN);
                break;
            case 18:
                settings.setGlassColor(Color.LIME);
                break;
            case 19:
                settings.setGlassColor(Color.GREEN);
                break;
            case 20:
                settings.setGlassColor(Color.BLUE);
                break;
            case 21:
                settings.setGlassColor(Color.CYAN);
                break;
            case 22:
                settings.setGlassColor(Color.LIGHT_BLUE);
                break;
            case 23:
                settings.setGlassColor(Color.WHITE);
                break;
            case 24:
                settings.setGlassColor(Color.LIGHT_GRAY);
                break;
            case 25:
                settings.setGlassColor(Color.GRAY);
                break;
            case 26:
                settings.setGlassColor(Color.BLACK);
                break;
        }

        openInventory(player);
    }
}
