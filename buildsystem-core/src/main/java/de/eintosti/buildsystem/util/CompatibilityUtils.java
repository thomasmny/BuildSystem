/*
 * Copyright (c) 2018-2024, Thomas Meaney
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
package de.eintosti.buildsystem.util;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class CompatibilityUtils {

    private CompatibilityUtils() {
    }

    /**
     * Gets the title of the {@link Inventory} associated with the given {@link InventoryEvent}.
     * <p>
     * This is needed, since in API versions 1.20.6 and earlier, {@link InventoryView} is a class and in versions 1.21
     * and later, it is an interface.
     *
     * @param event The generic InventoryEvent with an InventoryView to inspect
     * @return The title of the inventory
     */
    public static String getInventoryTitle(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTitle = view.getClass().getMethod("getTitle");
            getTitle.setAccessible(true);
            return (String) getTitle.invoke(view);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the top {@link Inventory} associated with the given {@link InventoryEvent}.
     * <p>
     * This is needed, since in API versions 1.20.6 and earlier, {@link InventoryView} is a class and in versions 1.21
     * and later, it is an interface.
     *
     * @param event The generic InventoryEvent with an InventoryView to inspect
     * @return The top inventory
     * @author <a href="https://www.spigotmc.org/threads/inventoryview-changed-to-interface-backwards-compatibility.651754/#post-4747875">Rumsfield</a>
     */
    public static Inventory getTopInventory(InventoryEvent event) {
        try {
            Object view = event.getView();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            return (Inventory) getTopInventory.invoke(view);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the top {@link Inventory} for a given {@link Player}.
     * <p>
     * This is needed, since in API versions 1.20.6 and earlier, {@link InventoryView} is a class and in versions 1.21
     * and later, it is an interface.
     *
     * @param player The player whose inventory is to be found
     * @return The top inventory
     */
    public static Inventory getTopInventory(Player player) {
        try {
            Object view = player.getOpenInventory();
            Method getTopInventory = view.getClass().getMethod("getTopInventory");
            getTopInventory.setAccessible(true);
            return (Inventory) getTopInventory.invoke(view);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
