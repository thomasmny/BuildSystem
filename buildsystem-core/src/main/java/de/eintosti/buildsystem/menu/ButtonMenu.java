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
package de.eintosti.buildsystem.menu;

import de.eintosti.buildsystem.i18n.Messages;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Menu} whose slots are described as a registry of {@link MenuButton}s, so each slot's render and click
 * behavior is declared once instead of being split across a {@code populate} call and a {@code handleClick} switch.
 *
 * <p>Subclasses {@link #register(int, MenuButton) register} their buttons (typically from their constructor) and
 * delegate {@link #populate(Player)} to {@link #renderButtons(Player)}. Clicks are handled by the base
 * {@link #handleClick(InventoryClickEvent)}, which routes to the registered button or to {@link #onUnhandledClick}; a
 * subclass overrides {@code populate} (to fill background items before rendering) and {@code onUnhandledClick} (e.g. to
 * treat a click on the border as "go back") rather than touching click dispatch directly.
 *
 * @param <B> The concrete button type, allowing subclasses to attach their own per-slot metadata while reusing the
 *     shared registry, render loop, and click dispatch
 */
@NullMarked
public abstract class ButtonMenu<B extends MenuButton> extends Menu {

    private final Map<Integer, B> buttons = new LinkedHashMap<>();

    /**
     * Creates the menu and its backing inventory.
     *
     * @param messages The message provider
     * @param size The inventory size in slots (a multiple of 9)
     * @param title The inventory title shown to the player
     */
    protected ButtonMenu(Messages messages, int size, String title) {
        super(messages, size, title);
    }

    /**
     * Registers (or replaces) the button shown at the given slot.
     *
     * @param slot The inventory slot
     * @param button The button to place at the slot
     */
    protected final void register(int slot, B button) {
        buttons.put(slot, button);
    }

    /**
     * Removes all registered buttons. Useful for menus whose buttons are rebuilt from data that loads after construction
     * (e.g. asynchronously); the menu clears, re-{@link #register registers}, and re-{@link #renderButtons renders}.
     */
    protected final void clearButtons() {
        buttons.clear();
    }

    /**
     * {@return the registered slot &rarr; button view, in insertion order} Subclasses use this to derive per-slot
     * metadata (e.g. required permissions) from the same registry that drives rendering and clicks.
     */
    protected final Map<Integer, B> buttons() {
        return buttons;
    }

    /**
     * Renders every registered button into this menu's inventory. Subclasses typically call this from
     * {@link #populate(Player)} after filling any background items.
     *
     * @param player The viewing player
     */
    protected final void renderButtons(Player player) {
        Inventory inventory = getInventory();
        buttons.forEach((slot, button) -> button.render(player, inventory, slot));
    }

    /**
     * Cancels the click (menus never let players take their items) and routes it to the registered button at the clicked
     * slot, or to {@link #onUnhandledClick} when the slot has no button. Subclasses customise behaviour through
     * {@link #onUnhandledClick} (e.g. a "back" filler) rather than overriding this method.
     *
     * @param event The click event
     */
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        B button = buttonAt(event);
        if (button != null) {
            button.onClick(player, event);
        } else {
            onUnhandledClick(player, event);
        }
    }

    /**
     * Hook for a click on a slot with no registered button — a decorative filler, an empty slot, or the player's own
     * inventory. The default does nothing; menus override it to, for example, treat a click on the border as "go back".
     *
     * @param player The clicking player
     * @param event The click event (already cancelled)
     */
    protected void onUnhandledClick(Player player, InventoryClickEvent event) {}

    /**
     * {@return the button at the clicked slot, or {@code null} if the click landed outside this menu's inventory (e.g.
     * the player's own inventory) or on an unregistered slot} Uses the {@link InventoryClickEvent#getRawSlot() raw slot}
     * bounded to this menu's size, so a click in the player's inventory can never collide with a top-inventory button.
     *
     * @param event The click event
     */
    protected final @Nullable B buttonAt(InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= getInventory().getSize()) {
            return null;
        }
        return buttons.get(rawSlot);
    }
}
