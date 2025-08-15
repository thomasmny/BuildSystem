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

import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NullMarked;

/**
 * Manages active inventories and dispatches inventory-related events to their respective {@link InventoryHandler}s.
 * <p>
 * This class acts as a central point for handling custom inventory logic within the plugin.
 */
@NullMarked
public class InventoryManager {

    public final Map<Inventory, InventoryHandler> activeInventories;

    /**
     * Constructs a new {@link InventoryManager} instance.
     */
    public InventoryManager() {
        this.activeInventories = new HashMap<>();
    }

    /**
     * Registers an {@link InventoryHandler} for a specific {@link Inventory}. When events occur for the given inventory, the registered handler will be notified.
     *
     * @param inventory The inventory to register the handler for
     * @param handler   The handler to associate with the inventory
     */
    public void registerInventoryHandler(Inventory inventory, InventoryHandler handler) {
        this.activeInventories.put(inventory, handler);
    }

    /**
     * Unregisters the {@link InventoryHandler} associated with a specific {@link Inventory}. Events for this inventory will no longer be dispatched to its handler after
     * unregistration.
     *
     * @param inventory The inventory whose handler is to be unregistered
     */
    public void unregisterInventoryHandler(Inventory inventory) {
        this.activeInventories.remove(inventory);
    }

    /**
     * Handles an {@link InventoryOpenEvent}, dispatching it to the appropriate {@link InventoryHandler} if one is registered for the opened inventory.
     *
     * @param event The open event to handle
     */
    public void handleOpen(InventoryOpenEvent event) {
        InventoryHandler handler = this.activeInventories.get(event.getInventory());
        if (handler != null) {
            handler.onOpen(event);
        }
    }

    /**
     * Handles an {@link InventoryClickEvent}, dispatching it to the appropriate {@link InventoryHandler} if one is registered for the clicked inventory.
     *
     * @param event The click event to handle
     */
    public void handleClick(InventoryClickEvent event) {
        InventoryHandler handler = this.activeInventories.get(event.getInventory());
        if (handler != null) {
            handler.onClick(event);
        }
    }

    /**
     * Handles an {@link InventoryCloseEvent}, dispatching it to the appropriate {@link InventoryHandler} if one is registered for the closed inventory. The handler for the closed
     * inventory is also automatically unregistered upon closure.
     *
     * @param event The close event to handle
     */
    public void handleClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHandler handler = this.activeInventories.remove(inventory);
        if (handler != null) {
            handler.onClose(event);
            unregisterInventoryHandler(inventory);
        }
    }
}
