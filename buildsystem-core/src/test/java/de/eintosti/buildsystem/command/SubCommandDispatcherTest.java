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

import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.i18n.Messages;
import java.util.List;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@NullMarked
class SubCommandDispatcherTest {

    private Messages messages;
    private Player player;
    private SubCommand fooCmd;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("currentWorld");

        Argument fooArg = stubArg("foo", "perm.foo");
        fooCmd = mock(SubCommand.class);
        when(fooCmd.getArgument()).thenReturn(fooArg);
        when(fooCmd.complete(any(), any())).thenReturn(List.of("hint"));
    }

    @Test
    void dispatch_unknownSubCommand_sendsErrorAndReturnsFalse() {
        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd));

        boolean result = dispatcher.dispatch(player, new String[]{"unknown"});

        assertFalse(result);
        verify(messages).sendMessage(player, "worlds_unknown_command");
        verify(fooCmd, never()).execute(any(), any(), any());
    }

    @Test
    void dispatch_knownSubCommand_callsExecuteWithWorldNameFromArgs() {
        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd));

        boolean result = dispatcher.dispatch(player, new String[]{"foo", "myWorld"});

        assertTrue(result);
        verify(fooCmd).execute(player, "myWorld", new String[]{"foo", "myWorld"});
        verify(messages, never()).sendMessage(any(Player.class), anyString());
    }

    @Test
    void dispatch_noWorldArg_usesPlayerCurrentWorld() {
        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd));

        dispatcher.dispatch(player, new String[]{"foo"});

        verify(fooCmd).execute(player, "currentWorld", new String[]{"foo"});
    }

    @Test
    void complete_firstArg_returnsPermittedSubCommandNames() {
        Argument barArg = stubArg("bar", "perm.bar");
        SubCommand barCmd = mock(SubCommand.class);
        when(barCmd.getArgument()).thenReturn(barArg);

        when(player.hasPermission("perm.foo")).thenReturn(true);
        when(player.hasPermission("perm.bar")).thenReturn(false);

        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd, barCmd));

        List<String> result = dispatcher.complete(player, new String[]{""});

        assertTrue(result.contains("foo"));
        assertFalse(result.contains("bar"));
    }

    @Test
    void complete_secondArg_delegatesToMatchedSubCommand() {
        when(player.hasPermission(anyString())).thenReturn(true);
        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd));

        List<String> result = dispatcher.complete(player, new String[]{"foo", "partial"});

        assertEquals(List.of("hint"), result);
        verify(fooCmd).complete(player, new String[]{"foo", "partial"});
    }

    private static Argument stubArg(String name, String permission) {
        Argument arg = mock(Argument.class);
        when(arg.getName()).thenReturn(name);
        when(arg.getPermission()).thenReturn(permission);
        return arg;
    }
}
