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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.menu.MenuButton;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SettingsMenu extends Menu {

    private static final int DESIGN_SLOT = 11;
    private static final String PERMISSION_PREFIX = "buildsystem.setting.";

    /**
     * Classifies a settings slot's click behavior, so the per-slot contract can be asserted as data. {@code TOGGLE}
     * always accepts; {@code REJECTABLE} (the scoreboard slot) may reject when disabled in config; {@code SUBMENU} (the
     * design slot) opens another menu.
     */
    enum ClickOutcome {
        TOGGLE,
        REJECTABLE,
        SUBMENU
    }

    /**
     * A single settings slot: its permission {@code node} suffix (or {@code null} for the design slot, which has no
     * permission), its {@link ClickOutcome} classification, and its render/click behavior. Replaces the old private
     * {@code Toggle} record and the hand-rolled dispatch.
     */
    private record SettingsButton(
            @Nullable String node,
            ClickOutcome outcome,
            BiConsumer<Player, Inventory> renderer,
            BiConsumer<Player, InventoryClickEvent> clickHandler)
            implements MenuButton {

        @Override
        public void render(Player player, Inventory inventory) {
            renderer.accept(player, inventory);
        }

        @Override
        public void onClick(Player player, InventoryClickEvent event) {
            clickHandler.accept(player, event);
        }
    }

    private final BuildSystemPlugin plugin;
    private final SettingsService settingsManager;
    private final Map<Integer, SettingsButton> buttons;

    public SettingsMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), 45, plugin.getMessages().getString("settings_title", player));
        this.plugin = plugin;
        this.settingsManager = plugin.getSettingsService();
        this.buttons = buildButtons();
    }

    // protected: only for unit tests that cannot run a Bukkit server
    SettingsMenu(BuildSystemPlugin plugin, SettingsService settingsManager, Messages messages, Inventory inventory) {
        super(messages, inventory);
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.buttons = buildButtons();
    }

    private Map<Integer, SettingsButton> buildButtons() {
        Map<Integer, SettingsButton> map = new LinkedHashMap<>();

        map.put(DESIGN_SLOT, new SettingsButton(null, ClickOutcome.SUBMENU, this::renderDesign, (player, event) -> {
            new DesignMenu(plugin, player).open(player);
            XSound.ENTITY_ITEM_PICKUP.play(player);
        }));

        map.put(
                12,
                toggleButton(
                        12,
                        "clear-inventory",
                        "settings_clear_inventory_item",
                        "settings_clear_inventory_lore",
                        s -> s.isClearInventory() ? XMaterial.MINECART : XMaterial.CHEST_MINECART,
                        Settings::isClearInventory,
                        (player, s) -> s.setClearInventory(!s.isClearInventory())));
        map.put(
                13,
                toggleButton(
                        13,
                        "disable-interact",
                        "settings_disableinteract_item",
                        "settings_disableinteract_lore",
                        XMaterial.DIAMOND_AXE,
                        Settings::isDisableInteract,
                        (player, s) -> s.setDisableInteract(!s.isDisableInteract())));
        map.put(
                14,
                toggleButton(
                        14,
                        "hide-players",
                        "settings_hideplayers_item",
                        "settings_hideplayers_lore",
                        XMaterial.ENDER_EYE,
                        Settings::isHidePlayers,
                        (player, s) -> {
                            s.setHidePlayers(!s.isHidePlayers());
                            toggleHidePlayers(player, s);
                        }));
        map.put(
                15,
                toggleButton(
                        15,
                        "instant-place-signs",
                        "settings_instantplacesigns_item",
                        "settings_instantplacesigns_lore",
                        XMaterial.OAK_SIGN,
                        Settings::isInstantPlaceSigns,
                        (player, s) -> s.setInstantPlaceSigns(!s.isInstantPlaceSigns())));
        map.put(
                20,
                toggleButton(
                        20,
                        "keep-navigator",
                        "settings_keep_navigator_item",
                        "settings_keep_navigator_lore",
                        XMaterial.SLIME_BLOCK,
                        Settings::isKeepNavigator,
                        (player, s) -> s.setKeepNavigator(!s.isKeepNavigator())));
        map.put(
                21,
                toggleButton(
                        21,
                        "navigator-type",
                        "settings_new_navigator_item",
                        "settings_new_navigator_lore",
                        s -> plugin.getConfigService()
                                .current()
                                .settings()
                                .navigator()
                                .item(),
                        s -> s.getNavigatorType() == NavigatorType.NEW,
                        this::toggleNavigatorType));
        map.put(
                22,
                toggleButton(
                        22,
                        "night-vision",
                        "settings_nightvision_item",
                        "settings_nightvision_lore",
                        XMaterial.GOLDEN_CARROT,
                        Settings::isNightVision,
                        this::toggleNightVision));
        map.put(
                23,
                toggleButton(
                        23,
                        "no-clip",
                        "settings_no_clip_item",
                        "settings_no_clip_lore",
                        XMaterial.BRICKS,
                        Settings::isNoClip,
                        this::toggleNoClip));
        map.put(
                24,
                toggleButton(
                        24,
                        "open-trapdoors",
                        "settings_open_trapdoors_item",
                        "settings_open_trapdoors_lore",
                        XMaterial.IRON_TRAPDOOR,
                        Settings::isOpenTrapDoors,
                        (player, s) -> s.setOpenTrapDoors(!s.isOpenTrapDoors())));
        map.put(
                29,
                toggleButton(
                        29,
                        "place-plants",
                        "settings_placeplants_item",
                        "settings_placeplants_lore",
                        XMaterial.FERN,
                        Settings::isPlacePlants,
                        (player, s) -> s.setPlacePlants(!s.isPlacePlants())));
        map.put(30, scoreboardButton());
        map.put(
                31,
                toggleButton(
                        31,
                        "slab-breaking",
                        "settings_slab_breaking_item",
                        "settings_slab_breaking_lore",
                        XMaterial.SMOOTH_STONE_SLAB,
                        Settings::isSlabBreaking,
                        (player, s) -> s.setSlabBreaking(!s.isSlabBreaking())));
        map.put(
                32,
                toggleButton(
                        32,
                        "spawn-teleport",
                        "settings_spawnteleport_item",
                        "settings_spawnteleport_lore",
                        XMaterial.MAGMA_CREAM,
                        Settings::isSpawnTeleport,
                        (player, s) -> s.setSpawnTeleport(!s.isSpawnTeleport())));

        return map;
    }

    private SettingsButton toggleButton(
            int slot,
            String node,
            String itemKey,
            String loreKey,
            XMaterial material,
            Predicate<Settings> enabled,
            BiConsumer<Player, Settings> flip) {
        return toggleButton(slot, node, itemKey, loreKey, s -> material, enabled, flip);
    }

    /**
     * Builds a toggle slot. The icon/state are read from live {@link Settings} at render time, and the flip runs against
     * live settings at click time — equivalent to the previous per-click snapshot.
     */
    private SettingsButton toggleButton(
            int slot,
            String node,
            String itemKey,
            String loreKey,
            Function<Settings, XMaterial> material,
            Predicate<Settings> enabled,
            BiConsumer<Player, Settings> flip) {
        return new SettingsButton(
                node,
                ClickOutcome.TOGGLE,
                (player, inventory) -> {
                    Settings settings = settingsManager.getSettings(player);
                    plugin.getMenuItems()
                            .addToggleItem(
                                    player,
                                    inventory,
                                    slot,
                                    material.apply(settings),
                                    enabled.test(settings),
                                    itemKey,
                                    loreKey);
                },
                (player, event) -> handleToggle(player, node, () -> {
                    flip.accept(player, settingsManager.getSettings(player));
                    return true;
                }));
    }

    private SettingsButton scoreboardButton() {
        return new SettingsButton(
                "scoreboard",
                ClickOutcome.REJECTABLE,
                (player, inventory) -> {
                    boolean scoreboardEnabled =
                            plugin.getConfigService().current().settings().scoreboard();
                    Settings settings = settingsManager.getSettings(player);
                    plugin.getMenuItems()
                            .addToggleItem(
                                    player,
                                    inventory,
                                    30,
                                    XMaterial.PAPER,
                                    settings.isScoreboard(),
                                    scoreboardEnabled
                                            ? "settings_scoreboard_item"
                                            : "settings_scoreboard_disabled_item",
                                    scoreboardEnabled
                                            ? "settings_scoreboard_lore"
                                            : "settings_scoreboard_disabled_lore");
                },
                (player, event) -> {
                    boolean scoreboardEnabled =
                            plugin.getConfigService().current().settings().scoreboard();
                    handleToggle(
                            player,
                            "scoreboard",
                            () -> toggleScoreboard(player, settingsManager.getSettings(player), scoreboardEnabled));
                });
    }

    /**
     * The shared toggle click sequence. Note the two intentional differences from {@code Menu.requirePermission}: a
     * denied permission does <strong>not</strong> close the inventory, and a rejected toggle (scoreboard disabled in
     * config) plays the break sound without re-opening.
     */
    private void handleToggle(Player player, String node, BooleanSupplier onToggle) {
        if (!player.hasPermission(PERMISSION_PREFIX + node)) {
            messages.sendPermissionError(player);
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        if (!onToggle.getAsBoolean()) {
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        XSound.ENTITY_ITEM_PICKUP.play(player);
        new SettingsMenu(plugin, player).open(player);
    }

    @Override
    protected void populate(Player player) {
        Inventory inv = getInventory();
        plugin.getMenuItems().fillRange(player, inv, 0, 45);
        buttons.forEach((slot, button) -> button.render(player, inv));
    }

    private void renderDesign(Player player, Inventory inventory) {
        DesignColor color = settingsManager.getSettings(player).getDesignColor();
        XMaterial material =
                XMaterial.matchXMaterial(color.name() + "_STAINED_GLASS").orElse(XMaterial.BLACK_STAINED_GLASS);

        ItemBuilder.of(material)
                .name(messages.getString("settings_change_design_item", player))
                .lore(messages.getStringList("settings_change_design_lore", player))
                .glow(true)
                .into(inventory, DESIGN_SLOT);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        MenuButton button = buttons.get(event.getSlot());
        if (button != null) {
            button.onClick(player, event);
        }
    }

    /**
     * The slot &rarr; full permission node mapping, derived from the button registry. The design slot (no permission)
     * is omitted. Exposed for the golden test that pins the per-slot contract.
     */
    Map<Integer, String> permissionNodeBySlot() {
        Map<Integer, String> nodes = new LinkedHashMap<>();
        buttons.forEach((slot, button) -> {
            String node = button.node();
            if (node != null) {
                nodes.put(slot, PERMISSION_PREFIX + node);
            }
        });
        return nodes;
    }

    /**
     * The slot &rarr; {@link ClickOutcome} classification, derived from the button registry. Exposed for the golden
     * test that pins the per-slot contract.
     */
    Map<Integer, ClickOutcome> outcomeBySlot() {
        Map<Integer, ClickOutcome> outcomes = new LinkedHashMap<>();
        buttons.forEach((slot, button) -> outcomes.put(slot, button.outcome()));
        return outcomes;
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
