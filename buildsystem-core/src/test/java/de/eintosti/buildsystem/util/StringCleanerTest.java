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

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
class StringCleanerTest {

    @Test
    void hasInvalidNameCharacters_validInput_returnsFalse() {
        assertFalse(StringCleaner.hasInvalidNameCharacters("valid_World-1/x"));
    }

    @Test
    void hasInvalidNameCharacters_spaceAndExclamation_returnsTrue() {
        assertTrue(StringCleaner.hasInvalidNameCharacters("bad name!"));
    }

    @Test
    void hasInvalidNameCharacters_emptyString_returnsFalse() {
        assertFalse(StringCleaner.hasInvalidNameCharacters(""));
    }

    @Test
    void hasInvalidNameCharacters_onlySpecialChars_returnsTrue() {
        assertTrue(StringCleaner.hasInvalidNameCharacters("@#%"));
    }

    @Test
    void firstInvalidChar_stringWithSpace_returnsSpace() {
        assertEquals(" ", StringCleaner.firstInvalidChar("abc def"));
    }

    @Test
    void firstInvalidChar_validString_returnsNull() {
        assertNull(StringCleaner.firstInvalidChar("valid_World-1"));
    }

    @Test
    void firstInvalidChar_multipleInvalid_returnsFirst() {
        assertEquals("!", StringCleaner.firstInvalidChar("hello!world?"));
    }

    @Test
    void sanitize_removesInvalidCharactersAndSpace() {
        // space is matched by INVALID_NAME_CHARACTERS and removed before replace(" ", "_") runs
        assertEquals("myworld", StringCleaner.sanitize("my world!"));
    }

    @Test
    void sanitize_alreadyClean_returnsUnchanged() {
        assertEquals("clean_World-1/x", StringCleaner.sanitize("clean_World-1/x"));
    }

    @Test
    void sanitize_onlyInvalidChars_returnsEmpty() {
        assertEquals("", StringCleaner.sanitize("!!!"));
    }

    @Test
    void sanitize_trailingWhitespace_trimmed() {
        // Leading/trailing whitespace: replaceAll removes them (they match INVALID_NAME_CHARACTERS),
        // then trim() catches any residual whitespace (e.g. non-breaking spaces not caught by regex).
        assertEquals("abc", StringCleaner.sanitize("  abc  "));
    }
}
