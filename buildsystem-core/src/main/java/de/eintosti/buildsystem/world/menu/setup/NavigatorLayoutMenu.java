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
package de.eintosti.buildsystem.world.menu.setup;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import de.eintosti.buildsystem.world.menu.SetupMenu;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The combined navigator setup: it both manages categories and arranges their navigator layout in one screen. The top
 * inventory is a live, exact preview of the inventory navigator — each shown category at its slot plus the settings
 * button at its slot. The player's own inventory is taken over (snapshotted and cleared by
 * {@link de.eintosti.buildsystem.navigator.NavigatorEditorService}, restored on close/quit/shutdown) to present a
 * back button, a create-category button, and the palette of categories not yet in the navigator.
 */
@NullMarked
public class NavigatorLayoutMenu extends Menu {

    private static final int NAVIGATOR_SIZE = 27;

    private static final int BACK_SLOT = 0;
    private static final int CREATE_SLOT = 4;
    private static final int RESET_SLOT = 7;
    private static final int DELETE_SLOT = 8;
    private static final int PALETTE_FIRST_SLOT = 9;
    private static final int PALETTE_LAST_SLOT = 35;

    private final BuildSystemPlugin plugin;
    private final NavigatorCategoryRegistryImpl registry;
    private final ActiveCursorState cursorState = new ActiveCursorState();

    public NavigatorLayoutMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), NAVIGATOR_SIZE, plugin.getMessages().getString("setup_navigator_title", player));
        this.plugin = plugin;
        this.registry = plugin.getNavigatorCategoryRegistry();
    }

    @Override
    public void open(Player player) {
        plugin.getNavigatorEditorService().beginSession(player);
        populate(player);
        player.openInventory(getInventory());
        renderControls(player);
    }

    @Override
    protected void populate(Player player) {
        Inventory inventory = getInventory();

        // Empty navigator slots default to structural filler design panes
        for (int slot = 0; slot < NAVIGATOR_SIZE; slot++) {
            plugin.getMenuItems().addGlassPane(player, inventory, slot);
        }

        int settingsSlot = registry.getSettingsSlot();
        if (!cursorState.isHoldingSettings() && isSlotValid(settingsSlot)) {
            ItemBuilder.skull(Profileable.detect(SkullTextures.SETTINGS))
                    .name(messages.getString("old_navigator_settings", player))
                    .lore(messages.getStringList("setup_navigator_settings_placed_lore", player))
                    .into(inventory, settingsSlot);
        }

        for (NavigatorCategory category : registry.getCategories()) {
            int slot = category.getNavigatorSlot();
            if (!category.isShownInNavigator() || !isSlotValid(slot) || slot == settingsSlot) {
                continue;
            }
            if (category.getId().equals(cursorState.getHeldCategoryId())) {
                continue;
            }
            ItemBuilder.icon(category, player)
                    .name(ColorAPI.process(category.getColor() + category.getDisplayName()))
                    .lore(messages.getStringList("setup_navigator_placed_lore", player))
                    .into(inventory, slot);
        }
    }

    private void renderControls(Player player) {
        Inventory playerInventory = player.getInventory();
        playerInventory.clear();

        ItemBuilder.of(XMaterial.BARRIER)
                .name(messages.getString("setup_back", player))
                .into(playerInventory, BACK_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                .name(messages.getString("setup_category_add", player))
                .lore(messages.getStringList("setup_navigator_create_lore", player))
                .into(playerInventory, CREATE_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.RESET))
                .name(messages.getString("setup_navigator_reset", player))
                .lore(messages.getStringList("setup_navigator_reset_lore", player))
                .into(playerInventory, RESET_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.DELETE))
                .name(messages.getString("setup_navigator_delete", player))
                .lore(messages.getStringList("setup_navigator_delete_lore", player))
                .into(playerInventory, DELETE_SLOT);

        List<NavigatorCategory> notAdded = notAddedCategories();
        for (int i = 0; i < notAdded.size() && PALETTE_FIRST_SLOT + i <= PALETTE_LAST_SLOT; i++) {
            NavigatorCategory category = notAdded.get(i);
            int slot = PALETTE_FIRST_SLOT + i;

            if (category.getId().equals(cursorState.getHeldCategoryId())) {
                continue;
            }
            ItemBuilder.icon(category, player)
                    .name(ColorAPI.process(category.getColor() + category.getDisplayName()))
                    .lore(messages.getStringList("setup_navigator_palette_lore", player))
                    .into(playerInventory, slot);
        }

        int settingsTokenSlot = settingsPaletteSlot();
        if (!cursorState.isHoldingSettings() && registry.getSettingsSlot() < 0 && settingsTokenSlot >= 0) {
            ItemBuilder.skull(Profileable.detect(SkullTextures.SETTINGS))
                    .name(messages.getString("old_navigator_settings", player))
                    .lore(messages.getStringList("setup_navigator_settings_palette_lore", player))
                    .into(playerInventory, settingsTokenSlot);
        }
    }

    private int settingsPaletteSlot() {
        int slot = PALETTE_FIRST_SLOT + notAddedCategories().size();
        return slot <= PALETTE_LAST_SLOT ? slot : -1;
    }

    private List<NavigatorCategory> notAddedCategories() {
        return registry.getCategories().stream()
                .filter(category -> !category.isShownInNavigator())
                .toList();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == getInventory()) {
            handleNavigatorClick(player, event.getRawSlot(), event.isShiftClick());
        } else if (event.getClickedInventory() == player.getInventory()) {
            handlePlayerClick(player, event.getSlot(), event.isRightClick(), event.isShiftClick());
        } else {
            handleOutsideClickWhileHolding(player);
        }
    }

    private void handleNavigatorClick(Player player, int slot, boolean shiftClick) {
        if (!isSlotValid(slot)) {
            return;
        }

        if (cursorState.isHoldingSettings()) {
            placeSettings(player, slot);
            return;
        }
        if (cursorState.isHoldingCategory()) {
            placeHeldCategory(player, slot);
            return;
        }

        if (slot == registry.getSettingsSlot()) {
            pickUpSettings(player);
            return;
        }

        NavigatorCategoryImpl occupant = categoryAtSlot(slot);
        if (occupant != null) {
            if (shiftClick) {
                new CategoryEditorMenu(plugin, player, occupant).open(player);
            } else {
                pickUpCategory(player, occupant.getId(), slot);
            }
        }
    }

    private void placeHeldCategory(Player player, int slot) {
        NavigatorCategoryImpl held = (NavigatorCategoryImpl)
                registry.getCategory(cursorState.getHeldCategoryId()).orElse(null);
        if (held == null) {
            clearHeld(player);
            return;
        }

        if (slot == registry.getSettingsSlot()) {
            registry.setSettingsSlot(
                    cursorState.getHeldFromSlot() >= 0 ? cursorState.getHeldFromSlot() : firstFreeSlot());
        } else {
            NavigatorCategoryImpl occupant = categoryAtSlot(slot);
            if (occupant != null && !occupant.equals(held)) {
                if (cursorState.getHeldFromSlot() >= 0) {
                    occupant.setNavigatorSlot(cursorState.getHeldFromSlot());
                } else {
                    occupant.setShownInNavigator(false);
                }
                registry.persist(occupant);
            }
        }

        held.setNavigatorSlot(slot);
        held.setShownInNavigator(true);
        registry.persist(held);
        clearHeld(player);
        refresh(player);
    }

    private void placeSettings(Player player, int slot) {
        NavigatorCategoryImpl occupant = categoryAtSlot(slot);
        if (occupant != null) {
            occupant.setNavigatorSlot(registry.getSettingsSlot());
            registry.persist(occupant);
        }
        registry.setSettingsSlot(slot);
        clearHeld(player);
        refresh(player);
    }

    private void handlePlayerClick(Player player, int slot, boolean rightClick, boolean shiftClick) {
        if (cursorState.isHoldingCategory() && slot == DELETE_SLOT) {
            deleteHeldCategory(player);
            return;
        }
        if (cursorState.isHoldingCategory() || cursorState.isHoldingSettings()) {
            handleOutsideClickWhileHolding(player);
            return;
        }

        if (slot == BACK_SLOT) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            player.closeInventory();
            new SetupMenu(plugin, player).open(player);
            return;
        }
        if (slot == CREATE_SLOT) {
            beginCategoryCreation(player);
            return;
        }
        if (slot == RESET_SLOT) {
            // Left-click resets only the layout; right-click resets every category to the built-in defaults.
            promptReset(player, rightClick);
            return;
        }
        if (registry.getSettingsSlot() < 0 && slot == settingsPaletteSlot()) {
            pickUpSettings(player);
            return;
        }

        int paletteIndex = slot - PALETTE_FIRST_SLOT;
        List<NavigatorCategory> notAdded = notAddedCategories();
        if (paletteIndex < 0 || paletteIndex >= notAdded.size() || slot > PALETTE_LAST_SLOT) {
            return;
        }

        NavigatorCategory clicked = notAdded.get(paletteIndex);
        if (shiftClick) {
            new CategoryEditorMenu(plugin, player, clicked).open(player);
        } else {
            pickUpCategory(player, clicked.getId(), -1);
        }
    }

    private void deleteHeldCategory(Player player) {
        if (cursorState.isHoldingCategory() && registry.deleteCategory(cursorState.getHeldCategoryId())) {
            XSound.ENTITY_ITEM_BREAK.play(player);
        }
        clearHeld(player);
        refresh(player);
    }

    private void handleOutsideClickWhileHolding(Player player) {
        if (cursorState.isHoldingSettings()) {
            registry.setSettingsSlot(-1);
        } else if (cursorState.isHoldingCategory()) {
            NavigatorCategoryImpl held = (NavigatorCategoryImpl)
                    registry.getCategory(cursorState.getHeldCategoryId()).orElse(null);
            if (held != null) {
                held.setShownInNavigator(false);
                registry.persist(held);
            }
        }
        clearHeld(player);
        refresh(player);
    }

    private void promptReset(Player player, boolean resetEverything) {
        String confirmLoreKey =
                resetEverything ? "setup_navigator_reset_all_confirm_lore" : "setup_navigator_reset_confirm_lore";
        new DeletionConfirmMenu(
                        plugin,
                        player,
                        messages.getString("setup_navigator_reset", player),
                        messages.getStringList(confirmLoreKey, player),
                        () -> {
                            if (resetEverything) {
                                registry.resetToDefaults();
                            } else {
                                registry.resetNavigatorLayout();
                            }
                            XSound.ENTITY_CHICKEN_EGG.play(player);
                            new NavigatorLayoutMenu(plugin, player).open(player);
                        },
                        () -> new NavigatorLayoutMenu(plugin, player).open(player))
                .open(player);
    }

    private void beginCategoryCreation(Player player) {
        PlayerChatInput.requestSanitizedName(
                plugin,
                player,
                "setup_category_add_prompt",
                "setup_name_invalid_characters",
                "setup_name_empty",
                name -> {
                    NavigatorCategoryImpl created = registry.createCategory(name);
                    created.setShownInNavigator(true);
                    created.setNavigatorSlot(firstFreeSlot());
                    registry.persist(created);
                    new CategoryEditorMenu(plugin, player, created).open(player);
                },
                () -> new NavigatorLayoutMenu(plugin, player).open(player));
    }

    private void pickUpCategory(Player player, String categoryId, int fromSlot) {
        cursorState.trackCategory(categoryId, fromSlot);
        NavigatorCategory category = registry.getCategory(categoryId).orElse(null);
        ItemStack cursor = category == null
                ? null
                : ItemBuilder.icon(category, player)
                        .name(ColorAPI.process(category.getColor() + category.getDisplayName()))
                        .build();

        setCursorNextTick(player, cursor);
        XSound.ITEM_ARMOR_EQUIP_LEATHER.play(player);
        refresh(player);
    }

    private void pickUpSettings(Player player) {
        cursorState.trackSettings();
        setCursorNextTick(
                player,
                ItemBuilder.skull(Profileable.detect(SkullTextures.SETTINGS))
                        .name(messages.getString("old_navigator_settings", player))
                        .build());

        XSound.ITEM_ARMOR_EQUIP_LEATHER.play(player);
        refresh(player);
    }

    private void clearHeld(Player player) {
        cursorState.reset();
        setCursorNextTick(player, null);
    }

    private void setCursorNextTick(Player player, @Nullable ItemStack cursor) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getNavigatorEditorService().hasSession(player)) {
                player.setItemOnCursor(cursor);
            }
        });
    }

    private void refresh(Player player) {
        populate(player);
        renderControls(player);
    }

    private @Nullable NavigatorCategoryImpl categoryAtSlot(int slot) {
        return registry.getCategories().stream()
                .filter(category -> category.isShownInNavigator() && category.getNavigatorSlot() == slot)
                .map(category -> (NavigatorCategoryImpl) category)
                .findFirst()
                .orElse(null);
    }

    private int firstFreeSlot() {
        for (int slot = 0; slot < NAVIGATOR_SIZE; slot++) {
            if (categoryAtSlot(slot) == null) {
                return slot;
            }
        }
        return 0;
    }

    private static boolean isSlotValid(int slot) {
        return slot >= 0 && slot < NAVIGATOR_SIZE;
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        plugin.getNavigatorEditorService().restore((Player) event.getPlayer());
    }

    /**
     * Inner structured manager wrapping pick-and-place tracking metrics.
     */
    private static final class ActiveCursorState {
        private @Nullable String heldCategoryId;
        private boolean heldSettings;
        private int heldFromSlot = -1;

        public void trackCategory(String categoryId, int fromSlot) {
            this.heldCategoryId = categoryId;
            this.heldSettings = false;
            this.heldFromSlot = fromSlot;
        }

        public void trackSettings() {
            this.heldSettings = true;
            this.heldCategoryId = null;
            this.heldFromSlot = -1;
        }

        public void reset() {
            this.heldCategoryId = null;
            this.heldSettings = false;
            this.heldFromSlot = -1;
        }

        public @Nullable String getHeldCategoryId() {
            return heldCategoryId;
        }

        public boolean isHoldingSettings() {
            return heldSettings;
        }

        public boolean isHoldingCategory() {
            return heldCategoryId != null;
        }

        public int getHeldFromSlot() {
            return heldFromSlot;
        }
    }
}
