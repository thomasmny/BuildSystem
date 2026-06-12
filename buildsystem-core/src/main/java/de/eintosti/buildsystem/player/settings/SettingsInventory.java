/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.player.settings;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.navigator.settings.NavigatorType;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SettingsInventory extends Menu {

    private final BuildSystemPlugin plugin;
    private final SettingsService settingsManager;

    public SettingsInventory(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 45, plugin.getMessages().getString("settings_title", player));
        this.plugin = plugin;
        this.settingsManager = plugin.getSettingsService();
    }

    @Override
    protected void populate(Player player) {
        Inventory inv = getInventory();
        for (int i = 0; i <= 44; i++) {
            InventoryUtils.addGlassPane(player, inv, i);
        }

        Settings settings = settingsManager.getSettings(player);
        addDesignItem(inv, player);
        addClearInventoryItem(inv, player);
        addSettingsItem(player, inv, 13, XMaterial.DIAMOND_AXE, settings.isDisableInteract(), "settings_disableinteract_item", "settings_disableinteract_lore");
        addSettingsItem(player, inv, 14, XMaterial.ENDER_EYE, settings.isHidePlayers(), "settings_hideplayers_item", "settings_hideplayers_lore");
        addSettingsItem(player, inv, 15, XMaterial.OAK_SIGN, settings.isInstantPlaceSigns(), "settings_instantplacesigns_item", "settings_instantplacesigns_lore");
        addSettingsItem(player, inv, 20, XMaterial.SLIME_BLOCK, settings.isKeepNavigator(), "settings_keep_navigator_item", "settings_keep_navigator_lore");
        addSettingsItem(player, inv, 21, plugin.getConfigService().current().settings().navigator().item(), settings.getNavigatorType() == NavigatorType.NEW, "settings_new_navigator_item", "settings_new_navigator_lore");
        addSettingsItem(player, inv, 22, XMaterial.GOLDEN_CARROT, settings.isNightVision(), "settings_nightvision_item", "settings_nightvision_lore");
        addSettingsItem(player, inv, 23, XMaterial.BRICKS, settings.isNoClip(), "settings_no_clip_item", "settings_no_clip_lore");
        addSettingsItem(player, inv, 24, XMaterial.IRON_TRAPDOOR, settings.isOpenTrapDoors(), "settings_open_trapdoors_item", "settings_open_trapdoors_lore");
        addSettingsItem(player, inv, 29, XMaterial.FERN, settings.isPlacePlants(), "settings_placeplants_item", "settings_placeplants_lore");
        addSettingsItem(player, inv, 30, XMaterial.PAPER, settings.isScoreboard(),
                plugin.getConfigService().current().settings().scoreboard() ? "settings_scoreboard_item" : "settings_scoreboard_disabled_item",
                plugin.getConfigService().current().settings().scoreboard() ? "settings_scoreboard_lore" : "settings_scoreboard_disabled_lore");
        addSettingsItem(player, inv, 31, XMaterial.SMOOTH_STONE_SLAB, settings.isSlabBreaking(), "settings_slab_breaking_item", "settings_slab_breaking_lore");
        addSettingsItem(player, inv, 32, XMaterial.MAGMA_CREAM, settings.isSpawnTeleport(), "settings_spawnteleport_item", "settings_spawnteleport_lore");
    }

    private void addSettingsItem(Player player, Inventory inventory, int position, XMaterial material, boolean enabled, String displayNameKey, String loreKey) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName(messages.getString(displayNameKey, player));
        itemMeta.setLore(messages.getStringList(loreKey, player));
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);

        if (enabled) {
            itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);
        }

        inventory.setItem(position, itemStack);
    }

    private void addClearInventoryItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);
        XMaterial xMaterial = settings.isClearInventory() ? XMaterial.MINECART : XMaterial.CHEST_MINECART;
        addSettingsItem(player, inventory, 12, xMaterial, settings.isClearInventory(),
                "settings_clear_inventory_item", "settings_clear_inventory_lore"
        );
    }

    private void addDesignItem(Inventory inventory, Player player) {
        DesignColor color = settingsManager.getSettings(player).getDesignColor();
        XMaterial material = XMaterial.matchXMaterial(color.name() + "_STAINED_GLASS").orElse(XMaterial.BLACK_STAINED_GLASS);
        ItemStack itemStack = InventoryUtils.createItem(material, messages.getString("settings_change_design_item", player));
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.setLore(messages.getStringList("settings_change_design_lore", player));
        itemStack.setItemMeta(itemMeta);
        itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);

        inventory.setItem(11, itemStack);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);

        switch (event.getSlot()) {
            case 11:
                new DesignInventory(plugin, player).open(player);
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
                    plugin.getNavigatorService().removeArmorStands(player);
                    player.removePotionEffect(XPotion.BLINDNESS.get());
                }
                break;
            case 22:
                if (!settings.isNightVision()) {
                    settings.setNightVision(true);
                    player.addPotionEffect(new PotionEffect(XPotion.NIGHT_VISION.get(), PotionEffect.INFINITE_DURATION, 0, false, false));
                } else {
                    settings.setNightVision(false);
                    player.removePotionEffect(XPotion.NIGHT_VISION.get());
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
                settings.setOpenTrapDoors(!settings.isOpenTrapDoors());
                break;

            case 29:
                settings.setPlacePlants(!settings.isPlacePlants());
                break;
            case 30:
                if (!plugin.getConfigService().current().settings().scoreboard()) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                if (settings.isScoreboard()) {
                    settings.setScoreboard(false);
                    settingsManager.hideScoreboard(player);
                } else {
                    settings.setScoreboard(true);
                    settingsManager.displayScoreboard(player);
                    plugin.getSettingsService().forceUpdateSidebar(player);
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
        new SettingsInventory(plugin, player).open(player);
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
