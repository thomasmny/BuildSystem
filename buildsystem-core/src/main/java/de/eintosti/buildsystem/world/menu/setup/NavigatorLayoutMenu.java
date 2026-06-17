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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.Menu;
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
 * The drag-and-drop navigator layout editor. The top inventory is a live preview of the inventory navigator: each
 * category shown in the navigator sits at its slot. The player's own inventory is taken over (snapshotted and cleared
 * by {@link de.eintosti.buildsystem.navigator.NavigatorEditorService}) to present a palette of every category plus the
 * controls, and is restored when the editor closes.
 *
 * <p>Editing is a controlled pick-and-place: clicking a palette category (or an already-placed one) "picks it up" onto
 * the cursor, and clicking a navigator slot drops it there — moving it, swapping with whatever was there, or adding it
 * to the navigator. Dropping a placed category onto the remove control takes it out of the navigator. All clicks are
 * cancelled, so the real items can never be taken; the model is the single source of truth and is re-rendered after
 * every change.
 */
@NullMarked
public class NavigatorLayoutMenu extends Menu {

    private static final int NAVIGATOR_SIZE = 27;

    // Player-inventory control slots (raw slots NAVIGATOR_SIZE + n).
    private static final int PALETTE_FIRST_SLOT = 0;
    private static final int PALETTE_LAST_SLOT = 26;
    private static final int CONTROL_REMOVE_SLOT = 30;
    private static final int CONTROL_DONE_SLOT = 34;

    private final BuildSystemPlugin plugin;
    private final NavigatorCategoryRegistryImpl registry;

    private @Nullable String heldCategoryId;
    private int heldFromSlot = -1;

    public NavigatorLayoutMenu(BuildSystemPlugin plugin, Player player) {
        super(plugin.getMessages(), NAVIGATOR_SIZE, plugin.getMessages().getString("setup_navigator_title", player));
        this.plugin = plugin;
        this.registry = plugin.getNavigatorCategoryRegistry();
    }

    @Override
    public void open(Player player) {
        plugin.getNavigatorEditorService().beginSession(player);
        populate(player);
        renderControls(player);
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
        for (NavigatorCategory category : registry.getCategories()) {
            int slot = category.getNavigatorSlot();
            if (!category.isShownInNavigator() || slot < 0 || slot >= NAVIGATOR_SIZE) {
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
        ItemBuilder.of(XMaterial.LAVA_BUCKET)
                .name(messages.getString("setup_navigator_remove", player))
                .lore(messages.getStringList("setup_navigator_remove_lore", player))
                .into(playerInventory, CONTROL_REMOVE_SLOT);
        ItemBuilder.of(XMaterial.LIME_DYE)
                .name(messages.getString("setup_navigator_done", player))
                .into(playerInventory, CONTROL_DONE_SLOT);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        boolean topInventory = event.getClickedInventory() == getInventory();
        if (topInventory) {
            handleNavigatorClick(player, event.getRawSlot());
        } else if (event.getClickedInventory() == player.getInventory()) {
            handlePlayerClick(player, event.getSlot());
        }
    }

    private void handleNavigatorClick(Player player, int slot) {
        if (slot < 0 || slot >= NAVIGATOR_SIZE) {
            return;
        }
        NavigatorCategoryImpl occupant = categoryAtSlot(slot);

        if (heldCategoryId == null) {
            // Nothing held: pick up whatever sits here so it can be moved.
            if (occupant != null) {
                pickUp(player, occupant.getId(), slot);
            }
            return;
        }

        NavigatorCategoryImpl held =
                (NavigatorCategoryImpl) registry.getCategory(heldCategoryId).orElse(null);
        if (held == null) {
            clearHeld(player);
            return;
        }

        if (occupant != null && !occupant.equals(held)) {
            // Swap: the displaced category takes the held one's previous slot, or leaves the navigator if the held one
            // came from the palette.
            if (heldFromSlot >= 0) {
                occupant.setNavigatorSlot(heldFromSlot);
            } else {
                occupant.setShownInNavigator(false);
            }
            registry.persist(occupant);
        }

        held.setNavigatorSlot(slot);
        held.setShownInNavigator(true);
        registry.persist(held);

        clearHeld(player);
        refresh(player);
    }

    private void handlePlayerClick(Player player, int slot) {
        if (slot == CONTROL_DONE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == CONTROL_REMOVE_SLOT) {
            if (heldCategoryId != null) {
                NavigatorCategoryImpl held = (NavigatorCategoryImpl)
                        registry.getCategory(heldCategoryId).orElse(null);
                if (held != null) {
                    held.setShownInNavigator(false);
                    registry.persist(held);
                }
                clearHeld(player);
                refresh(player);
            }
            return;
        }

        int paletteIndex = slot - PALETTE_FIRST_SLOT;
        List<NavigatorCategory> categories = List.copyOf(registry.getCategories());
        if (paletteIndex < 0 || paletteIndex >= categories.size() || slot > PALETTE_LAST_SLOT) {
            return;
        }
        String clickedId = categories.get(paletteIndex).getId();
        if (clickedId.equals(heldCategoryId)) {
            clearHeld(player);
        } else {
            pickUp(player, clickedId, -1);
        }
        refresh(player);
    }

    private void pickUp(Player player, String categoryId, int fromSlot) {
        this.heldCategoryId = categoryId;
        this.heldFromSlot = fromSlot;
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

    private void clearHeld(Player player) {
        this.heldCategoryId = null;
        this.heldFromSlot = -1;
        setCursorNextTick(player, null);
    }

    /**
     * Applies the cursor item one tick after the click. Setting the cursor inside an {@link InventoryClickEvent} can be
     * overwritten by the client's own post-event cursor sync, so the change is deferred to take reliable effect.
     */
    private void setCursorNextTick(Player player, @Nullable ItemStack cursor) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Skip if the editor closed in the meantime, so a deferred cursor item is never dropped into the world.
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

    @Override
    public void handleClose(InventoryCloseEvent event) {
        plugin.getNavigatorEditorService().restore((Player) event.getPlayer());
    }
}
