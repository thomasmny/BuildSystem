/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.world.display;

import com.cryptomorin.xseries.XMaterial;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an item that can be displayed in the navigator inventory. This interface follows functional programming principles by defining pure functions for data transformation
 * and display.
 */
public interface Displayable {

    /**
     * Gets the unique name of this displayable item.
     *
     * @return The name
     */
    String getName();

    /**
     * Gets the material to display this item with.
     *
     * @return The material
     */
    XMaterial getMaterial();

    /**
     * Gets the display name shown in the inventory. This should be a pure function that transforms the name based on the player's context.
     *
     * @param player The player viewing the inventory
     * @return The display name
     */
    String getDisplayName(Player player);

    /**
     * Gets the lore shown in the inventory. This should be a pure function that generates lore based on the player's context.
     *
     * @param player The player viewing the inventory
     * @return The lore
     */
    List<String> getLore(Player player);

    /**
     * Converts this displayable to an ItemStack for display. This should be a pure function that creates a new ItemStack instance.
     *
     * @param player The player viewing the inventory
     * @return The ItemStack representation
     */
    ItemStack asItemStack(Player player);
} 