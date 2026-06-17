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
package de.eintosti.buildsystem.api.world.display;

import com.cryptomorin.xseries.XMaterial;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;

/**
 * Represents an object that can be displayed in an inventory.
 *
 * @since 3.0.0
 */
@NullMarked
public interface Displayable {

    /**
     * Gets the unique name of this displayable item.
     *
     * @return The name
     */
    String getName();

    /**
     * Sets the name of this displayable item.
     *
     * <p><b>Note:</b> this only updates the in-memory name. Persisted storage is keyed by the name, so a durable rename
     * additionally requires re-saving under the new key (delete the old storage entry, then save under the new name).
     *
     * @param name The new name for this item
     * @throws IllegalArgumentException if the name is {@code null} or blank
     */
    void setName(String name);

    /**
     * Gets the name used to display this item in an inventory.
     *
     * @param player The player viewing the item
     * @return The display name
     */
    String getDisplayName(Player player);

    /**
     * Gets the creation timestamp of the displayable.
     *
     * @return The number of milliseconds that have passed since {@code January 1, 1970 UTC}, until the displayable was
     *     created.
     */
    long getCreation();

    /**
     * Gets the material to display this item with.
     *
     * @return The material
     */
    XMaterial getIcon();

    /**
     * Sets the icon for this displayable item.
     *
     * @param material The material to set as the icon
     */
    void setIcon(XMaterial material);

    /**
     * Gets the lore of this displayable item.
     *
     * @param player The player viewing the item
     * @return The lore
     */
    List<String> getLore(Player player);

    /**
     * Converts this displayable to an {@link ItemStack} for display.
     *
     * @param player The player viewing the inventory
     * @return The ItemStack representation
     */
    default ItemStack asItemStack(Player player) {
        ItemStack itemStack = getIcon().parseItem();
        if (itemStack == null) {
            throw new IllegalStateException("Icon material %s could not be parsed into an ItemStack.".formatted(getIcon()));
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            throw new IllegalStateException("ItemMeta for %s is null. This should not happen.".formatted(getIcon()));
        }

        itemMeta.setDisplayName(getDisplayName(player));
        itemMeta.setLore(getLore(player));
        itemMeta.addItemFlags(ItemFlag.values());

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    /**
     * Adds this displayable to an {@link Inventory} at the given slot.
     *
     * @param inventory The inventory to add the item to
     * @param slot The slot in the inventory to add the item
     * @param player The player viewing the inventory
     */
    default void addToInventory(Inventory inventory, int slot, Player player) {
        inventory.setItem(slot, asItemStack(player));
    }
}
