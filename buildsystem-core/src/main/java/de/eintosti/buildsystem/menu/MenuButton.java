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

import java.util.function.Predicate;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
     * {@return the permission required to click this button, or {@code null} if it is unrestricted}
     *
     * <p>This is <em>enforced</em> through {@link #canClick} before {@link ButtonMenu#handleClick} dispatches to
     * {@link #onClick}, so a button that declares a permission never has to check it again. Denial is handled by
     * {@link ButtonMenu#onPermissionDenied}. For access a single node cannot express, see {@link #usableBy()}.
     */
    default @Nullable String permission() {
        return null;
    }

    /**
     * {@return a test the clicking player must pass, or {@code null} if there is none}
     *
     * <p>Exists because {@link #permission()} can only express a single node, while access is often scoped to the
     * resource the menu is acting on &mdash; whether the player created this world, whether they are under their world
     * limit. Both are inputs to {@link #canClick}; neither is enforced on its own.
     */
    default @Nullable Predicate<Player> usableBy() {
        return null;
    }

    /**
     * {@return whether the player is allowed to click this button}
     *
     * <p>The single place {@link #permission()} and {@link #usableBy()} are combined, and the only thing
     * {@link ButtonMenu#handleClick} consults &mdash; so declaring either one is enough to have it enforced, and a
     * button never re-checks access inside {@link #onClick}. Hand-rolled checks in a click handler are what let
     * authorization drift out of step with what was rendered.
     *
     * @param player The clicking player
     */
    default boolean canClick(Player player) {
        String permission = permission();
        if (permission != null && !player.hasPermission(permission)) {
            return false;
        }

        Predicate<Player> usableBy = usableBy();
        return usableBy == null || usableBy.test(player);
    }

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
        private @Nullable String permission;
        private @Nullable Predicate<Player> usableBy;

        private Builder() {}

        /**
         * Sets the permission required to click the button; {@code null} leaves it unrestricted. The menu enforces
         * this before dispatching the click, so the handler does not repeat the check.
         *
         * @param permission The required permission, or {@code null}
         * @return This builder
         */
        public Builder permission(@Nullable String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Restricts the button to players passing the given test, for access that a single permission node cannot
         * express because it depends on the resource being acted on. Combined with the permission by
         * {@link MenuButton#canClick(Player)}.
         *
         * @param usableBy The test, or {@code null} for no restriction
         * @return This builder
         */
        public Builder usableBy(@Nullable Predicate<Player> usableBy) {
            this.usableBy = usableBy;
            return this;
        }

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
            String builtPermission = permission;
            Predicate<Player> builtUsableBy = usableBy;

            return new MenuButton() {
                @Override
                public void render(Player player, Inventory inventory, int slot) {
                    builtRenderer.render(player, inventory, slot);
                }

                @Override
                public void onClick(Player player, InventoryClickEvent event) {
                    builtClickHandler.onClick(player, event);
                }

                @Override
                public @Nullable String permission() {
                    return builtPermission;
                }

                @Override
                public @Nullable Predicate<Player> usableBy() {
                    return builtUsableBy;
                }
            };
        }
    }
}
