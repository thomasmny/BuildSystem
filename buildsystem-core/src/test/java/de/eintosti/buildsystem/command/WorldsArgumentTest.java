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
package de.eintosti.buildsystem.command;

import static org.junit.jupiter.api.Assertions.*;

import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Pins {@link WorldsArgument} as the single source of truth for {@code /worlds} subcommand permissions and names. Subcommand executors and tab completions both derive their
 * permission from {@code getArgument().getPermission()}; this guards against a literal drifting from the enum (the historic TeleportSubCommand bug).
 */
@NullMarked
class WorldsArgumentTest {

    @ParameterizedTest
    @EnumSource(WorldsArgument.class)
    void permissionIsNamespaced(WorldsArgument argument) {
        assertTrue(
                argument.getPermission().startsWith("buildsystem."),
                argument + " permission should be namespaced: " + argument.getPermission());
    }

    @ParameterizedTest
    @EnumSource(WorldsArgument.class)
    void matchArgumentRoundTrips(WorldsArgument argument) {
        WorldsArgument resolved = WorldsArgument.matchArgument(argument.getName());
        assertNotNull(resolved, "matchArgument should resolve " + argument.getName());
        assertEquals(argument, resolved);
    }
}
