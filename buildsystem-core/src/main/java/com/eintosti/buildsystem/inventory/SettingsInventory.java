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
import com.eintosti.buildsystem.api.settings.NavigatorType;
import com.eintosti.buildsystem.api.settings.WorldSort;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.NoClipManager;
import com.eintosti.buildsystem.manager.SettingsManager;
import com.eintosti.buildsystem.object.settings.CraftSettings;
import com.eintosti.buildsystem.util.ConfigValues;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * @author einTosti
 */
public class SettingsInventory implements Listener {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private final InventoryManager inventoryManager;
    private final SettingsManager settingsManager;

    public SettingsInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.inventoryManager = plugin.getInventoryManager();
        this.settingsManager = plugin.getSettingsManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, plugin.getString("settings_title"));
        fillGuiWithGlass(player, inventory);

        CraftSettings settings = settingsManager.getSettings(player);
        addDesignItem(inventory, player);
        addClearInventoryItem(inventory, player);
        addSettingsItem(inventory, 13, XMaterial.DIAMOND_AXE, settings.isDisableInteract(), plugin.getString("settings_disableinteract_item"), plugin.getStringList("settings_disableinteract_lore"));
        addSettingsItem(inventory, 14, XMaterial.ENDER_EYE, settings.isHidePlayers(), plugin.getString("settings_hideplayers_item"), plugin.getStringList("settings_hideplayers_lore"));
        addSettingsItem(inventory, 15, XMaterial.OAK_SIGN, settings.isInstantPlaceSigns(), plugin.getString("settings_instantplacesigns_item"), plugin.getStringList("settings_instantplacesigns_lore"));
        addSettingsItem(inventory, 20, XMaterial.SLIME_BLOCK, settings.isKeepNavigator(), plugin.getString("settings_keep_navigator_item"), plugin.getStringList("settings_keep_navigator_lore"));
        addSettingsItem(inventory, 21, configValues.getNavigatorItem(), settings.getNavigatorType().equals(NavigatorType.NEW), plugin.getString("settings_new_navigator_item"), plugin.getStringList("settings_new_navigator_lore"));
        addSettingsItem(inventory, 22, XMaterial.GOLDEN_CARROT, settings.isNightVision(), plugin.getString("settings_nightvision_item"), plugin.getStringList("settings_nightvision_lore"));
        addSettingsItem(inventory, 23, XMaterial.BRICKS, settings.isNoClip(), plugin.getString("settings_no_clip_item"), plugin.getStringList("settings_no_clip_lore"));
        addSettingsItem(inventory, 24, XMaterial.IRON_TRAPDOOR, settings.isOpenTrapDoor(), plugin.getString("settings_open_trapdoors_item"), plugin.getStringList("settings_open_trapdoors_lore"));
        addSettingsItem(inventory, 29, XMaterial.FERN, settings.isPlacePlants(), plugin.getString("settings_placeplants_item"), plugin.getStringList("settings_placeplants_lore"));
        addSettingsItem(inventory, 30, XMaterial.PAPER, settings.isScoreboard(), configValues.isScoreboard() ? plugin.getString("settings_scoreboard_item") : plugin.getString("settings_scoreboard_disabled_item"),
                configValues.isScoreboard() ? plugin.getStringList("settings_scoreboard_lore") : plugin.getStringList("settings_scoreboard_disabled_lore"));
        addSettingsItem(inventory, 31, XMaterial.SMOOTH_STONE_SLAB, settings.isSlabBreaking(), plugin.getString("settings_slab_breaking_item"), plugin.getStringList("settings_slab_breaking_lore"));
        addSettingsItem(inventory, 32, XMaterial.MAGMA_CREAM, settings.isSpawnTeleport(), plugin.getString("settings_spawnteleport_item"), plugin.getStringList("settings_spawnteleport_lore"));
        addWorldSortItem(inventory, player);

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void addSettingsItem(Inventory inventory, int position, XMaterial material, boolean enabled, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
            itemMeta.setLore(lore);
            itemMeta.addItemFlags(ItemFlag.values());
        }

        itemStack.setItemMeta(itemMeta);
        if (enabled) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }

    private void addClearInventoryItem(Inventory inventory, Player player) {
        CraftSettings settings = settingsManager.getSettings(player);
        XMaterial xMaterial = settings.isClearInventory() ? XMaterial.MINECART : XMaterial.CHEST_MINECART;
        addSettingsItem(inventory, 12, xMaterial, settings.isClearInventory(), plugin.getString("settings_clear_inventory_item"), plugin.getStringList("settings_clear_inventory_lore"));
    }

    private void addDesignItem(Inventory inventory, Player player) {
        ItemStack itemStack = inventoryManager.getItemStack(inventoryManager.getColouredGlass(plugin, player), plugin.getString("settings_change_design_item"));
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemMeta.setLore(plugin.getStringList("settings_change_design_lore"));
        }
        itemStack.setItemMeta(itemMeta);
        itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);

        inventory.setItem(11, itemStack);
    }

    private void addWorldSortItem(Inventory inventory, Player player) {
        CraftSettings settings = settingsManager.getSettings(player);

        String url;
        List<String> lore;
        switch (settings.getWorldSort()) {
            default: //NAME_A_TO_Z
                url = "https://textures.minecraft.net/texture/a67d813ae7ffe5be951a4f41f2aa619a5e3894e85ea5d4986f84949c63d7672e";
                lore = plugin.getStringList("settings_worldsort_lore_alphabetically_name_az");
                break;
            case NAME_Z_TO_A:
                url = "https://textures.minecraft.net/texture/90582b9b5d97974b11461d63eced85f438a3eef5dc3279f9c47e1e38ea54ae8d";
                lore = plugin.getStringList("settings_worldsort_lore_alphabetically_name_za");
                break;
            case PROJECT_A_TO_Z:
                url = "https://textures.minecraft.net/texture/2ac58b1a3b53b9481e317a1ea4fc5eed6bafca7a25e741a32e4e3c2841278c";
                lore = plugin.getStringList("settings_worldsort_lore_alphabetically_project_az");
                break;
            case PROJECT_Z_TO_A:
                url = "https://textures.minecraft.net/texture/4e91200df1cae51acc071f85c7f7f5b8449d39bb32f363b0aa51dbc85d133e";
                lore = plugin.getStringList("settings_worldsort_lore_alphabetically_project_za");
                break;
            case NEWEST_FIRST:
                url = "https://textures.minecraft.net/texture/71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530";
                lore = plugin.getStringList("settings_worldsort_lore_date_newest");
                break;
            case OLDEST_FIRST:
                url = "https://textures.minecraft.net/texture/e67caf7591b38e125a8017d58cfc6433bfaf84cd499d794f41d10bff2e5b840";
                lore = plugin.getStringList("settings_worldsort_lore_date_oldest");
                break;
        }

        ItemStack itemStack = inventoryManager.getUrlSkull(plugin.getString("settings_worldsort_item"), url);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setLore(lore);
        }

        itemStack.setItemMeta(itemMeta);
        inventory.setItem(33, itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "settings_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        CraftSettings settings = settingsManager.getSettings(player);

        switch (event.getSlot()) {
            case 11:
                plugin.getDesignInventory().openInventory(player);
                XSound.ENTITY_ITEM_PICKUP.play(player);
                return;
            case 12:
                settings.setClearInventory(!settings.isClearInventory());
                break;
            case 13:
                settings.setDisableInteract(!settings.isDisableInteract());
                break;
            case 14:
                settings.setHidePlayers(!settings.isHidePlayers());
                toggleHidePlayers(player, settings);
                break;
            case 15:
                settings.setInstantPlaceSigns(!settings.isInstantPlaceSigns());
                break;

            case 20:
                settings.setKeepNavigator(!settings.isKeepNavigator());
                break;
            case 21:
                if (settings.getNavigatorType().equals(NavigatorType.OLD)) {
                    settings.setNavigatorType(NavigatorType.NEW);
                } else {
                    settings.setNavigatorType(NavigatorType.OLD);
                    plugin.getArmorStandManager().removeArmorStands(player);
                    if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                    }
                }
                break;
            case 22:
                if (!settings.isNightVision()) {
                    settings.setNightVision(true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                } else {
                    settings.setNightVision(false);
                    if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    }
                }
                break;
            case 23:
                NoClipManager noClipManager = plugin.getNoClipManager();
                if (!settings.isNoClip()) {
                    settings.setNoClip(true);
                    noClipManager.startNoClip(player);
                } else {
                    settings.setNoClip(false);
                    noClipManager.stopNoClip(player.getUniqueId());
                }
                break;
            case 24:
                settings.setOpenTrapDoor(!settings.isOpenTrapDoor());
                break;

            case 29:
                settings.setPlacePlants(!settings.isPlacePlants());
                break;
            case 30:
                if (!configValues.isScoreboard()) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                if (settings.isScoreboard()) {
                    settings.setScoreboard(false);
                    settingsManager.stopScoreboard(player);
                } else {
                    settings.setScoreboard(true);
                    settingsManager.startScoreboard(player);
                    plugin.getPlayerManager().forceUpdateSidebar(player);
                }
                break;
            case 31:
                settings.setSlabBreaking(!settings.isSlabBreaking());
                break;
            case 32:
                settings.setSpawnTeleport(!settings.isSpawnTeleport());
                break;
            case 33:
                switch (settings.getWorldSort()) {
                    case NAME_A_TO_Z:
                        settings.setWorldSort(WorldSort.NAME_Z_TO_A);
                        break;
                    case NAME_Z_TO_A:
                        settings.setWorldSort(WorldSort.PROJECT_A_TO_Z);
                        break;
                    case PROJECT_A_TO_Z:
                        settings.setWorldSort(WorldSort.PROJECT_Z_TO_A);
                        break;
                    case PROJECT_Z_TO_A:
                        settings.setWorldSort(WorldSort.NEWEST_FIRST);
                        break;
                    case NEWEST_FIRST:
                        settings.setWorldSort(WorldSort.OLDEST_FIRST);
                        break;
                    case OLDEST_FIRST:
                        settings.setWorldSort(WorldSort.NAME_A_TO_Z);
                        break;
                }
                break;
            default:
                return;
        }

        XSound.ENTITY_ITEM_PICKUP.play(player);
        plugin.getSettingsInventory().openInventory(player);
    }

    @SuppressWarnings("deprecation")
    private void toggleHidePlayers(Player player, CraftSettings settings) {
        if (settings.isHidePlayers()) {
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        } else {
            Bukkit.getOnlinePlayers().forEach(player::showPlayer);
        }
    }
}
