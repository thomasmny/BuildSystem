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

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.Registry;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The navigator setup: the preview is a live copy of the inventory navigator, and categories are dragged into it from
 * the palette. The drag-and-drop itself lives in {@link LayoutEditorMenu}.
 *
 * <p>Unlike the status editor, this screen also arranges the settings button, which is not a registry entry. It
 * occupies a navigator slot, can be picked up and placed like a category, and returns to the palette when dropped
 * outside — which is what the {@code ...Extra...} and {@link #displaceReserved} hooks are for.
 */
@NullMarked
public class NavigatorLayoutMenu extends LayoutEditorMenu<NavigatorCategory> {

    private static final int NAVIGATOR_SIZE = 27;

    private static final LayoutMessages MESSAGES = new LayoutMessages(
            "setup_category_add",
            "setup_navigator_create_lore",
            "setup_navigator_reset",
            "setup_navigator_reset_lore",
            "setup_navigator_reset_confirm_lore",
            "setup_navigator_reset_all_confirm_lore",
            "setup_navigator_delete",
            "setup_navigator_delete_lore",
            "setup_navigator_placed_lore",
            "setup_navigator_palette_lore",
            "setup_category_add_prompt");

    private final NavigatorCategoryRegistryImpl registry;
    private final SettingsButton settingsButton = new SettingsButton();

    public NavigatorLayoutMenu(
            Messages messages,
            MenuItems menuItems,
            Menus menus,
            TaskScheduler scheduler,
            Prompts prompts,
            NavigatorCategoryRegistryImpl navigatorCategoryRegistry,
            NavigatorEditorService navigatorEditorService,
            Player player) {
        super(
                messages,
                NAVIGATOR_SIZE,
                messages.getString("setup_navigator_title", player),
                MESSAGES,
                menuItems,
                menus,
                scheduler,
                prompts,
                navigatorEditorService);
        this.registry = navigatorCategoryRegistry;
    }

    @Override
    protected Registry<NavigatorCategory> registry() {
        return registry;
    }

    @Override
    protected ItemBuilder icon(NavigatorCategory category, Player player) {
        return ItemBuilder.icon(category, player);
    }

    @Override
    protected void openEditor(NavigatorCategory category, Player player) {
        menus.openCategoryEditor(category, player);
    }

    @Override
    protected void reopen(Player player) {
        menus.openNavigatorLayout(player);
    }

    @Override
    protected LayoutOccupant occupant() {
        return settingsButton;
    }

    /**
     * A new category is placed straight into the navigator when there is room, so it is visible immediately rather
     * than hiding in the palette.
     */
    @Override
    protected void afterCreate(NavigatorCategory created) {
        int freeSlot = firstFreeSlot();
        if (freeSlot >= 0) {
            created.setShown(true);
            created.setSlot(freeSlot);
        } else {
            // Grid is full: leave the new category in the palette rather than overwriting slot 0.
            created.setShown(false);
        }
        registry.persist(created);
    }

    /**
     * {@return the first unoccupied navigator slot, or {@code -1} when the grid is full} Callers must treat {@code -1}
     * as "no room" rather than placing at slot 0, which would overwrite the category already there.
     */
    private int firstFreeSlot() {
        Set<Integer> occupied = registry.getAll().stream()
                .filter(NavigatorCategory::isShown)
                .map(NavigatorCategory::getSlot)
                .collect(Collectors.toSet());
        // The settings button holds a navigator slot too; never hand its slot out, or the category placed there is
        // skipped by populate() and silently vanishes from the navigator.
        int settingsSlot = registry.getSettingsSlot();
        for (int slot = 0; slot < NAVIGATOR_SIZE; slot++) {
            if (slot != settingsSlot && !occupied.contains(slot)) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * The settings button: a navigator slot holder that is not a category. It renders in the grid (or in the palette
     * when it has been removed from the grid), can be picked up and dropped like a category, and steps aside when a
     * category is placed onto its slot.
     */
    private final class SettingsButton implements LayoutOccupant {

        private boolean held;

        @Override
        public boolean occupies(int slot) {
            return slot == registry.getSettingsSlot();
        }

        @Override
        public void renderInPreview(Player player, Inventory inventory) {
            int settingsSlot = registry.getSettingsSlot();
            if (!held && isSlotValid(settingsSlot)) {
                icon(player, "setup_navigator_settings_placed_lore").into(inventory, settingsSlot);
            }
        }

        @Override
        public void renderInPalette(Player player, Inventory playerInventory, int entryCount) {
            int slot = paletteSlot(entryCount);
            if (!held && registry.getSettingsSlot() < 0 && slot >= 0) {
                icon(player, "setup_navigator_settings_palette_lore").into(playerInventory, slot);
            }
        }

        @Override
        public boolean onPreviewClick(Player player, int slot) {
            if (held) {
                place(player, slot);
                return true;
            }
            if (NavigatorLayoutMenu.this.isHoldingEntry()) {
                // A held category is placed by the shared machinery, even onto the settings slot.
                return false;
            }
            if (slot == registry.getSettingsSlot()) {
                pickUp(player);
                return true;
            }
            return false;
        }

        @Override
        public boolean onPaletteClick(Player player, int slot) {
            if (registry.getSettingsSlot() < 0 && slot == paletteSlot(notAdded().size())) {
                pickUp(player);
                return true;
            }
            return false;
        }

        @Override
        public boolean onDroppedOutside(Player player) {
            if (!held) {
                return false;
            }
            registry.setSettingsSlot(-1);
            return true;
        }

        @Override
        public boolean displacedBy(int slot, int heldFromSlot) {
            if (slot != registry.getSettingsSlot()) {
                return false;
            }
            registry.setSettingsSlot(heldFromSlot >= 0 ? heldFromSlot : firstFreeSlot());
            return true;
        }

        @Override
        public boolean isBeingDragged() {
            return held;
        }

        @Override
        public void releaseDrag() {
            held = false;
        }

        private void place(Player player, int slot) {
            NavigatorCategory occupant = entryAtSlot(slot);
            if (occupant != null) {
                int previousSettingsSlot = registry.getSettingsSlot();
                if (isSlotValid(previousSettingsSlot)) {
                    occupant.setSlot(previousSettingsSlot);
                } else {
                    // Settings came from the palette (no slot to swap into); send the occupant to the palette rather
                    // than an invalid slot, which would hide it from the navigator entirely.
                    occupant.setShown(false);
                }
                registry.persist(occupant);
            }
            registry.setSettingsSlot(slot);
            clearHeld(player);
            refresh(player);
        }

        private void pickUp(Player player) {
            held = true;
            setCursorNextTick(player, icon(player, null).build());
            XSound.ITEM_ARMOR_EQUIP_LEATHER.play(player);
            refresh(player);
        }

        private ItemBuilder icon(Player player, @Nullable String loreKey) {
            ItemBuilder builder = ItemBuilder.skull(Profileable.detect(SkullTextures.SETTINGS))
                    .name(messages.getString("old_navigator_settings", player));
            return loreKey != null ? builder.lore(messages.getStringList(loreKey, player)) : builder;
        }

        /** The palette slot the settings token occupies, immediately after the not-yet-added categories. */
        private int paletteSlot(int entryCount) {
            int slot = PALETTE_FIRST_SLOT + entryCount;
            return slot <= PALETTE_LAST_SLOT ? slot : -1;
        }
    }
}
