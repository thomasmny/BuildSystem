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

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class PlaceholdersTest {

    @Test
    void apply_simplePlaceholder_substituted() {
        String result = Placeholders.apply("World: %world%", Map.entry("%world%", "MyWorld"));
        assertEquals("World: MyWorld", result);
    }

    @Test
    void apply_valueContainingDollarSign_treatedLiterally() {
        // Previously replaceAll treated the value as a regex replacement template;
        // a '$' in the value would cause IndexOutOfBoundsException.
        String result = Placeholders.apply("Price: %val%", Map.entry("%val%", "$100"));
        assertEquals("Price: $100", result);
    }

    @Test
    void apply_valueContainingBackslash_treatedLiterally() {
        String result = Placeholders.apply("Path: %val%", Map.entry("%val%", "C:\\Users\\test"));
        assertEquals("Path: C:\\Users\\test", result);
    }

    @Test
    void apply_multiplePlaceholders_allSubstituted() {
        String result = Placeholders.apply(
                "%player% joined %world%",
                Map.entry("%player%", "Alice"),
                Map.entry("%world%", "Lobby")
        );
        assertEquals("Alice joined Lobby", result);
    }

    @Test
    void apply_absentPlaceholder_templateUnchanged() {
        String result = Placeholders.apply("Hello %name%", Map.entry("%world%", "X"));
        assertEquals("Hello %name%", result);
    }

    @Test
    void apply_noPlaceholders_returnsOriginal() {
        String result = Placeholders.apply("no placeholders here");
        assertEquals("no placeholders here", result);
    }
}
