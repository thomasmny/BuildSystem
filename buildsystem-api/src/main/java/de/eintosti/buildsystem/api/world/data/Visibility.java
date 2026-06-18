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
package de.eintosti.buildsystem.api.world.data;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.jspecify.annotations.NullMarked;

/**
 * Determines who may see and enter a {@link BuildWorld}. Replaces the legacy public/private boolean: a world is either
 * open to {@link #EVERYONE} or restricted to its {@link #ADDED_PLAYERS}.
 *
 * @since 3.0.0
 */
@NullMarked
public enum Visibility {

    /**
     * The world is open: any player may see and enter it, subject to its own access permission.
     */
    EVERYONE("public"),

    /**
     * The world is restricted: only its creator and the builders added to it (plus players able to bypass the view
     * permission) may see and enter it.
     */
    ADDED_PLAYERS("private");

    private final String permissionNode;

    Visibility(String permissionNode) {
        this.permissionNode = permissionNode;
    }

    /**
     * Gets the stable permission segment for this visibility, used in the world-creation permissions
     * ({@code buildsystem.create.<node>} and {@code buildsystem.create.<node>.<limit>}). Kept as the familiar
     * {@code public}/{@code private} wording rather than the enum name so those permissions are unchanged from prior
     * versions.
     *
     * @return The permission node ({@code "public"} or {@code "private"})
     */
    public String getPermissionNode() {
        return permissionNode;
    }

    /**
     * Returns the visibility matching the legacy private flag.
     *
     * @param isPrivateWorld Whether the world is private
     * @return {@link #ADDED_PLAYERS} if private, otherwise {@link #EVERYONE}
     */
    public static Visibility matchVisibility(boolean isPrivateWorld) {
        return isPrivateWorld ? ADDED_PLAYERS : EVERYONE;
    }

    /**
     * Returns whether this visibility corresponds to a private (added-players-only) world.
     *
     * @return {@code true} if {@link #ADDED_PLAYERS}, otherwise {@code false}
     */
    public boolean isPrivate() {
        return this == ADDED_PLAYERS;
    }
}
