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
import org.jetbrains.annotations.NotNull;

/**
 * Holder for custom Bukkit inventories used within the BuildSystem plugin. This class implements {@link InventoryHolder} to allow for custom inventory handling.
 */
public class BuildSystemHolder implements InventoryHolder {

    private final BuildSystemInventory buildSystemInventory;
    private final Inventory bukkitInventory;

    /**
     * Initializes a new {@link BuildSystemHolder} with the specified size and title.
     *
     * @param buildSystemInventory The custom inventory logic associated with this holder.
     * @param size                 The size of the inventory.
     * @param title                The title of the inventory.
     */
    public BuildSystemHolder(@NotNull BuildSystemInventory buildSystemInventory, int size, @NotNull String title) {
        this.buildSystemInventory = buildSystemInventory;
        this.bukkitInventory = Bukkit.createInventory(this, size, title);
    }

    /**
     * Returns the custom inventory held by this holder.
     *
     * @return The {@link Inventory} instance.
     */
    @Override
    public @NotNull Inventory getInventory() {
        return this.bukkitInventory;
    }

    /**
     * Returns the custom inventory logic associated with this holder.
     *
     * @return The {@link BuildSystemInventory} instance.
     */
    public @NotNull BuildSystemInventory getBuildSystemInventory() {
        return this.buildSystemInventory;
    }
}
