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

import de.eintosti.buildsystem.i18n.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@NullMarked
class CommandBaseTest {

    private static final String[] NO_ARGS = new String[0];
    private static final String PERMISSION = "test.perm";

    // Minimal no-op subclass for testing CommandBase internals
    private static class TestCommand extends CommandBase {

        final List<String[]> playerInvocations = new ArrayList<>();
        final List<String[]> senderInvocations = new ArrayList<>();

        TestCommand(boolean playerOnly) {
            this(mock(Logger.class), mock(Messages.class), playerOnly);
        }

        private TestCommand(Logger logger, Messages messages, boolean playerOnly) {
            super(logger, messages, playerOnly);
            when(messages.getString(anyString(), any(CommandSender.class))).thenReturn("test");
        }

        @Override
        protected void run(Player player, String label, String[] args) {
            playerInvocations.add(args);
        }

        @Override
        protected void run(CommandSender sender, String label, String[] args) {
            senderInvocations.add(args);
        }
    }

    @Test
    void playerOnly_consoleSender_warnsAndDoesNotDelegate() {
        TestCommand cmd = new TestCommand(true);
        CommandSender console = mock(CommandSender.class);

        cmd.onCommand(console, null, "test", NO_ARGS);

        assertTrue(cmd.playerInvocations.isEmpty(), "run(Player) must not be called for console sender");
        verify(cmd.logger).warning(anyString());
    }

    @Test
    void playerOnly_playerSender_delegatesToRunPlayer() {
        TestCommand cmd = new TestCommand(true);
        Player player = mock(Player.class);

        cmd.onCommand(player, null, "test", NO_ARGS);

        assertEquals(1, cmd.playerInvocations.size());
        assertTrue(cmd.senderInvocations.isEmpty());
    }

    @Test
    void consoleCapable_consoleSender_delegatesToRunSender() {
        TestCommand cmd = new TestCommand(false);
        CommandSender console = mock(CommandSender.class);

        cmd.onCommand(console, null, "test", NO_ARGS);

        assertEquals(1, cmd.senderInvocations.size());
        assertTrue(cmd.playerInvocations.isEmpty());
    }

    @Test
    void worldNameFromArgs_emptyArgs_returnsCurrentWorldName() {
        TestCommand cmd = new TestCommand(true);
        Player player = mock(Player.class);
        org.bukkit.World world = mock(org.bukkit.World.class);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");

        String result = cmd.worldNameFromArgs(player, NO_ARGS, 0);

        assertEquals("world", result);
    }

    @Test
    void worldNameFromArgs_argsPresent_returnsArgAtIndex() {
        TestCommand cmd = new TestCommand(true);
        Player player = mock(Player.class);

        String result = cmd.worldNameFromArgs(player, new String[] {"myWorld"}, 0);

        assertEquals("myWorld", result);
    }

    @Test
    void requirePermission_granted_returnsTrue() {
        TestCommand cmd = new TestCommand(true);
        Player player = mock(Player.class);
        when(player.hasPermission(PERMISSION)).thenReturn(true);

        assertTrue(cmd.requirePermission(player, PERMISSION));
        verify(cmd.messages, never()).sendPermissionError(any());
    }

    @Test
    void requirePermission_denied_returnsFalseAndSendsError() {
        TestCommand cmd = new TestCommand(true);
        Player player = mock(Player.class);
        when(player.hasPermission(PERMISSION)).thenReturn(false);

        assertFalse(cmd.requirePermission(player, PERMISSION));
        verify(cmd.messages).sendPermissionError(player);
    }
}
