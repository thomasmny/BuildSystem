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
package de.eintosti.buildsystem.command.subcommand.worlds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.command.HelpPages.Entry;
import java.util.Arrays;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@NullMarked
class HelpSubCommandTest {

    @ParameterizedTest
    @EnumSource(WorldsArgument.class)
    void everySubcommandIsListed_withItsPermission(WorldsArgument argument) {
        Optional<Entry> entry = entryFor(argument.getName());

        assertTrue(entry.isPresent(), "/worlds " + argument.getName() + " is missing from /worlds help");
        assertEquals(argument.getPermission(), entry.get().permission());
        assertTrue(entry.get().suggest().startsWith("/worlds " + argument.getName()));
    }

    @ParameterizedTest
    @CsvSource({"addBuilder, /worlds addBuilder [player]", "removeBuilder, /worlds removeBuilder [player]"})
    void builderCommands_takeAPlayer_notAWorld(String name, String usage) {
        assertEquals(usage, entryFor(name).orElseThrow().usage());
    }

    @Test
    void everyEntryIsARegisteredSubcommand() {
        for (Entry entry : HelpSubCommand.COMMANDS) {
            String name = entry.usage().split(" ")[1];
            assertTrue(
                    Arrays.stream(WorldsArgument.values())
                            .anyMatch(a -> a.getName().equals(name)),
                    name);
        }
    }

    private static Optional<Entry> entryFor(String name) {
        return HelpSubCommand.COMMANDS.stream()
                .filter(entry -> entry.usage().split(" ")[1].equals(name))
                .findFirst();
    }
}
