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
package de.eintosti.buildsystem.api.world.util;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Provides utilities for teleporting {@link Player}s to specific locations within a {@link BuildWorld}. This interface ensures safe and controlled player movement.
 *
 * @since 3.0.0
 */
public interface WorldTeleporter {

    /**
     * Teleports the given {@link Player} to the designated spawn location of the world associated with this teleporter. If a custom spawn is not set, the player will be teleported
     * to the world's default spawn.
     *
     * @param player The {@link Player} to teleport
     */
    void teleport(Player player);

    /**
     * Checks if a given {@link Location} is considered safe for teleportation. A safe location typically means there are no immediate hazards (e.g., lava, void) at the target
     * coordinates.
     *
     * @param location The {@link Location} to check
     * @return {@code true} if the location is safe, {@code false} otherwise
     */
    boolean isSafeLocation(Location location);
}
