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
package de.eintosti.buildsystem.world.menu;

import de.eintosti.buildsystem.world.menu.EditMenu.TimeOfDay;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the tick → {@link TimeOfDay} bucketing used by the editor's time button. The boundaries are
 * inclusive-low/exclusive-high: {@code [0, noon)} sunrise, {@code [noon, 13000)} noon, {@code [13000, ..)} night.
 */
@NullMarked
class TimeOfDayTest {

    private static final int NOON = 6000;

    @Test
    void startOfDayIsSunrise() {
        assertEquals(TimeOfDay.SUNRISE, TimeOfDay.fromTicks(0, NOON));
        assertEquals(TimeOfDay.SUNRISE, TimeOfDay.fromTicks(NOON - 1, NOON));
    }

    @Test
    void noonBoundaryIsInclusive() {
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromTicks(NOON, NOON));
        assertEquals(TimeOfDay.NOON, TimeOfDay.fromTicks(TimeOfDay.NIGHT_START_TICKS - 1, NOON));
    }

    @Test
    void nightBeginsAtThirteenThousand() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromTicks(TimeOfDay.NIGHT_START_TICKS, NOON));
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromTicks(23999, NOON));
    }
}
