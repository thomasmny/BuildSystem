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

import org.jspecify.annotations.NullMarked;

/**
 * Which third of the Minecraft day the world clock currently sits in. Drives the editor's time button.
 */
@NullMarked
public enum TimeOfDay {
    SUNRISE,
    NOON,
    NIGHT;

    /**
     * Minecraft tick at which night begins (the day is 24000 ticks).
     */
    static final int NIGHT_START_TICKS = 13000;

    /**
     * Buckets a raw world tick into a {@link TimeOfDay}.
     *
     * @param worldTicks The world time in ticks (0–24000)
     * @param noonStart The configured tick at which noon begins
     * @return The matching time-of-day bucket
     */
    static TimeOfDay fromTicks(int worldTicks, int noonStart) {
        if (worldTicks >= 0 && worldTicks < noonStart) {
            return SUNRISE;
        } else if (worldTicks >= noonStart && worldTicks < NIGHT_START_TICKS) {
            return NOON;
        } else {
            return NIGHT;
        }
    }
}
