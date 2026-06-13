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

import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class WorldDataAccessorTest {

    private WorldDataImpl newWorldData() {
        return new WorldDataBuilder("test-world").build();
    }

    @Test
    void flatSetter_isReflectedByFlatGetterAndProperty() {
        WorldDataImpl data = newWorldData();

        data.setStatus(BuildWorldStatus.FINISHED);

        assertEquals(BuildWorldStatus.FINISHED, data.getStatus());
        assertEquals(BuildWorldStatus.FINISHED, data.status().get());
    }

    @Test
    void propertySetter_isReflectedByFlatGetter() {
        WorldDataImpl data = newWorldData();

        data.status().set(BuildWorldStatus.ARCHIVE);

        assertEquals(BuildWorldStatus.ARCHIVE, data.getStatus());
        assertEquals(BuildWorldStatus.ARCHIVE, data.status().get());
    }

    @Test
    void booleanFlatSetter_isReflectedByPropertyAndFlatGetter() {
        WorldDataImpl data = newWorldData();

        data.setPhysics(false);

        assertEquals(false, data.isPhysics());
        assertEquals(false, data.physics().get());
    }
}
