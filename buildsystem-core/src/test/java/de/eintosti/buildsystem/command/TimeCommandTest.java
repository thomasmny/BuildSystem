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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

@NullMarked
class TimeCommandTest {

    private ServerMock server;
    private Messages messages;
    private TimeCommand command;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        server.addSimpleWorld("unmanaged");
        messages = mock(Messages.class);
        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(mock(WorldStorageImpl.class));
        when(worldService.resolveWorldName(any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        command = new TimeCommand(
                messages, Logger.getLogger("test"), mock(ConfigService.class, RETURNS_DEEP_STUBS), worldService);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @ParameterizedTest(name = "/{0} without the permission")
    @ValueSource(strings = {"day", "night"})
    void unmanagedWorld_withoutThePermission_isRefused(String label) {
        run(label);

        verify(messages).sendPermissionError(player);
        verify(messages, never()).sendMessage(eq(player), eq(label + "_set"), any(Placeholders.class));
    }

    @ParameterizedTest(name = "/{0} with the permission")
    @ValueSource(strings = {"day", "night"})
    void unmanagedWorld_withThePermission_setsTheTime(String label) {
        player.addAttachment(
                MockBukkit.createMockPlugin(), label.equals("day") ? Permissions.DAY : Permissions.NIGHT, true);

        run(label);

        verify(messages, never()).sendPermissionError(player);
        verify(messages).sendMessage(eq(player), eq(label + "_set"), any(Placeholders.class));
    }

    private void run(String label) {
        command.onCommand(player, mock(Command.class), label, new String[] {"unmanaged"});
    }
}
