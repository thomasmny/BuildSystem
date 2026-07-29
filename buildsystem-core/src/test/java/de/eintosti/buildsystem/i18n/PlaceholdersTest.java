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
package de.eintosti.buildsystem.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class PlaceholdersTest {

    @Test
    void applyTo_simplePlaceholder_substituted() {
        assertEquals("World: MyWorld", Placeholders.of("%world%", "MyWorld").applyTo("World: %world%"));
    }

    @Test
    void applyTo_valueContainingDollarSign_treatedLiterally() {
        // Previously replaceAll treated the value as a regex replacement template;
        // a '$' in the value would cause IndexOutOfBoundsException.
        assertEquals("Price: $100", Placeholders.of("%val%", "$100").applyTo("Price: %val%"));
    }

    @Test
    void applyTo_valueContainingBackslash_treatedLiterally() {
        assertEquals(
                "Path: C:\\Users\\test",
                Placeholders.of("%val%", "C:\\Users\\test").applyTo("Path: %val%"));
    }

    @Test
    void applyTo_multiplePlaceholders_allSubstituted() {
        Placeholders placeholders = Placeholders.of()
                .add("%player%", "Alice")
                .add("%world%", "Lobby")
                .build();
        assertEquals("Alice joined Lobby", placeholders.applyTo("%player% joined %world%"));
    }

    @Test
    void applyTo_absentPlaceholder_templateUnchanged() {
        assertEquals("Hello %name%", Placeholders.of("%world%", "X").applyTo("Hello %name%"));
    }

    @Test
    void applyTo_noPlaceholders_returnsOriginal() {
        assertEquals("no placeholders here", Placeholders.none().applyTo("no placeholders here"));
    }

    @Test
    void add_sameTokenTwice_keepsTheLaterValue() {
        Placeholders placeholders = Placeholders.of()
                .add("%world%", "first")
                .add("%world%", "second")
                .build();
        assertEquals("second", placeholders.applyTo("%world%"));
    }

    @Test
    void build_withNothingAdded_isTheSharedEmptySet() {
        assertSame(Placeholders.none(), Placeholders.of().build());
    }

    @Test
    void build_snapshotsTheBuilder() {
        Placeholders.Builder builder = Placeholders.of().add("%world%", "before");
        Placeholders built = builder.build();
        builder.add("%world%", "after");

        assertEquals("before", built.applyTo("%world%"), "a built set must not see later builder mutations");
    }

    @Test
    void nonStringValue_isRenderedWithValueOf() {
        assertEquals("Speed: 3", Placeholders.of("%speed%", 3).applyTo("Speed: %speed%"));
    }
}
