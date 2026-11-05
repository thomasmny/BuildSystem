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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.api.world.builder.Builder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.MemoryConfiguration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class BuilderCodecTest {

    private static final UUID ALICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String KEY = "builders";

    private static MemoryConfiguration section(Object stored) {
        MemoryConfiguration section = new MemoryConfiguration();
        if (stored instanceof Map<?, ?> fields) {
            // As every stored entry is loaded.
            section.createSection(KEY, fields);
        } else {
            section.set(KEY, stored);
        }
        return section;
    }

    @Test
    void serialize_writesFields() {
        assertEquals(
                List.of(
                        Map.of("uuid", ALICE_ID.toString(), "name", "Alice"),
                        Map.of("uuid", BOB_ID.toString(), "name", "Bob")),
                BuilderCodec.serialize(List.of(Builder.of(ALICE_ID, "Alice"), Builder.of(BOB_ID, "Bob"))));
    }

    @Test
    void list_roundTrips_inOrder() {
        List<Builder> parsed = BuilderCodec.parseList(
                section(BuilderCodec.serialize(List.of(Builder.of(ALICE_ID, "Alice"), Builder.of(BOB_ID, "Bob")))),
                KEY);

        assertEquals(2, parsed.size());
        assertEquals(ALICE_ID, parsed.get(0).getUniqueId());
        assertEquals("Alice", parsed.get(0).getName());
        assertEquals(BOB_ID, parsed.get(1).getUniqueId());
        assertEquals("Bob", parsed.get(1).getName());
    }

    @Test
    void list_missingOrEmpty_isEmpty() {
        assertTrue(BuilderCodec.parseList(new MemoryConfiguration(), KEY).isEmpty());
        assertTrue(BuilderCodec.parseList(section(List.of()), KEY).isEmpty());
        assertTrue(BuilderCodec.parseList(section(""), KEY).isEmpty());
    }

    @Test
    void list_skipsAnIncompleteEntry_keepsTheOthers() {
        List<Builder> parsed = BuilderCodec.parseList(
                section(List.of(
                        Map.of("uuid", ALICE_ID.toString(), "name", "Alice"),
                        Map.of("name", "No uuid"),
                        Map.of("uuid", BOB_ID.toString(), "name", "Bob"))),
                KEY);

        assertEquals(
                List.of("Alice", "Bob"), parsed.stream().map(Builder::getName).toList());
    }

    @Test
    void list_isMutable() {
        List<Builder> parsed = BuilderCodec.parseList(section(ALICE_ID + ",Alice"), KEY);

        assertDoesNotThrow(() -> parsed.add(Builder.of(BOB_ID, "Bob")));
    }

    @Test
    void list_readsTheStringFromBefore41_skippingAMalformedEntry() {
        List<Builder> parsed =
                BuilderCodec.parseList(section(ALICE_ID + ",Alice;not-a-valid-builder;" + BOB_ID + ",Bob"), KEY);

        assertEquals(
                List.of("Alice", "Bob"), parsed.stream().map(Builder::getName).toList());
    }

    @Test
    void single_readsFieldsAndTheStringFromBefore41() {
        Builder fromFields = BuilderCodec.parse(section(BuilderCodec.serialize(Builder.of(ALICE_ID, "Alice"))), KEY);
        Builder fromString = BuilderCodec.parse(section(ALICE_ID + ",Alice"), KEY);

        assertEquals(ALICE_ID, fromFields.getUniqueId());
        assertEquals("Alice", fromFields.getName());
        assertEquals(ALICE_ID, fromString.getUniqueId());
        assertEquals("Alice", fromString.getName());
    }

    @Test
    void single_incomplete_isNull_andABadUuidFails() {
        assertNull(BuilderCodec.parse(section(Map.of("name", "No uuid")), KEY));
        assertThrows(
                IllegalArgumentException.class,
                () -> BuilderCodec.parse(section(Map.of("uuid", "nonsense", "name", "Alice")), KEY));
    }
}
