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
package de.eintosti.buildsystem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class NumberUtilsTest {

    @Test
    void toInt_validString_returnsValue() {
        assertEquals(42, NumberUtils.toInt("42"));
    }

    @Test
    void toInt_nonNumericString_returnsZero() {
        assertEquals(0, NumberUtils.toInt("abc"));
    }

    @Test
    void toInt_nullString_returnsZero() {
        assertEquals(0, NumberUtils.toInt(null));
    }

    @Test
    void toInt_emptyString_returnsZero() {
        assertEquals(0, NumberUtils.toInt(""));
    }

    @Test
    void toInt_withDefault_nullString_returnsDefault() {
        assertEquals(7, NumberUtils.toInt(null, 7));
    }

    @Test
    void toInt_withDefault_validString_returnsValue() {
        assertEquals(13, NumberUtils.toInt("13", 7));
    }

    @Test
    void toInt_withDefault_nonNumericString_returnsDefault() {
        assertEquals(7, NumberUtils.toInt("abc", 7));
    }

    @Test
    void toInt_negativeNumber_returnsNegative() {
        assertEquals(-5, NumberUtils.toInt("-5"));
    }

    @Test
    void toInt_leadingWhitespace_returnsDefault() {
        // Integer.parseInt does not trim; whitespace causes NumberFormatException → default
        assertEquals(0, NumberUtils.toInt(" 1"));
    }
}
