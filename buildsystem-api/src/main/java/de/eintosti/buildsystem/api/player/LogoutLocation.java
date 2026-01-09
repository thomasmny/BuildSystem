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
package de.eintosti.buildsystem.api.player;

import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a player's logout location, which includes the world name and the location coordinates.
 *
 * @since 3.0.0
 */
@Internal
@NullMarked
public interface LogoutLocation {

    /**
     * Gets the name of the world the player logged out from.
     *
     * @return The world name
     */
    String worldName();

    /**
     * Gets the exact {@link Location} the player logged out from.
     *
     * @return The logout location
     */
    @Nullable
    Location location();
}