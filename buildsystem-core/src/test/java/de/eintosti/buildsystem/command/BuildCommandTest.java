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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.Permissions;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@NullMarked
class BuildCommandTest {

    private final BuildCommand command =
            new BuildCommand(mock(Messages.class), mock(Logger.class), mock(PlayerServiceImpl.class));

    @Test
    void complete_suggestsPlayersOnlyToThoseAllowedToToggleOthers() {
        Player other = mock(Player.class);
        when(other.getName()).thenReturn("Alex");

        Player allowed = player(true);
        Player denied = player(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of(other));

            assertEquals(List.of("Alex"), command.complete(allowed, "build", new String[] {"a"}));
            assertEquals(List.of(), command.complete(denied, "build", new String[] {"a"}));
        }
    }

    private static Player player(boolean mayToggleOthers) {
        Player player = mock(Player.class);
        when(player.hasPermission(Permissions.BUILD)).thenReturn(true);
        when(player.hasPermission(Permissions.BUILD_OTHER)).thenReturn(mayToggleOthers);
        return player;
    }
}
