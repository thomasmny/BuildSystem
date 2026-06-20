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

        data.setStatus(TestData.FINISHED);
        assertEquals(TestData.FINISHED, data.getStatus());

        data.setStatus(TestData.ARCHIVE_STATUS);
        assertEquals(TestData.ARCHIVE_STATUS, data.getStatus());
    }

    @Test
    void booleanAccessor_roundTrips() {
        WorldDataImpl data = worldData();

        data.setPhysics(false);
        assertFalse(data.isPhysics());

        data.setPhysics(true);
        assertTrue(data.isPhysics());
    }

    @Test
    void getAllData_isUnmodifiable() {
        WorldDataImpl data = worldData();
        assertThrows(
                UnsupportedOperationException.class, () -> data.getAllData().clear());
    }
}
