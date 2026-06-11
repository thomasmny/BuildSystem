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
class ArgumentParserTest {

    @Test
    void isArgument_presentArg_returnsTrue() {
        ArgumentParser parser = new ArgumentParser(new String[]{"-g", "Generator", "-t"});
        assertTrue(parser.isArgument("g"));
        assertTrue(parser.isArgument("t"));
    }

    @Test
    void isArgument_absentArg_returnsFalse() {
        ArgumentParser parser = new ArgumentParser(new String[]{"-g", "Generator"});
        assertFalse(parser.isArgument("x"));
    }

    @Test
    void isArgument_caseInsensitive() {
        // isArgument strips dashes and uses equalsIgnoreCase
        ArgumentParser parser = new ArgumentParser(new String[]{"-Name", "World"});
        assertTrue(parser.isArgument("name"));
        assertTrue(parser.isArgument("NAME"));
    }

    @Test
    void getFlag_presentFlag_returnsTrue() {
        // -t is a flag: it is last and/or followed by a dash-argument
        ArgumentParser parser = new ArgumentParser(new String[]{"-g", "Generator", "-t"});
        assertTrue(parser.getFlag("t"));
    }

    @Test
    void getFlag_absentFlag_returnsFalse() {
        ArgumentParser parser = new ArgumentParser(new String[]{"-g", "Generator"});
        assertFalse(parser.getFlag("x"));
    }

    @Test
    void getFlag_argWithValue_notAFlag() {
        // -g has a following non-dash value, so it is stored in map, not flags
        ArgumentParser parser = new ArgumentParser(new String[]{"-g", "Generator"});
        assertFalse(parser.getFlag("g"));
    }

    @Test
    void getValue_presentArg_returnsValue() {
        ArgumentParser parser = new ArgumentParser(new String[]{"-g", "Generator"});
        assertEquals("Generator", parser.getValue("g"));
    }

    @Test
    void getValue_multiWordValue_returnsJoined() {
        ArgumentParser parser = new ArgumentParser(new String[]{"-name", "My", "World"});
        assertEquals("My World", parser.getValue("name"));
    }

    @Test
    void getValue_absentArg_returnsNull() {
        ArgumentParser parser = new ArgumentParser(new String[]{"-g", "Generator"});
        assertNull(parser.getValue("x"));
    }

    @Test
    void emptyArray_noFlagsNoValues() {
        ArgumentParser parser = new ArgumentParser(new String[]{});
        assertFalse(parser.isArgument("g"));
        assertFalse(parser.getFlag("g"));
        assertNull(parser.getValue("g"));
    }
}
