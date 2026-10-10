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
package de.eintosti.buildsystem.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.test.TestData;
import java.io.File;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class WorldServiceImplResolveTest {

    @TempDir
    File dataFolder;

    private final Player player = mock(Player.class);
    private Messages messages;
    private WorldServiceImpl worldService;

    @BeforeEach
    void setUp() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        Services services = TestData.mockServices();
        when(services.config().current().world().defaultNamespace()).thenReturn("maps");
        messages = services.messages();
        worldService = TestData.worldService(plugin, services);
    }

    private void addWorld(String name, boolean permitted) {
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getUniqueId()).thenReturn(UUID.randomUUID());
        when(buildWorld.getName()).thenReturn(name);
        WorldPermissions permissions = mock(WorldPermissions.class);
        when(permissions.canPerformCommand(any(), any())).thenReturn(permitted);
        when(buildWorld.getPermissions()).thenReturn(permissions);
        worldService.getWorldStorage().addBuildWorld(buildWorld);
    }

    @Test
    void uniqueName_resolvesWithoutAMessage() {
        addWorld("events:Arena", true);

        assertEquals("events:Arena", worldService.resolveWorldName(player, "arena", "perm"));
        verifyNoInteractions(messages);
    }

    @Test
    void unknownName_isReturnedAsTyped() {
        assertEquals("world", worldService.resolveWorldName(player, "world", "perm"));
        assertEquals("world", worldService.resolveWorldName(player, "minecraft:world", "perm"));
        verifyNoInteractions(messages);
    }

    @Test
    void ambiguousName_listsOnlyTheWorldsThePlayerMayUse() {
        addWorld("events:Arena", true);
        addWorld("games:Arena", true);
        addWorld("secret:Arena", false);

        assertNull(worldService.resolveWorldName(player, "arena", "perm"));

        ArgumentCaptor<Placeholders> placeholders = ArgumentCaptor.forClass(Placeholders.class);
        verify(messages).sendMessage(eq(player), eq("worlds_world_ambiguous"), placeholders.capture());
        String reply = placeholders.getValue().applyTo("%worlds%");
        assertTrue(reply.contains("events:Arena"), reply);
        assertTrue(reply.contains("games:Arena"), reply);
        assertFalse(reply.contains("secret"), reply);
    }

    @Test
    void ambiguousName_withNoPermittedWorld_isAPermissionError() {
        addWorld("events:Arena", false);
        addWorld("games:Arena", false);

        assertNull(worldService.resolveWorldName(player, "arena", "perm"));

        verify(messages).sendPermissionError(player);
        verify(messages, never()).sendMessage(any(), anyString(), any(Placeholders.class));
    }
}
