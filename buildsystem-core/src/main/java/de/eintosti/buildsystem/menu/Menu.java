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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.i18n.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NullMarked;

/**
 * Base class for every chest-style GUI in the plugin.
 *
 * <p>A {@code Menu} <em>is</em> its inventory's {@link InventoryHolder}: the backing {@link Inventory} is created with
 * {@code this} as the holder, so the shared {@code MenuListener} can recover the owning menu from any inventory event
 * via {@code event.getInventory().getHolder() instanceof Menu} and route the event back to the corresponding
 * {@link #handleClick}/{@link #handleOpen}/{@link #handleClose} hook. Subclasses therefore never register their own
 * listeners; they only override the hooks they care about.
 *
 * <p><strong>Lifecycle.</strong> Construction builds the (empty) inventory. {@link #open(Player)} then
 * {@link #populate(Player) populates} it and shows it to the player. Re-opening a menu after a state change is done by
 * constructing a fresh instance and calling {@link #open(Player)} again (menus are cheap and per-open); subclasses do
 * not mutate a live inventory in place beyond what {@code populate} writes.
 *
 * <p><strong>Threading.</strong> Inventory creation and display must happen on the server main thread, so menus are
 * main-thread objects.
 *
 * @see PaginatedMenu for the multi-page variant
 * @see MenuButton for the per-slot render/click abstraction used by heterogeneous menus
 */
@NullMarked
public abstract class Menu implements InventoryHolder {

    protected final Messages messages;
    private final Inventory inventory;

    /**
     * Creates the menu and its backing inventory, with this menu as the inventory holder.
     *
     * @param messages The message provider used for permission errors and (by subclasses) item text
     * @param size The inventory size in slots (a multiple of 9)
     * @param title The inventory title shown to the player
     */
    protected Menu(Messages messages, int size, String title) {
        this.messages = messages;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    /**
     * {@return the backing inventory} This is the holder linkage the event listener relies on; it is {@code final} so a
     * menu and its inventory always identify each other.
     */
    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    /**
     * Fills the inventory with this menu's items for the given viewer. Called by {@link #open(Player)} immediately
     * before the inventory is shown. Implementations render into {@link #getInventory()}.
     *
     * @param player The player the menu is being built for (drives permission-dependent and per-player content)
     */
    protected abstract void populate(Player player);

    /**
     * Populates the menu for the player and opens it.
     *
     * @param player The player to show the menu to
     */
    public void open(Player player) {
        populate(player);
        player.openInventory(inventory);
    }

    /**
     * Shared click-time permission guard: if the player lacks the permission, closes the menu, sends the permission
     * error, and plays the deny sound.
     *
     * <p>Note that this <strong>closes the inventory</strong> on denial. Menus that must keep the inventory open on a
     * denied click (e.g. per-toggle settings) deliberately do their own inline check instead of calling this.
     *
     * @param player The clicking player
     * @param permission The required permission
     * @return {@code true} if the player may proceed, {@code false} if denied (UX already handled)
     */
    protected boolean requirePermission(Player player, String permission) {
        if (player.hasPermission(permission)) {
            return true;
        }
        player.closeInventory();
        messages.sendPermissionError(player);
        XSound.ENTITY_ITEM_BREAK.play(player);
        return false;
    }

    /**
     * Handles a click in this menu's inventory. Routed here by the shared menu listener. The base implementation does
     * nothing; interactive menus override it (typically cancelling the event first, then dispatching by slot).
     *
     * @param event The click event
     */
    public void handleClick(InventoryClickEvent event) {}

    /**
     * Handles this menu's inventory being closed. No-op by default; override for cleanup or follow-up actions.
     *
     * @param event The close event
     */
    public void handleClose(InventoryCloseEvent event) {}

    /**
     * Handles this menu's inventory being opened. No-op by default; override to react to the open.
     *
     * @param event The open event
     */
    public void handleOpen(InventoryOpenEvent event) {}
}
