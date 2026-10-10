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
package de.eintosti.buildsystem.world.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Pins which type a world is generated as when it records a custom generator.
 */
@NullMarked
class BukkitWorldFactoryTest {

    @Test
    void importedWithBuiltInGenerator_generatesThatType() {
        assertEquals(
                BuildWorldType.VOID,
                BukkitWorldFactory.generationType(
                        BuildWorldType.IMPORTED, new CustomGeneratorImpl("BuildSystem", "void", null)));
    }

    @Test
    void importedWithPluginGenerator_staysImported() {
        // Used to be valueOf("TERRA"), which threw after the world had already been registered.
        assertEquals(
                BuildWorldType.IMPORTED,
                BukkitWorldFactory.generationType(
                        BuildWorldType.IMPORTED, new CustomGeneratorImpl("Terra", "Terra", null)));
    }

    @Test
    void importedWithUnknownBuiltInType_staysImported() {
        assertEquals(
                BuildWorldType.IMPORTED,
                BukkitWorldFactory.generationType(
                        BuildWorldType.IMPORTED, new CustomGeneratorImpl("BuildSystem", "nonsense", null)));
    }

    @Test
    void notImported_keepsItsType() {
        assertEquals(
                BuildWorldType.FLAT,
                BukkitWorldFactory.generationType(
                        BuildWorldType.FLAT, new CustomGeneratorImpl("BuildSystem", "void", null)));
    }
}
