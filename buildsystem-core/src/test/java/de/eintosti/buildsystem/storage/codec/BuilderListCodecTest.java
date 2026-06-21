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

import de.eintosti.buildsystem.api.world.builder.Builder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuilderListCodecTest {

    private static final UUID ALICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void format_joinsEntriesWithSemicolon() {
        String encoded = BuilderListCodec.format(List.of(Builder.of(ALICE_ID, "Alice"), Builder.of(BOB_ID, "Bob")));
        assertEquals(ALICE_ID + ",Alice;" + BOB_ID + ",Bob", encoded);
    }

    @Test
    void format_emptyCollection_returnsEmptyString() {
        assertEquals("", BuilderListCodec.format(List.of()));
    }

    @Test
    void roundTrip_preservesOrderAndIdentity() {
        List<Builder> original = List.of(Builder.of(ALICE_ID, "Alice"), Builder.of(BOB_ID, "Bob"));

        List<Builder> parsed = BuilderListCodec.parse(BuilderListCodec.format(original));

        assertEquals(2, parsed.size());
        assertEquals(ALICE_ID, parsed.get(0).getUniqueId());
        assertEquals("Alice", parsed.get(0).getName());
        assertEquals(BOB_ID, parsed.get(1).getUniqueId());
        assertEquals("Bob", parsed.get(1).getName());
    }

    @Test
    void parse_null_returnsEmptyList() {
        assertTrue(BuilderListCodec.parse(null).isEmpty());
    }

    @Test
    void parse_empty_returnsEmptyList() {
        assertTrue(BuilderListCodec.parse("").isEmpty());
    }

    @Test
    void parse_skipsMalformedEntry_keepsValidOnes() {
        String raw = ALICE_ID + ",Alice;not-a-valid-builder;" + BOB_ID + ",Bob";

        List<Builder> parsed = BuilderListCodec.parse(raw);

        assertEquals(2, parsed.size());
        assertEquals("Alice", parsed.get(0).getName());
        assertEquals("Bob", parsed.get(1).getName());
    }

    @Test
    void parse_returnsMutableList() {
        List<Builder> parsed = BuilderListCodec.parse(ALICE_ID + ",Alice");
        assertDoesNotThrow(() -> parsed.add(Builder.of(BOB_ID, "Bob")));
    }
}
