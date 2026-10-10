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
package de.eintosti.buildsystem.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;

final class Expectations {

    private Expectations() {}

    /**
     * Asserts that {@code actual} holds every expected entry, reporting all mismatches at once.
     */
    static void assertContains(Map<String, String> actual, Map<String, String> expected) {
        Map<String, String> wrong = new TreeMap<>();
        expected.forEach((key, value) -> {
            if (!value.equals(actual.get(key))) {
                wrong.put(key, "expected <" + value + "> but was <" + actual.get(key) + ">");
            }
        });
        assertEquals(Map.of(), wrong);
    }
}
