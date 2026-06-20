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
package de.eintosti.buildsystem.world.display;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * The per-category access permission, shared by the navigator (which categories a player sees) and the {@code /worlds
 * <category>} shortcuts (which categories a player can open by command). The two are deliberately one and the same node,
 * so an admin who hides a category from the navigator also disables its shortcut, and vice versa.
 *
 * <p>The node is <strong>default-allow</strong>: like the navigator, which shows every category to everyone out of the
 * box, a player may access a category unless {@code buildsystem.navigator.<id>} has been explicitly set to {@code false}.
 * It is consulted through {@link Player#isPermissionSet(String)} rather than registered with a default, because category
 * ids are dynamic and cannot be declared up front in {@code plugin.yml}.
 */
@NullMarked
public final class CategoryPermissions {

    private CategoryPermissions() {}

    /**
     * {@return the permission node guarding access to the category with the given id}
     *
     * @param categoryId The category id
     */
    public static String node(String categoryId) {
        return "buildsystem.navigator." + categoryId;
    }

    /**
     * {@return whether the player may see and open the category with the given id} Defaults to {@code true}; only an
     * explicit {@code false} on {@link #node(String)} denies access.
     *
     * @param player The player
     * @param categoryId The category id
     */
    public static boolean canAccess(Player player, String categoryId) {
        String node = node(categoryId);
        return !player.isPermissionSet(node) || player.hasPermission(node);
    }
}
