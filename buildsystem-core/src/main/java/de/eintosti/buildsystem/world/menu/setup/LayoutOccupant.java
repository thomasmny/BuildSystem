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

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

/**
 * A grid occupant in a {@link LayoutEditorMenu} that is not a registry entry — currently only the navigator's settings
 * button, which holds a navigator slot and can be dragged like a category without being one.
 *
 * <p>The editor drives drag-and-drop for registry entries itself and defers to an occupant for everything it does not
 * recognise. Gathering those callbacks behind one type keeps the whole of a non-entry item's behaviour in a single
 * named class, and lets a layout that has no such item say so ({@link #NONE}) instead of leaving the reader to infer it
 * from which methods it did not override.
 */
@NullMarked
interface LayoutOccupant {

    /**
     * A layout with nothing but registry entries in its grid, such as the status editor.
     */
    LayoutOccupant NONE = new LayoutOccupant() {};

    /**
     * {@return whether this occupant holds the given preview slot} A held slot is skipped when entries are rendered and
     * when a free slot is searched for.
     *
     * @param slot The preview slot to test
     */
    default boolean occupies(int slot) {
        return false;
    }

    /**
     * Renders this occupant into the preview grid.
     *
     * @param player The viewing player
     * @param inventory The preview inventory
     */
    default void renderInPreview(Player player, Inventory inventory) {}

    /**
     * Renders this occupant into the palette, after the entries that are not in the grid.
     *
     * @param player The viewing player
     * @param playerInventory The player's inventory, which backs the palette
     * @param entryCount How many entries precede this occupant in the palette
     */
    default void renderInPalette(Player player, Inventory playerInventory, int entryCount) {}

    /**
     * Handles a preview click the editor's own entry handling should not take.
     *
     * @param player The clicking player
     * @param slot The clicked preview slot
     * @return {@code true} if the click was consumed
     */
    default boolean onPreviewClick(Player player, int slot) {
        return false;
    }

    /**
     * Handles a palette click the editor's own entry handling should not take.
     *
     * @param player The clicking player
     * @param slot The clicked palette slot
     * @return {@code true} if the click was consumed
     */
    default boolean onPaletteClick(Player player, int slot) {
        return false;
    }

    /**
     * Handles this occupant being dropped outside the inventory, which removes it from the grid.
     *
     * @param player The clicking player
     * @return {@code true} if the drop was consumed
     */
    default boolean onDroppedOutside(Player player) {
        return false;
    }

    /**
     * Moves this occupant aside so an entry can take the slot it holds.
     *
     * @param slot The slot the entry is being placed into
     * @param heldFromSlot The slot the entry came from, or {@code -1} when it came from the palette
     * @return {@code true} if this occupant held the slot and moved
     */
    default boolean displacedBy(int slot, int heldFromSlot) {
        return false;
    }

    /**
     * {@return whether this occupant is currently on the player's cursor} While it is, the editor must not treat a
     * click as picking up an entry.
     */
    default boolean isBeingDragged() {
        return false;
    }

    /**
     * Drops this occupant from the cursor, called whenever the editor clears what the player is holding.
     */
    default void releaseDrag() {}
}
