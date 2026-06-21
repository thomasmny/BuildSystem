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
package de.eintosti.buildsystem.storage.codec;

import static org.junit.jupiter.api.Assertions.*;

import de.eintosti.buildsystem.player.LogoutLocation;
import org.junit.jupiter.api.Test;

class LogoutLocationCodecTest {

    @Test
    void format_matchesCanonicalString() {
        LogoutLocation location = new LogoutLocation("world", 1.5, 64.0, -2.5, 90.0f, 45.0f);
        assertEquals("world:1.5:64.0:-2.5:90.0:45.0", LogoutLocationCodec.format(location));
    }

    @Test
    void roundTrip_preservesAllComponents() {
        LogoutLocation original = new LogoutLocation("nether", 12.25, 7.0, -88.75, 180.0f, -12.5f);

        LogoutLocation parsed = LogoutLocationCodec.parse(LogoutLocationCodec.format(original));

        assertNotNull(parsed);
        assertEquals("nether", parsed.worldName());
        assertEquals(original.toString(), parsed.toString());
    }

    @Test
    void parse_null_returnsNull() {
        assertNull(LogoutLocationCodec.parse(null));
    }

    @Test
    void parse_blank_returnsNull() {
        assertNull(LogoutLocationCodec.parse("   "));
    }

    @Test
    void parse_wrongPartCount_returnsNull() {
        assertNull(LogoutLocationCodec.parse("world:1.0:2.0:3.0"));
    }

    @Test
    void parse_nonNumericComponent_returnsNull() {
        assertNull(LogoutLocationCodec.parse("world:1.0:NaNsense:3.0:0.0:0.0"));
    }
}
