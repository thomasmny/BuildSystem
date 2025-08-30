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
package de.eintosti.buildsystem.util.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NullMarked;

/**
 * Holder for custom Bukkit inventories used within the BuildSystem plugin. This class implements {@link InventoryHolder} to allow for custom inventory handling.
 */
@NullMarked
public class BuildSystemHolder implements InventoryHolder {

    private final Inventory bukkitInventory;

    /**
     * Initializes a new {@link BuildSystemHolder} with the specified size and title.
     *
     * @param size  The size of the inventory.
     * @param title The title of the inventory.
     */
    public BuildSystemHolder(int size, String title) {
        this.bukkitInventory = Bukkit.createInventory(this, size, title);
    }

    /**
     * Returns the custom inventory held by this holder.
     *
     * @return The {@link Inventory} instance.
     */
    @Override
    public Inventory getInventory() {
        return this.bukkitInventory;
    }
}
