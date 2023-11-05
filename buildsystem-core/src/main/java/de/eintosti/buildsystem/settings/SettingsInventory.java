/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.settings;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.navigator.settings.NavigatorType;
import de.eintosti.buildsystem.util.InventoryUtils;
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

public class SettingsInventory implements Listener {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private final InventoryUtils inventoryUtils;
    private final SettingsManager settingsManager;

    public SettingsInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.inventoryUtils = plugin.getInventoryUtil();
        this.settingsManager = plugin.getSettingsManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, Messages.getString("settings_title"));
        fillGuiWithGlass(player, inventory);

        Settings settings = settingsManager.getSettings(player);
        addDesignItem(inventory, player);
        addClearInventoryItem(inventory, player);
        addSettingsItem(inventory, 13, XMaterial.DIAMOND_AXE, settings.isDisableInteract(), Messages.getString("settings_disableinteract_item"), Messages.getStringList("settings_disableinteract_lore"));
        addSettingsItem(inventory, 14, XMaterial.ENDER_EYE, settings.isHidePlayers(), Messages.getString("settings_hideplayers_item"), Messages.getStringList("settings_hideplayers_lore"));
        addSettingsItem(inventory, 15, XMaterial.OAK_SIGN, settings.isInstantPlaceSigns(), Messages.getString("settings_instantplacesigns_item"), Messages.getStringList("settings_instantplacesigns_lore"));
        addSettingsItem(inventory, 20, XMaterial.SLIME_BLOCK, settings.isKeepNavigator(), Messages.getString("settings_keep_navigator_item"), Messages.getStringList("settings_keep_navigator_lore"));
        addSettingsItem(inventory, 21, configValues.getNavigatorItem(), settings.getNavigatorType().equals(NavigatorType.NEW), Messages.getString("settings_new_navigator_item"), Messages.getStringList("settings_new_navigator_lore"));
        addSettingsItem(inventory, 22, XMaterial.GOLDEN_CARROT, settings.isNightVision(), Messages.getString("settings_nightvision_item"), Messages.getStringList("settings_nightvision_lore"));
        addSettingsItem(inventory, 23, XMaterial.BRICKS, settings.isNoClip(), Messages.getString("settings_no_clip_item"), Messages.getStringList("settings_no_clip_lore"));
        addSettingsItem(inventory, 24, XMaterial.IRON_TRAPDOOR, settings.isTrapDoor(), Messages.getString("settings_open_trapdoors_item"), Messages.getStringList("settings_open_trapdoors_lore"));
        addSettingsItem(inventory, 29, XMaterial.FERN, settings.isPlacePlants(), Messages.getString("settings_placeplants_item"), Messages.getStringList("settings_placeplants_lore"));
        addSettingsItem(inventory, 30, XMaterial.PAPER, settings.isScoreboard(), configValues.isScoreboard() ? Messages.getString("settings_scoreboard_item") : Messages.getString("settings_scoreboard_disabled_item"),
                configValues.isScoreboard() ? Messages.getStringList("settings_scoreboard_lore") : Messages.getStringList("settings_scoreboard_disabled_lore"));
        addSettingsItem(inventory, 31, getSlabBreakingMaterial(), settings.isSlabBreaking(), Messages.getString("settings_slab_breaking_item"), Messages.getStringList("settings_slab_breaking_lore"));
        addSettingsItem(inventory, 32, XMaterial.MAGMA_CREAM, settings.isSpawnTeleport(), Messages.getString("settings_spawnteleport_item"), Messages.getStringList("settings_spawnteleport_lore"));

        return inventory;
    }

    private XMaterial getSlabBreakingMaterial() {
        return XMaterial.supports(13) ? XMaterial.SMOOTH_STONE_SLAB : XMaterial.STONE_SLAB;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            inventoryUtils.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void addSettingsItem(Inventory inventory, int position, XMaterial material, boolean enabled, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);

        if (enabled) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }

    private void addClearInventoryItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);
        XMaterial xMaterial = settings.isClearInventory() ? XMaterial.MINECART : XMaterial.CHEST_MINECART;
        addSettingsItem(inventory, 12, xMaterial, settings.isClearInventory(), Messages.getString("settings_clear_inventory_item"), Messages.getStringList("settings_clear_inventory_lore"));
    }

    private void addDesignItem(Inventory inventory, Player player) {
        ItemStack itemStack = inventoryUtils.getItemStack(inventoryUtils.getColouredGlass(plugin, player), Messages.getString("settings_change_design_item"));
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.setLore(Messages.getStringList("settings_change_design_lore"));
        itemStack.setItemMeta(itemMeta);
        itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);

        inventory.setItem(11, itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtils.checkIfValidClick(event, "settings_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);

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
                settings.setTrapDoor(!settings.isTrapDoor());
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
            default:
                return;
        }

        XSound.ENTITY_ITEM_PICKUP.play(player);
        plugin.getSettingsInventory().openInventory(player);
    }

    @SuppressWarnings("deprecation")
    private void toggleHidePlayers(Player player, Settings settings) {
        if (settings.isHidePlayers()) {
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        } else {
            Bukkit.getOnlinePlayers().forEach(player::showPlayer);
        }
    }
}