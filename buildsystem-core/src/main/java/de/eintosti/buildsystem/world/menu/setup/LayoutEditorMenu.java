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
import de.eintosti.buildsystem.api.world.display.Registry;
import de.eintosti.buildsystem.api.world.display.RegistryEntry;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.ItemBuilder;
import de.eintosti.buildsystem.menu.Menu;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.menu.SkullTextures;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.util.color.ColorAPI;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The drag-and-drop layout editor shared by the navigator and status setups. The top inventory is a live preview of
 * the menu being arranged; the player's own inventory is taken over (snapshotted and cleared by the
 * {@link NavigatorEditorService}, restored on close/quit/shutdown) to show a back button, a create button, reset and
 * delete controls, and the palette of entries not currently placed.
 *
 * <p>Entries are picked up by clicking, placed by clicking a slot, swapped by dropping onto an occupant, hidden by
 * dropping outside, and deleted by dropping on the bin. Shift-clicking opens the entry's editor.
 *
 * <p>Subclasses supply their registry, their icon rendering, their message keys, and their editor navigation. The
 * navigator additionally drags a settings button that is not a registry entry; the {@code ...Extra...} hooks exist for
 * it and default to doing nothing.
 *
 * @param <T> The registry entry type being arranged
 */
@NullMarked
public abstract class LayoutEditorMenu<T extends RegistryEntry> extends Menu {

    protected static final int BACK_SLOT = 0;
    protected static final int CREATE_SLOT = 4;
    protected static final int RESET_SLOT = 7;
    protected static final int DELETE_SLOT = 8;
    protected static final int PALETTE_FIRST_SLOT = 9;
    protected static final int PALETTE_LAST_SLOT = 35;

    /**
     * The message keys that differ between the two editors. Grouped as data so the shared code reads them by role
     * rather than each subclass overriding a dozen accessors.
     *
     * @param createItem The create button's name
     * @param createLore The create button's lore
     * @param reset The reset button's name, reused as the confirm-dialog title
     * @param resetLore The reset button's lore
     * @param resetConfirmLore The confirm lore when resetting only the layout
     * @param resetAllConfirmLore The confirm lore when resetting every entry to defaults
     * @param delete The delete (bin) button's name
     * @param deleteLore The delete button's lore
     * @param placedLore The lore on an entry sitting in the preview
     * @param paletteLore The lore on an entry sitting in the palette
     * @param addPrompt The prompt title when creating an entry
     */
    protected record LayoutMessages(
            String createItem,
            String createLore,
            String reset,
            String resetLore,
            String resetConfirmLore,
            String resetAllConfirmLore,
            String delete,
            String deleteLore,
            String placedLore,
            String paletteLore,
            String addPrompt) {}

    private final int previewSize;
    private final LayoutMessages keys;
    protected final MenuItems menuItems;
    protected final Menus menus;
    protected final Prompts prompts;
    private final TaskScheduler scheduler;
    private final NavigatorEditorService navigatorEditorService;
    private final Held held = new Held();

    protected LayoutEditorMenu(
            Messages messages,
            int previewSize,
            String title,
            LayoutMessages keys,
            MenuItems menuItems,
            Menus menus,
            TaskScheduler scheduler,
            Prompts prompts,
            NavigatorEditorService navigatorEditorService) {
        super(messages, previewSize, title);
        this.previewSize = previewSize;
        this.keys = keys;
        this.menuItems = menuItems;
        this.menus = menus;
        this.scheduler = scheduler;
        this.prompts = prompts;
        this.navigatorEditorService = navigatorEditorService;
    }

    // ---------------------------------------------------------------- subclass contract

    /**
     * {@return the registry being arranged}
     */
    protected abstract Registry<T> registry();

    /**
     * {@return a builder for the entry's icon, already resolved but not yet named}
     *
     * @param entry The entry to render
     * @param player The viewing player
     */
    protected abstract ItemBuilder icon(T entry, Player player);

    /**
     * Opens the per-entry editor, reached by shift-clicking an entry.
     *
     * @param entry The entry to edit
     * @param player The viewing player
     */
    protected abstract void openEditor(T entry, Player player);

    /**
     * Re-opens this editor, used after a confirm dialog or a prompt is cancelled.
     *
     * @param player The viewing player
     */
    protected abstract void reopen(Player player);

    /**
     * {@return the non-entry item this layout keeps in its grid} Defaults to {@link LayoutOccupant#NONE}; a subclass
     * that has one (the navigator's settings button) overrides this.
     */
    protected LayoutOccupant occupant() {
        return LayoutOccupant.NONE;
    }

    /**
     * Hook run after an entry is created, before its editor opens.
     *
     * @param created The newly created entry
     */
    protected void afterCreate(T created) {}

    // ---------------------------------------------------------------- rendering

    @Override
    public void open(Player player) {
        navigatorEditorService.beginSession(player);
        populate(player);
        player.openInventory(getInventory());
        renderControls(player);
    }

    @Override
    protected void populate(Player player) {
        Inventory inventory = getInventory();
        for (int slot = 0; slot < previewSize; slot++) {
            menuItems.addGlassPane(player, inventory, slot);
        }

        occupant().renderInPreview(player, inventory);

        for (T entry : registry().getAll()) {
            int slot = entry.getSlot();
            if (!entry.isShown() || !isSlotValid(slot) || occupant().occupies(slot) || isHeld(entry)) {
                continue;
            }
            icon(entry, player)
                    .name(ColorAPI.process(entry.getStyledName()))
                    .lore(messages.getStringList(keys.placedLore(), player))
                    .into(inventory, slot);
        }
    }

    protected final void renderControls(Player player) {
        Inventory playerInventory = player.getInventory();
        playerInventory.clear();

        ItemBuilder.of(XMaterial.BARRIER)
                .name(messages.getString("setup_back", player))
                .into(playerInventory, BACK_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.ADD_ITEM))
                .name(messages.getString(keys.createItem(), player))
                .lore(messages.getStringList(keys.createLore(), player))
                .into(playerInventory, CREATE_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.RESET))
                .name(messages.getString(keys.reset(), player))
                .lore(messages.getStringList(keys.resetLore(), player))
                .into(playerInventory, RESET_SLOT);
        ItemBuilder.skull(Profileable.detect(SkullTextures.DELETE))
                .name(messages.getString(keys.delete(), player))
                .lore(messages.getStringList(keys.deleteLore(), player))
                .into(playerInventory, DELETE_SLOT);

        List<T> notAdded = notAdded();
        for (int i = 0; i < notAdded.size() && PALETTE_FIRST_SLOT + i <= PALETTE_LAST_SLOT; i++) {
            T entry = notAdded.get(i);
            if (isHeld(entry)) {
                continue;
            }
            icon(entry, player)
                    .name(ColorAPI.process(entry.getStyledName()))
                    .lore(messages.getStringList(keys.paletteLore(), player))
                    .into(playerInventory, PALETTE_FIRST_SLOT + i);
        }

        occupant().renderInPalette(player, playerInventory, notAdded.size());
    }

    protected final List<T> notAdded() {
        return registry().getAll().stream().filter(entry -> !entry.isShown()).toList();
    }

    protected final void refresh(Player player) {
        populate(player);
        renderControls(player);
    }

    // ---------------------------------------------------------------- clicks

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == getInventory()) {
            handlePreviewClick(player, event.getRawSlot(), event.isShiftClick());
        } else if (event.getClickedInventory() == player.getInventory()) {
            handlePlayerClick(player, event.getSlot(), event.isRightClick(), event.isShiftClick());
        } else {
            handleOutsideClickWhileHolding(player);
        }
    }

    private void handlePreviewClick(Player player, int slot, boolean shiftClick) {
        if (!isSlotValid(slot)) {
            return;
        }
        if (occupant().onPreviewClick(player, slot)) {
            return;
        }
        if (held.isHolding()) {
            placeHeld(player, slot);
            return;
        }

        T occupant = entryAtSlot(slot);
        if (occupant == null) {
            return;
        }
        if (shiftClick) {
            openEditor(occupant, player);
        } else {
            pickUp(player, occupant.getId(), slot);
        }
    }

    /**
     * Places the held entry at {@code slot}. An occupant is displaced to the slot the held entry came from, or hidden
     * when the held entry came from the palette and has no slot to give back.
     */
    protected final void placeHeld(Player player, int slot) {
        T heldEntry = currentHeld();
        if (heldEntry == null) {
            clearHeld(player);
            return;
        }

        if (!occupant().displacedBy(slot, held.getFromSlot())) {
            T occupant = entryAtSlot(slot);
            if (occupant != null && !occupant.getId().equals(heldEntry.getId())) {
                if (held.getFromSlot() >= 0) {
                    occupant.setSlot(held.getFromSlot());
                } else {
                    occupant.setShown(false);
                }
                registry().persist(occupant);
            }
        }

        heldEntry.setSlot(slot);
        heldEntry.setShown(true);
        registry().persist(heldEntry);
        clearHeld(player);
        refresh(player);
    }

    private void handlePlayerClick(Player player, int slot, boolean rightClick, boolean shiftClick) {
        if (held.isHolding() && slot == DELETE_SLOT) {
            deleteHeld(player);
            return;
        }
        if (held.isHolding() || occupant().isBeingDragged()) {
            handleOutsideClickWhileHolding(player);
            return;
        }

        if (slot == BACK_SLOT) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            player.closeInventory();
            menus.openSetup(player);
            return;
        }
        if (slot == CREATE_SLOT) {
            beginCreation(player);
            return;
        }
        if (slot == RESET_SLOT) {
            // Left-click resets only the layout; right-click resets every entry to the built-in defaults.
            promptReset(player, rightClick);
            return;
        }
        if (occupant().onPaletteClick(player, slot)) {
            return;
        }

        int paletteIndex = slot - PALETTE_FIRST_SLOT;
        List<T> notAdded = notAdded();
        if (paletteIndex < 0 || paletteIndex >= notAdded.size() || slot > PALETTE_LAST_SLOT) {
            return;
        }

        T clicked = notAdded.get(paletteIndex);
        if (shiftClick) {
            openEditor(clicked, player);
        } else {
            pickUp(player, clicked.getId(), -1);
        }
    }

    private void deleteHeld(Player player) {
        if (held.isHolding() && registry().delete(held.getEntryId())) {
            XSound.ENTITY_ITEM_BREAK.play(player);
        }
        clearHeld(player);
        refresh(player);
    }

    /**
     * Dropping outside puts the held entry back in the palette rather than deleting it; deletion is the bin's job.
     */
    protected final void handleOutsideClickWhileHolding(Player player) {
        if (!occupant().onDroppedOutside(player)) {
            T heldEntry = currentHeld();
            if (heldEntry != null) {
                heldEntry.setShown(false);
                registry().persist(heldEntry);
            }
        }
        clearHeld(player);
        refresh(player);
    }

    private void promptReset(Player player, boolean resetEverything) {
        String confirmLoreKey = resetEverything ? keys.resetAllConfirmLore() : keys.resetConfirmLore();
        menus.openDeletionConfirm(
                player,
                messages.getString(keys.reset(), player),
                messages.getStringList(confirmLoreKey, player),
                () -> {
                    if (resetEverything) {
                        registry().resetToDefaults();
                    } else {
                        registry().resetLayout();
                    }
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    reopen(player);
                },
                () -> reopen(player));
    }

    private void beginCreation(Player player) {
        prompts.prompt(player)
                .title(keys.addPrompt())
                .sanitizeName("setup_name_invalid_characters", "setup_name_empty")
                .onCancel(() -> reopen(player))
                .request(name -> {
                    T created = registry().create(name);
                    afterCreate(created);
                    openEditor(created, player);
                });
    }

    // ---------------------------------------------------------------- cursor

    protected final void pickUp(Player player, String entryId, int fromSlot) {
        held.track(entryId, fromSlot);
        T entry = registry().get(entryId).orElse(null);
        ItemStack cursor = entry == null
                ? null
                : icon(entry, player)
                        .name(ColorAPI.process(entry.getStyledName()))
                        .build();

        setCursorNextTick(player, cursor);
        XSound.ITEM_ARMOR_EQUIP_LEATHER.play(player);
        refresh(player);
    }

    protected final void clearHeld(Player player) {
        held.reset();
        occupant().releaseDrag();
        setCursorNextTick(player, null);
    }

    /**
     * {@return whether the player is dragging a registry entry} An {@link LayoutOccupant occupant} uses this to tell an
     * entry drop apart from one of its own.
     */
    protected final boolean isHoldingEntry() {
        return held.isHolding();
    }

    protected final void setCursorNextTick(Player player, @Nullable ItemStack cursor) {
        scheduler.run(() -> {
            if (navigatorEditorService.hasSession(player)) {
                player.setItemOnCursor(cursor);
            }
        });
    }

    // ---------------------------------------------------------------- lookup

    protected final @Nullable T entryAtSlot(int slot) {
        return registry().getAll().stream()
                .filter(entry -> entry.isShown() && entry.getSlot() == slot)
                .findFirst()
                .orElse(null);
    }

    protected final @Nullable T currentHeld() {
        String id = held.getEntryId();
        return id == null ? null : registry().get(id).orElse(null);
    }

    private boolean isHeld(T entry) {
        return entry.getId().equals(held.getEntryId());
    }

    protected final boolean isSlotValid(int slot) {
        return slot >= 0 && slot < previewSize;
    }

    protected final int previewSize() {
        return previewSize;
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        navigatorEditorService.restore((Player) event.getPlayer());
    }

    /**
     * Pick-and-place tracking for the entry currently held on the cursor. {@code fromSlot} is {@code -1} when the
     * entry was taken from the palette, which is what distinguishes "swap" from "displace into the palette".
     */
    private static final class Held {
        private @Nullable String entryId;
        private int fromSlot = -1;

        void track(String entryId, int fromSlot) {
            this.entryId = entryId;
            this.fromSlot = fromSlot;
        }

        void reset() {
            this.entryId = null;
            this.fromSlot = -1;
        }

        @Nullable String getEntryId() {
            return entryId;
        }

        boolean isHolding() {
            return entryId != null;
        }

        int getFromSlot() {
            return fromSlot;
        }
    }
}
