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
package de.eintosti.buildsystem.player.menu;

import static java.util.Map.entry;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.menu.InventoryUtils;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.Map;
import java.util.function.BooleanSupplier;
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
public class SettingsMenu extends Menu {

    private static final int DESIGN_SLOT = 11;

    private final BuildSystemPlugin plugin;
    private final SettingsService settingsManager;

    public SettingsMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 45, plugin.getMessages().getString("settings_title", player));
        this.plugin = plugin;
        this.settingsManager = plugin.getSettingsService();
    }

    /**
     * A boolean setting rendered as a glowing-when-enabled item. {@code onToggle} performs the state change (including
     * any side effects) and returns {@code false} when the click is rejected, in which case the menu is not reopened.
     */
    private record Toggle(
            XMaterial material, boolean enabled, String itemKey, String loreKey, BooleanSupplier onToggle) {

        static Toggle of(XMaterial material, boolean enabled, String itemKey, String loreKey, Runnable flip) {
            return new Toggle(material, enabled, itemKey, loreKey, () -> {
                flip.run();
                return true;
            });
        }
    }

    private Map<Integer, Toggle> toggles(Player player, Settings settings) {
        boolean scoreboardEnabled =
                plugin.getConfigService().current().settings().scoreboard();
        return Map.ofEntries(
                entry(
                        12,
                        Toggle.of(
                                settings.isClearInventory() ? XMaterial.MINECART : XMaterial.CHEST_MINECART,
                                settings.isClearInventory(),
                                "settings_clear_inventory_item",
                                "settings_clear_inventory_lore",
                                () -> settings.setClearInventory(!settings.isClearInventory()))),
                entry(
                        13,
                        Toggle.of(
                                XMaterial.DIAMOND_AXE,
                                settings.isDisableInteract(),
                                "settings_disableinteract_item",
                                "settings_disableinteract_lore",
                                () -> settings.setDisableInteract(!settings.isDisableInteract()))),
                entry(
                        14,
                        Toggle.of(
                                XMaterial.ENDER_EYE,
                                settings.isHidePlayers(),
                                "settings_hideplayers_item",
                                "settings_hideplayers_lore",
                                () -> {
                                    settings.setHidePlayers(!settings.isHidePlayers());
                                    toggleHidePlayers(player, settings);
                                })),
                entry(
                        15,
                        Toggle.of(
                                XMaterial.OAK_SIGN,
                                settings.isInstantPlaceSigns(),
                                "settings_instantplacesigns_item",
                                "settings_instantplacesigns_lore",
                                () -> settings.setInstantPlaceSigns(!settings.isInstantPlaceSigns()))),
                entry(
                        20,
                        Toggle.of(
                                XMaterial.SLIME_BLOCK,
                                settings.isKeepNavigator(),
                                "settings_keep_navigator_item",
                                "settings_keep_navigator_lore",
                                () -> settings.setKeepNavigator(!settings.isKeepNavigator()))),
                entry(
                        21,
                        Toggle.of(
                                plugin.getConfigService()
                                        .current()
                                        .settings()
                                        .navigator()
                                        .item(),
                                settings.getNavigatorType() == NavigatorType.NEW,
                                "settings_new_navigator_item",
                                "settings_new_navigator_lore",
                                () -> toggleNavigatorType(player, settings))),
                entry(
                        22,
                        Toggle.of(
                                XMaterial.GOLDEN_CARROT,
                                settings.isNightVision(),
                                "settings_nightvision_item",
                                "settings_nightvision_lore",
                                () -> toggleNightVision(player, settings))),
                entry(
                        23,
                        Toggle.of(
                                XMaterial.BRICKS,
                                settings.isNoClip(),
                                "settings_no_clip_item",
                                "settings_no_clip_lore",
                                () -> toggleNoClip(player, settings))),
                entry(
                        24,
                        Toggle.of(
                                XMaterial.IRON_TRAPDOOR,
                                settings.isOpenTrapDoors(),
                                "settings_open_trapdoors_item",
                                "settings_open_trapdoors_lore",
                                () -> settings.setOpenTrapDoors(!settings.isOpenTrapDoors()))),
                entry(
                        29,
                        Toggle.of(
                                XMaterial.FERN,
                                settings.isPlacePlants(),
                                "settings_placeplants_item",
                                "settings_placeplants_lore",
                                () -> settings.setPlacePlants(!settings.isPlacePlants()))),
                entry(
                        30,
                        new Toggle(
                                XMaterial.PAPER,
                                settings.isScoreboard(),
                                scoreboardEnabled ? "settings_scoreboard_item" : "settings_scoreboard_disabled_item",
                                scoreboardEnabled ? "settings_scoreboard_lore" : "settings_scoreboard_disabled_lore",
                                () -> toggleScoreboard(player, settings, scoreboardEnabled))),
                entry(
                        31,
                        Toggle.of(
                                XMaterial.SMOOTH_STONE_SLAB,
                                settings.isSlabBreaking(),
                                "settings_slab_breaking_item",
                                "settings_slab_breaking_lore",
                                () -> settings.setSlabBreaking(!settings.isSlabBreaking()))),
                entry(
                        32,
                        Toggle.of(
                                XMaterial.MAGMA_CREAM,
                                settings.isSpawnTeleport(),
                                "settings_spawnteleport_item",
                                "settings_spawnteleport_lore",
                                () -> settings.setSpawnTeleport(!settings.isSpawnTeleport()))));
    }

    @Override
    protected void populate(Player player) {
        Inventory inv = getInventory();
        plugin.getMenuItems().fillRange(player, inv, 0, 45);
        addDesignItem(inv, player);

        Settings settings = settingsManager.getSettings(player);
        toggles(player, settings)
                .forEach((slot, toggle) -> plugin.getMenuItems()
                        .addToggleItem(
                                player,
                                inv,
                                slot,
                                toggle.material(),
                                toggle.enabled(),
                                toggle.itemKey(),
                                toggle.loreKey()));
    }

    private void addDesignItem(Inventory inventory, Player player) {
        DesignColor color = settingsManager.getSettings(player).getDesignColor();
        XMaterial material =
                XMaterial.matchXMaterial(color.name() + "_STAINED_GLASS").orElse(XMaterial.BLACK_STAINED_GLASS);
        ItemStack itemStack =
                InventoryUtils.createItem(material, messages.getString("settings_change_design_item", player));
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.setLore(messages.getStringList("settings_change_design_lore", player));
        itemStack.setItemMeta(itemMeta);
        itemStack.addUnsafeEnchantment(XEnchantment.UNBREAKING.get(), 1);

        inventory.setItem(DESIGN_SLOT, itemStack);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getSlot() == DESIGN_SLOT) {
            new DesignMenu(plugin, player).open(player);
            XSound.ENTITY_ITEM_PICKUP.play(player);
            return;
        }

        Settings settings = settingsManager.getSettings(player);
        Toggle toggle = toggles(player, settings).get(event.getSlot());
        if (toggle == null) {
            return;
        }

        if (!toggle.onToggle().getAsBoolean()) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        XSound.ENTITY_ITEM_PICKUP.play(player);
        new SettingsMenu(plugin, player).open(player);
    }

    private void toggleNavigatorType(Player player, Settings settings) {
        if (settings.getNavigatorType() == NavigatorType.OLD) {
            settings.setNavigatorType(NavigatorType.NEW);
        } else {
            settings.setNavigatorType(NavigatorType.OLD);
            plugin.getNavigatorService().removeArmorStands(player);
            player.removePotionEffect(XPotion.BLINDNESS.get());
        }
    }

    private void toggleNightVision(Player player, Settings settings) {
        if (settings.isNightVision()) {
            settings.setNightVision(false);
            player.removePotionEffect(XPotion.NIGHT_VISION.get());
        } else {
            settings.setNightVision(true);
            player.addPotionEffect(
                    new PotionEffect(XPotion.NIGHT_VISION.get(), PotionEffect.INFINITE_DURATION, 0, false, false));
        }
    }

    private void toggleNoClip(Player player, Settings settings) {
        NoClipService noClipService = plugin.getNoClipService();
        if (settings.isNoClip()) {
            settings.setNoClip(false);
            noClipService.stopNoClip(player.getUniqueId());
        } else {
            settings.setNoClip(true);
            noClipService.startNoClip(player);
        }
    }

    private boolean toggleScoreboard(Player player, Settings settings, boolean scoreboardEnabled) {
        if (!scoreboardEnabled) {
            return false;
        }

        if (settings.isScoreboard()) {
            settings.setScoreboard(false);
            settingsManager.hideScoreboard(player);
        } else {
            settings.setScoreboard(true);
            settingsManager.displayScoreboard(player);
            settingsManager.forceUpdateSidebar(player);
        }
        return true;
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
