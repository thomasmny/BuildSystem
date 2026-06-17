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

    // The taken-over player inventory: a back button (left hotbar) and create button (centre hotbar), with the palette
    // of not-yet-added categories filling the three main rows above.
    private static final int BACK_SLOT = 0;
    private static final int CREATE_SLOT = 4;
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
        // Empty navigator slots are the player's blank design-colour pane, exactly like the live navigator.
        for (int slot = 0; slot < NAVIGATOR_SIZE; slot++) {
            plugin.getMenuItems().addGlassPane(player, inventory, slot);
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

        ItemBuilder.of(XMaterial.BARRIER)
                .name(messages.getString("setup_back", player))
                .into(playerInventory, BACK_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                .name(messages.getString("setup_category_add", player))
                .lore(messages.getStringList("setup_navigator_create_lore", player))
                .into(playerInventory, CREATE_SLOT);

        // The palette holds only categories not currently in the navigator — the ones available to add.
        List<NavigatorCategory> notAdded = notAddedCategories();
        for (int i = 0; i < notAdded.size() && PALETTE_FIRST_SLOT + i <= PALETTE_LAST_SLOT; i++) {
            NavigatorCategory category = notAdded.get(i);
            int slot = PALETTE_FIRST_SLOT + i;
            // The picked-up category lives on the cursor; show a design-colour pane in its palette slot so it never
            // appears twice.
            if (category.getId().equals(heldCategoryId)) {
                plugin.getMenuItems().addGlassPane(player, playerInventory, slot);
                continue;
            }
            ItemBuilder.icon(category, player)
                    .name(ColorAPI.process(category.getColor() + category.getDisplayName()))
                    .lore(messages.getStringList("setup_navigator_palette_lore", player))
                    .into(playerInventory, slot);
        }
    }

    /**
     * {@return the categories not currently shown in the navigator, ordered as the registry lists them}
     */
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
            handleNavigatorClick(player, event.getRawSlot(), event.isRightClick());
        } else if (event.getClickedInventory() == player.getInventory()) {
            handlePlayerClick(player, event.getSlot(), event.isRightClick());
        } else {
            // Click outside any inventory: while placing, this drops the held category off the navigator.
            handleOutsideClickWhileHolding(player);
        }
    }

    private void handleNavigatorClick(Player player, int slot, boolean rightClick) {
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

        // Nothing held.
        if (slot == registry.getSettingsSlot()) {
            pickUpSettings(player);
            return;
        }
        NavigatorCategoryImpl occupant = categoryAtSlot(slot);
        if (occupant != null) {
            if (rightClick) {
                new CategoryEditorMenu(plugin, player, occupant).open(player);
            } else {
                pickUpCategory(player, occupant.getId(), slot);
            }
        }
        // An empty slot with nothing held does nothing; categories are created via the create button.
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
        // While holding, clicking out of the navigator (into the palette) drops the category off the navigator.
        if (heldCategoryId != null || heldSettings) {
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

        int paletteIndex = slot - PALETTE_FIRST_SLOT;
        List<NavigatorCategory> notAdded = notAddedCategories();
        if (paletteIndex < 0 || paletteIndex >= notAdded.size() || slot > PALETTE_LAST_SLOT) {
            return;
        }
        NavigatorCategory clicked = notAdded.get(paletteIndex);
        if (rightClick) {
            new CategoryEditorMenu(plugin, player, clicked).open(player);
        } else {
            pickUpCategory(player, clicked.getId(), -1);
        }
    }

    /**
     * While a category is held, a click outside the navigator preview drops it off the navigator (it returns to the
     * palette). A held settings button cannot be removed, so the click simply cancels the pickup.
     */
    private void handleOutsideClickWhileHolding(Player player) {
        if (heldSettings) {
            clearHeld(player);
            refresh(player);
            return;
        }
        if (heldCategoryId != null) {
            NavigatorCategoryImpl held =
                    (NavigatorCategoryImpl) registry.getCategory(heldCategoryId).orElse(null);
            if (held != null) {
                held.setShownInNavigator(false);
                registry.persist(held);
            }
            clearHeld(player);
            refresh(player);
        }
    }

    private void beginCategoryCreation(Player player) {
        PlayerChatInput.requestSanitizedName(
                plugin,
                player,
                "setup_category_add_prompt",
                "setup_name_invalid_characters",
                "setup_name_empty",
                name -> {
                    // New categories start in the navigator at the first free slot so they are immediately visible.
                    NavigatorCategoryImpl created = registry.createCategory(name);
                    created.setShownInNavigator(true);
                    created.setNavigatorSlot(firstFreeSlot());
                    registry.persist(created);
                    new CategoryEditorMenu(plugin, player, created).open(player);
                });
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
        // Re-render so the picked-up category's source slot (top preview or palette) shows a glass pane immediately.
        refresh(player);
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
        refresh(player);
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
