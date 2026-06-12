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

import de.eintosti.buildsystem.i18n.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class Menu implements InventoryHolder {

    protected final Messages messages;
    private final Inventory inventory;

    protected Menu(Messages messages, int size, String title) {
        this.messages = messages;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    // package-private: only for unit tests that cannot run a Bukkit server
    Menu(Messages messages, Inventory inventory) {
        this.messages = messages;
        this.inventory = inventory;
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    protected abstract void populate(Player player);

    public void open(Player player) {
        populate(player);
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
    }

    public void handleClose(InventoryCloseEvent event) {
    }

    public void handleOpen(InventoryOpenEvent event) {
    }
}
