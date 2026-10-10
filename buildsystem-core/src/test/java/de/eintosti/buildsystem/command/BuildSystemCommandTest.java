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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.eintosti.buildsystem.command.HelpPages.Entry;
import de.eintosti.buildsystem.util.Permissions;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class BuildSystemCommandTest {

    @Test
    void worldsHelp_showsThePermissionItNeeds() {
        assertEquals(Permissions.HELP_WORLDS, entry("/worlds help").permission());
    }

    @Test
    void speed_withoutALevel_opensTheMenu_soTheLevelIsOptional() {
        assertEquals("/speed [1-5]", entry("/speed ").usage());
    }

    private static Entry entry(String suggest) {
        return BuildSystemCommand.COMMANDS.stream()
                .filter(entry -> entry.suggest().equals(suggest))
                .findFirst()
                .orElseThrow();
    }
}
