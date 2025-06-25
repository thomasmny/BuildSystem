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

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Abstract base class for custom inventory logic within the BuildSystem plugin.
 */
public abstract class BuildSystemInventory {

    /**
     * Handles an {@link InventoryClickEvent} for this custom inventory.
     *
     * @param event The {@link InventoryClickEvent} to handle
     */
    public void onClick(InventoryClickEvent event) {
    }

    /**
     * Handles an {@link InventoryCloseEvent} for this custom inventory.
     *
     * @param event The {@link InventoryCloseEvent} to handle
     */
    public void onClose(InventoryCloseEvent event) {
    }
}
