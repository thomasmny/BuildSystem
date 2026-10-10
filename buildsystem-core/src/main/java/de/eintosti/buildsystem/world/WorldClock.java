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
package de.eintosti.buildsystem.world;

import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

/**
 * Changes the time of day in worlds that may not have one. Since the 26.x world clocks, a dimension without a default
 * clock, like the nether, rejects a time change.
 */
@NullMarked
public final class WorldClock {

    private WorldClock() {}

    /**
     * Sets the time of day, unless the world has no default clock. Spigot's API has no way to ask whether a world has
     * one, and Paper's {@code isFixedTime} reads a different property ({@code has_fixed_time}, not
     * {@code default_clock}), so the change is tried and the {@link IllegalArgumentException} the server throws for
     * such a world is caught.
     *
     * @return Whether the time was set
     */
    public static boolean trySetTime(World world, long time) {
        try {
            world.setTime(time);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
