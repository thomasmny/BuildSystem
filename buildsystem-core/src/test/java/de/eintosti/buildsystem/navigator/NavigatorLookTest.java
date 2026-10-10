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
package de.eintosti.buildsystem.navigator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class NavigatorLookTest {

    // Yaw 0 looks along +Z.
    private final Location eye = new Location(null, 0, 0, 0, 0, 0);

    @Test
    void headStraightAhead_isLookedAt() {
        assertTrue(NavigatorService.isLookingAt(eye, new Location(null, 0, 0, 2)));
        assertTrue(NavigatorService.isLookingAt(eye, new Location(null, 0.25, -0.25, 2.9)));
    }

    @Test
    void headToTheSideOrOutOfReach_isNot() {
        assertFalse(NavigatorService.isLookingAt(eye, new Location(null, 0.5, 0, 2)));
        assertFalse(NavigatorService.isLookingAt(eye, new Location(null, 0, 0, 3.5)));
        assertFalse(NavigatorService.isLookingAt(eye, new Location(null, 0, 0, -2)));
    }
}
