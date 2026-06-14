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
 * A single slot in a heterogeneous menu: it knows how to render its icon and how to react to a click. Menus that map
 * each slot to its own behavior keep a {@code Map<Integer, MenuButton>} so the slot &rarr; behavior contract is declared
 * once per button instead of split across a {@code populate} call and a {@code handleClick} switch branch.
 */
@NullMarked
public interface MenuButton {

    /**
     * Renders this button's icon into the given inventory.
     *
     * @param player The viewing player
     * @param inventory The inventory to render into
     */
    void render(Player player, Inventory inventory);

    /**
     * Handles a click on this button. The button is fully responsible for its own outcome, including re-opening the menu
     * when that is the intended behavior.
     *
     * @param player The clicking player
     * @param event The click event
     */
    void onClick(Player player, InventoryClickEvent event);
}
