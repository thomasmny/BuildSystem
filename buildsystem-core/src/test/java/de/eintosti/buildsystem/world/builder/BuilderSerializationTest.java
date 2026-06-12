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
package de.eintosti.buildsystem.world.builder;

import de.eintosti.buildsystem.api.world.builder.Builder;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class BuilderSerializationTest {

    private static final UUID UUID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UUID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void roundTrip_serializeDeserialize() {
        Builder original = Builder.of(UUID_A, "Alice");
        Builder restored = Builder.deserialize(original.toString());
        assertNotNull(restored);
        assertEquals(original.getUniqueId(), restored.getUniqueId());
        assertEquals(original.getName(), restored.getName());
    }

    @Test
    void deserialize_null_returnsNull() {
        assertNull(Builder.deserialize(null));
    }

    @Test
    void deserialize_dash_returnsNull() {
        assertNull(Builder.deserialize("-"));
    }

    @Test
    void deserialize_malformed_returnsNull() {
        assertNull(Builder.deserialize("notauuid,foo,extra"));
        assertNull(Builder.deserialize("notauuid"));
    }

    @Test
    void equals_sameUuidAndName_areEqual() {
        Builder a = Builder.of(UUID_A, "Alice");
        Builder b = Builder.of(UUID_A, "Alice");
        assertEquals(a.getUniqueId(), b.getUniqueId());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.toString(), b.toString());
    }

    @Test
    void setName_updatesName() {
        Builder builder = Builder.of(UUID_A, "OldName");
        builder.setName("NewName");
        assertEquals("NewName", builder.getName());
    }

    @Test
    void twoBuilders_differentUuids_differentToString() {
        Builder a = Builder.of(UUID_A, "Alice");
        Builder b = Builder.of(UUID_B, "Alice");
        assertNotEquals(a.toString(), b.toString());
    }
}
