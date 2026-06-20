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
import static org.mockito.Mockito.*;

import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.i18n.Messages;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        boolean result = dispatcher.dispatch(player, new String[] {"unknown"});

        assertFalse(result);
        verify(messages).sendMessage(player, "worlds_unknown_command");
        verify(fooCmd, never()).execute(any(), any(), any());
    }

    @Test
    void dispatch_knownSubCommand_callsExecuteWithWorldNameFromArgs() {
        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd));

        boolean result = dispatcher.dispatch(player, new String[] {"foo", "myWorld"});

        assertTrue(result);
        verify(fooCmd).execute(player, "myWorld", new String[] {"foo", "myWorld"});
        verify(messages, never()).sendMessage(any(Player.class), anyString());
    }

    @Test
    void dispatch_noWorldArg_usesPlayerCurrentWorld() {
        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd));

        dispatcher.dispatch(player, new String[] {"foo"});

        verify(fooCmd).execute(player, "currentWorld", new String[] {"foo"});
    }

    @Test
    void complete_firstArg_returnsPermittedSubCommandNames() {
        Argument barArg = stubArg("bar", "perm.bar");
        SubCommand barCmd = mock(SubCommand.class);
        when(barCmd.getArgument()).thenReturn(barArg);

        when(player.hasPermission("perm.foo")).thenReturn(true);
        when(player.hasPermission("perm.bar")).thenReturn(false);

        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd, barCmd));

        List<String> result = dispatcher.complete(player, new String[] {""});

        assertTrue(result.contains("foo"));
        assertFalse(result.contains("bar"));
    }

    @Test
    void complete_secondArg_delegatesToMatchedSubCommand() {
        when(player.hasPermission(anyString())).thenReturn(true);
        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd));

        List<String> result = dispatcher.complete(player, new String[] {"foo", "partial"});

        assertEquals(List.of("hint"), result);
        verify(fooCmd).complete(player, new String[] {"foo", "partial"});
    }

    @Test
    void dispatch_staticSubCommandWinsCollisionWithDynamicShortcut() {
        SubCommand dynamicFoo = stubCommand("foo", "perm.dynamic");
        SubCommandDispatcher dispatcher = new SubCommandDispatcher(messages, List.of(fooCmd), dynamicOf(dynamicFoo));

        dispatcher.dispatch(player, new String[] {"foo"});

        // The real subcommand runs; the like-named category shortcut never does.
        verify(fooCmd).execute(player, "currentWorld", new String[] {"foo"});
        verify(dynamicFoo, never()).execute(any(), any(), any());
    }

    @Test
    void dispatch_dynamicShortcutResolvedWhenNoStaticMatch() {
        SubCommand publicShortcut = stubCommand("public", "buildsystem.navigator.public");
        SubCommandDispatcher dispatcher =
                new SubCommandDispatcher(messages, List.of(fooCmd), dynamicOf(publicShortcut));

        boolean result = dispatcher.dispatch(player, new String[] {"public"});

        assertTrue(result);
        verify(publicShortcut).execute(player, "currentWorld", new String[] {"public"});
        verify(messages, never()).sendMessage(any(Player.class), anyString());
    }

    @Test
    void complete_dynamicShortcutShadowedByStaticName() {
        when(player.hasPermission("perm.foo")).thenReturn(true);
        SubCommand dynamicFoo = stubCommand("foo", "perm.dynamic");
        SubCommand publicShortcut = stubCommand("public", "buildsystem.navigator.public");
        SubCommandDispatcher dispatcher =
                new SubCommandDispatcher(messages, List.of(fooCmd), dynamicOf(dynamicFoo, publicShortcut));

        List<String> result = dispatcher.complete(player, new String[] {""});

        // "foo" is contributed once by the static subcommand; the colliding shortcut is dropped.
        assertEquals(1, result.stream().filter("foo"::equals).count());
        assertTrue(result.contains("public"));
    }

    private static Argument stubArg(String name, String permission) {
        Argument arg = mock(Argument.class);
        when(arg.getName()).thenReturn(name);
        when(arg.getPermission()).thenReturn(permission);
        return arg;
    }

    /**
     * Builds a {@link SubCommand} mock backed by a stubbed {@link Argument}. The argument is created first so the nested
     * stubbing completes before {@code when(cmd.getArgument())}.
     */
    private static SubCommand stubCommand(String name, String permission) {
        Argument arg = stubArg(name, permission);
        SubCommand cmd = mock(SubCommand.class);
        when(cmd.getArgument()).thenReturn(arg);
        return cmd;
    }

    /**
     * A {@link DynamicSubCommands} that resolves and lists the given commands by name, standing in for the live
     * category shortcuts. {@code available()} is unfiltered — permission filtering of shortcuts is the caller's job.
     */
    private static DynamicSubCommands dynamicOf(SubCommand... commands) {
        Map<String, SubCommand> byName = new HashMap<>();
        for (SubCommand cmd : commands) {
            byName.put(cmd.getArgument().getName().toLowerCase(Locale.ROOT), cmd);
        }
        return new DynamicSubCommands() {
            @Override
            public Optional<SubCommand> resolve(String name) {
                return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
            }

            @Override
            public List<SubCommand> available(Player player) {
                return List.copyOf(byName.values());
            }
        };
    }
}
