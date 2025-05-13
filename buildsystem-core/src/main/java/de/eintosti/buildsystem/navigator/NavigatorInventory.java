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
package de.eintosti.buildsystem.navigator;

import de.eintosti.buildsystem.BuildSystemPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public abstract class NavigatorInventory {

    protected final BuildSystemPlugin plugin;

    protected NavigatorInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    protected abstract Inventory createInventory(Player player);

    public abstract void handleClick(InventoryClickEvent event);

    public void openInventory(Player player) {
        player.openInventory(createInventory(player));
    }
} 