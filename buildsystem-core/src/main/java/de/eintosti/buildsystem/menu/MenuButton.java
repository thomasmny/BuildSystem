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

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

/**
 * A single slot in a {@link ButtonMenu}: it knows how to render its icon and how to react to a click. A menu keeps a
 * {@code Map<Integer, MenuButton>} so the slot &rarr; behavior contract is declared once per button instead of being
 * split across a {@code populate} call and a {@code handleClick} switch.
 *
 * <p>The owning slot is supplied to {@link #render} at render time, so a button does not need to capture its own slot;
 * the same button definition can be registered at any slot.
 */
@NullMarked
public interface MenuButton {

    /**
     * Renders this button's icon into the given inventory.
     *
     * @param player The viewing player
     * @param inventory The inventory to render into
     * @param slot The slot this button occupies
     */
    void render(Player player, Inventory inventory, int slot);

    /**
     * Handles a click on this button. The button is fully responsible for its own outcome, including re-opening the menu
     * when that is the intended behavior.
     *
     * @param player The clicking player
     * @param event The click event
     */
    void onClick(Player player, InventoryClickEvent event);

    /**
     * {@return a new {@link Builder} for assembling a {@code MenuButton} from a renderer and a click handler} Either part
     * may be omitted: an unset renderer draws nothing and an unset click handler does nothing.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Renders a button into the slot it occupies.
     */
    @FunctionalInterface
    interface Renderer {

        /**
         * Renders into the given slot.
         *
         * @param player The viewing player
         * @param inventory The inventory to render into
         * @param slot The slot this button occupies
         */
        void render(Player player, Inventory inventory, int slot);
    }

    /**
     * Reacts to a click on a button.
     */
    @FunctionalInterface
    interface ClickHandler {

        /**
         * Handles the click.
         *
         * @param player The clicking player
         * @param event The click event
         */
        void onClick(Player player, InventoryClickEvent event);
    }

    /**
     * Fluent builder for a plain {@link MenuButton}. Menus that attach their own per-slot metadata implement
     * {@code MenuButton} directly instead of using this builder.
     */
    final class Builder {

        private Renderer renderer = (player, inventory, slot) -> {};
        private ClickHandler clickHandler = (player, event) -> {};

        private Builder() {}

        /**
         * Sets how the button renders into its slot.
         *
         * @param renderer The renderer
         * @return This builder
         */
        public Builder render(Renderer renderer) {
            this.renderer = renderer;
            return this;
        }

        /**
         * Sets how the button reacts to a click.
         *
         * @param clickHandler The click handler
         * @return This builder
         */
        public Builder onClick(ClickHandler clickHandler) {
            this.clickHandler = clickHandler;
            return this;
        }

        /**
         * {@return the assembled {@link MenuButton}}
         */
        public MenuButton build() {
            Renderer builtRenderer = renderer;
            ClickHandler builtClickHandler = clickHandler;
            return new MenuButton() {
                @Override
                public void render(Player player, Inventory inventory, int slot) {
                    builtRenderer.render(player, inventory, slot);
                }

                @Override
                public void onClick(Player player, InventoryClickEvent event) {
                    builtClickHandler.onClick(player, event);
                }
            };
        }
    }
}
