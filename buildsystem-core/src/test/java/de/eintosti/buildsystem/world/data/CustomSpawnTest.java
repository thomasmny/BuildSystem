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
package de.eintosti.buildsystem.world.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class CustomSpawnTest {

    @Test
    void formatThenParse_roundTrips() {
        Location original = new Location(null, 1.5, 64.0, -3.25, 90.0f, -45.0f);

        Location parsed = CustomSpawn.parse(null, CustomSpawn.format(original));

        assertNotNull(parsed);
        assertEquals(original.getX(), parsed.getX());
        assertEquals(original.getY(), parsed.getY());
        assertEquals(original.getZ(), parsed.getZ());
        assertEquals(original.getYaw(), parsed.getYaw());
        assertEquals(original.getPitch(), parsed.getPitch());
    }

    @Test
    void parse_blank_returnsNull() {
        assertNull(CustomSpawn.parse(null, ""));
        assertNull(CustomSpawn.parse(null, "   "));
    }

    @Test
    void parse_wrongPartCount_returnsNull() {
        assertNull(CustomSpawn.parse(null, "1.0;2.0;3.0"));
    }

    @Test
    void parse_nonNumeric_returnsNull() {
        assertNull(CustomSpawn.parse(null, "1.0;abc;3.0;0.0;0.0"));
    }
}
