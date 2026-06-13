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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SetPermissionSubCommandTest {

    @Test
    void isPermissionAllowed_emptyWhitelist_allowsAnything() {
        assertTrue(SetPermissionSubCommand.isPermissionAllowed("worlds.lobby", List.of()));
        assertTrue(SetPermissionSubCommand.isPermissionAllowed("anything.at.all", List.of()));
        assertTrue(SetPermissionSubCommand.isPermissionAllowed("-", List.of()));
    }

    @Test
    void isPermissionAllowed_nonEmptyWhitelist_allowsOnlyListed() {
        List<String> whitelist = List.of("worlds.lobby", "worlds.spawn");

        assertTrue(SetPermissionSubCommand.isPermissionAllowed("worlds.lobby", whitelist));
        assertTrue(SetPermissionSubCommand.isPermissionAllowed("worlds.spawn", whitelist));
        assertFalse(SetPermissionSubCommand.isPermissionAllowed("worlds.secret", whitelist));
    }

    @Test
    void isPermissionAllowed_nonEmptyWhitelist_alwaysAllowsClearSentinel() {
        List<String> whitelist = List.of("worlds.lobby");

        assertTrue(SetPermissionSubCommand.isPermissionAllowed("-", whitelist));
    }
}
