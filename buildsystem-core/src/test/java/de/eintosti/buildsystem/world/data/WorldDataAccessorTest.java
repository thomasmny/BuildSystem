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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.test.TestData;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class WorldDataAccessorTest {

    private static WorldDataImpl worldData() {
        return new WorldDataImpl.WorldDataBuilder("test")
                .withStatus(TestData.NOT_STARTED)
                .build();
    }

    @Test
    void valueAccessor_roundTrips() {
        WorldDataImpl data = worldData();

        data.set(WorldDataKey.STATUS, TestData.FINISHED);
        assertEquals(TestData.FINISHED, data.get(WorldDataKey.STATUS));

        data.set(WorldDataKey.STATUS, TestData.ARCHIVE_STATUS);
        assertEquals(TestData.ARCHIVE_STATUS, data.get(WorldDataKey.STATUS));
    }

    @Test
    void booleanAccessor_roundTrips() {
        WorldDataImpl data = worldData();

        data.set(WorldDataKey.PHYSICS, false);
        assertFalse(data.get(WorldDataKey.PHYSICS));

        data.set(WorldDataKey.PHYSICS, true);
        assertTrue(data.get(WorldDataKey.PHYSICS));
    }

    @Test
    void get_returnsTheKeyTypedValue() {
        WorldDataImpl data = worldData();

        data.set(WorldDataKey.PERMISSION, "buildsystem.test");
        // The key's type parameter carries through, so no cast is needed at the call site.
        String permission = data.get(WorldDataKey.PERMISSION);
        assertEquals("buildsystem.test", permission);
    }

    @Test
    void unknownKey_isRejected() {
        WorldDataImpl data = worldData();
        WorldDataKey<String> unknown = WorldDataKey.of("not-a-real-key", String.class);
        assertThrows(IllegalArgumentException.class, () -> data.get(unknown));
    }

    @Test
    void getAllData_isUnmodifiable() {
        WorldDataImpl data = worldData();
        assertThrows(
                UnsupportedOperationException.class, () -> data.getAllData().clear());
    }
}
