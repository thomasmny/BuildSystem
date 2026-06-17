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
 * palette of every category and the settings token, plus add/remove/done controls.
 *
 * <p>Interaction is a controlled pick-and-place. Picking up a palette category, or one already placed, puts it on the
 * cursor; clicking a navigator slot drops it there (moving, or swapping with the occupant). The settings token is moved
 * the same way. Right-clicking a palette category opens its {@link CategoryEditorMenu}. Dropping a held category onto
 * the remove control takes it off the navigator. All clicks are cancelled, so the real items can never be taken; the
 * category registry is the single source of truth and persists every change.
 */
@NullMarked
public class NavigatorLayoutMenu extends Menu {

    private static final int NAVIGATOR_SIZE = 27;

    // Control slots within the player's (taken-over) inventory, addressed by InventoryClickEvent#getSlot.
    private static final int CONTROL_ADD_SLOT = 2;
    private static final int CONTROL_REMOVE_SLOT = 4;
    private static final int CONTROL_DONE_SLOT = 6;
    private static final int CONTROL_SETTINGS_SLOT = 8;
    private static final int PALETTE_FIRST_SLOT = 9;
    private static final int PALETTE_LAST_SLOT = 35;

    private final BuildSystemPlugin plugin;
    private final NavigatorCategoryRegistryImpl registry;

    private @Nullable String heldCategoryId;
    private boolean heldSettings;
    private int heldFromSlot = -1;

    public NavigatorLayoutMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), NAVIGATOR_SIZE, plugin.getMessages().getString("setup_navigator_title", player));
        this.plugin = plugin;
        this.registry = plugin.getNavigatorCategoryRegistry();
    }

    @Override
    public void open(Player player) {
        plugin.getNavigatorEditorService().beginSession(player);
        refresh(player);
        player.openInventory(getInventory());
    }

    @Override
    protected void populate(Player player) {
        Inventory inventory = getInventory();
        for (int slot = 0; slot < NAVIGATOR_SIZE; slot++) {
            ItemBuilder.of(XMaterial.GRAY_STAINED_GLASS_PANE)
                    .name(messages.getString("setup_navigator_empty_slot", player))
                    .into(inventory, slot);
        }

        int settingsSlot = registry.getSettingsSlot();
        if (!heldSettings && settingsSlot >= 0 && settingsSlot < NAVIGATOR_SIZE) {
            ItemBuilder.skull(Profileable.detect(SkullTextures.SETTINGS))
                    .name(messages.getString("old_navigator_settings", player))
                    .lore(messages.getStringList("setup_navigator_placed_lore", player))
                    .into(inventory, settingsSlot);
        }

        for (NavigatorCategory category : registry.getCategories()) {
            int slot = category.getNavigatorSlot();
            if (!category.isShownInNavigator() || slot < 0 || slot >= NAVIGATOR_SIZE || slot == settingsSlot) {
                continue;
            }
            if (category.getId().equals(heldCategoryId)) {
                continue; // currently on the cursor
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

        ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                .name(messages.getString("setup_category_add", player))
                .into(playerInventory, CONTROL_ADD_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.CANCEL))
                .name(messages.getString("setup_navigator_remove", player))
                .lore(messages.getStringList("setup_navigator_remove_lore", player))
                .into(playerInventory, CONTROL_REMOVE_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.CONFIRM))
                .name(messages.getString("setup_navigator_done", player))
                .into(playerInventory, CONTROL_DONE_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.SETTINGS))
                .name(messages.getString("old_navigator_settings", player))
                .lore(messages.getStringList("setup_navigator_settings_lore", player))
                .glow(heldSettings)
                .into(playerInventory, CONTROL_SETTINGS_SLOT);

        List<NavigatorCategory> categories = List.copyOf(registry.getCategories());
        for (int i = 0; i < categories.size() && PALETTE_FIRST_SLOT + i <= PALETTE_LAST_SLOT; i++) {
            NavigatorCategory category = categories.get(i);
            ItemBuilder.icon(category, player)
                    .name(ColorAPI.process(category.getColor() + category.getDisplayName()))
                    .lore(messages.getStringList(
                            category.isShownInNavigator()
                                    ? "setup_navigator_palette_shown_lore"
                                    : "setup_navigator_palette_hidden_lore",
                            player))
                    .glow(category.getId().equals(heldCategoryId))
                    .into(playerInventory, PALETTE_FIRST_SLOT + i);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == getInventory()) {
            handleNavigatorClick(player, event.getRawSlot());
        } else if (event.getClickedInventory() == player.getInventory()) {
            handlePlayerClick(player, event.getSlot(), event.isRightClick());
        }
    }

    private void handleNavigatorClick(Player player, int slot) {
        if (slot < 0 || slot >= NAVIGATOR_SIZE) {
            return;
        }

        if (heldSettings) {
            placeSettings(player, slot);
            return;
        }
        if (heldCategoryId != null) {
            placeHeldCategory(player, slot);
            return;
        }

        // Nothing held: pick up whatever sits here (a category, or the settings button) so it can be moved.
        if (slot == registry.getSettingsSlot()) {
            pickUpSettings(player);
        } else {
            NavigatorCategoryImpl occupant = categoryAtSlot(slot);
            if (occupant != null) {
                pickUpCategory(player, occupant.getId(), slot);
            }
        }
    }

    private void placeHeldCategory(Player player, int slot) {
        NavigatorCategoryImpl held =
                (NavigatorCategoryImpl) registry.getCategory(heldCategoryId).orElse(null);
        if (held == null) {
            clearHeld(player);
            return;
        }
        if (slot == registry.getSettingsSlot()) {
            // The settings button owns this slot; move it aside to where the held category came from (if any).
            registry.setSettingsSlot(heldFromSlot >= 0 ? heldFromSlot : firstFreeSlot());
        } else {
            NavigatorCategoryImpl occupant = categoryAtSlot(slot);
            if (occupant != null && !occupant.equals(held)) {
                if (heldFromSlot >= 0) {
                    occupant.setNavigatorSlot(heldFromSlot);
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
            // Displace the category that held this slot, sending it back to where settings was.
            occupant.setNavigatorSlot(registry.getSettingsSlot());
            registry.persist(occupant);
        }
        registry.setSettingsSlot(slot);
        clearHeld(player);
        refresh(player);
    }

    private void handlePlayerClick(Player player, int slot, boolean rightClick) {
        switch (slot) {
            case CONTROL_DONE_SLOT -> {
                player.closeInventory();
                return;
            }
            case CONTROL_ADD_SLOT -> {
                beginCategoryCreation(player);
                return;
            }
            case CONTROL_REMOVE_SLOT -> {
                removeHeldCategory(player);
                return;
            }
            case CONTROL_SETTINGS_SLOT -> {
                if (heldSettings) {
                    clearHeld(player);
                } else {
                    pickUpSettings(player);
                }
                refresh(player);
                return;
            }
            default -> {
                // fall through to palette handling
            }
        }

        int paletteIndex = slot - PALETTE_FIRST_SLOT;
        List<NavigatorCategory> categories = List.copyOf(registry.getCategories());
        if (paletteIndex < 0 || paletteIndex >= categories.size() || slot > PALETTE_LAST_SLOT) {
            return;
        }
        NavigatorCategory clicked = categories.get(paletteIndex);
        if (rightClick) {
            new CategoryEditorMenu(plugin, player, clicked).open(player);
            return;
        }
        if (clicked.getId().equals(heldCategoryId)) {
            clearHeld(player);
        } else {
            pickUpCategory(player, clicked.getId(), -1);
        }
        refresh(player);
    }

    private void removeHeldCategory(Player player) {
        if (heldCategoryId == null) {
            return;
        }
        NavigatorCategoryImpl held =
                (NavigatorCategoryImpl) registry.getCategory(heldCategoryId).orElse(null);
        if (held != null) {
            held.setShownInNavigator(false);
            registry.persist(held);
        }
        clearHeld(player);
        refresh(player);
    }

    private void beginCategoryCreation(Player player) {
        PlayerChatInput.requestSanitizedName(
                plugin,
                player,
                "setup_category_add_prompt",
                "setup_name_invalid_characters",
                "setup_name_empty",
                name -> new CategoryEditorMenu(plugin, player, registry.createCategory(name)).open(player));
    }

    private void pickUpCategory(Player player, String categoryId, int fromSlot) {
        this.heldCategoryId = categoryId;
        this.heldSettings = false;
        this.heldFromSlot = fromSlot;
        NavigatorCategory category = registry.getCategory(categoryId).orElse(null);
        ItemStack cursor = category == null
                ? null
                : ItemBuilder.icon(category, player)
                        .name(ColorAPI.process(category.getColor() + category.getDisplayName()))
                        .build();
        setCursorNextTick(player, cursor);
        XSound.ITEM_ARMOR_EQUIP_LEATHER.play(player);
    }

    private void pickUpSettings(Player player) {
        this.heldSettings = true;
        this.heldCategoryId = null;
        this.heldFromSlot = -1;
        setCursorNextTick(
                player,
                ItemBuilder.skull(Profileable.detect(SkullTextures.SETTINGS))
                        .name(messages.getString("old_navigator_settings", player))
                        .build());
        XSound.ITEM_ARMOR_EQUIP_LEATHER.play(player);
    }

    private void clearHeld(Player player) {
        this.heldCategoryId = null;
        this.heldSettings = false;
        this.heldFromSlot = -1;
        setCursorNextTick(player, null);
    }

    /**
     * Applies the cursor item one tick after the click. Setting the cursor inside an {@link InventoryClickEvent} can be
     * overwritten by the client's own post-event cursor sync, so the change is deferred to take reliable effect.
     */
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
        for (NavigatorCategory category : registry.getCategories()) {
            if (category.isShownInNavigator() && category.getNavigatorSlot() == slot) {
                return (NavigatorCategoryImpl) category;
            }
        }
        return null;
    }

    /**
     * {@return the first navigator slot not occupied by a shown category, or {@code 0} when the navigator is full}
     */
    private int firstFreeSlot() {
        for (int slot = 0; slot < NAVIGATOR_SIZE; slot++) {
            if (categoryAtSlot(slot) == null) {
                return slot;
            }
        }
        return 0;
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        plugin.getNavigatorEditorService().restore((Player) event.getPlayer());
    }
}
