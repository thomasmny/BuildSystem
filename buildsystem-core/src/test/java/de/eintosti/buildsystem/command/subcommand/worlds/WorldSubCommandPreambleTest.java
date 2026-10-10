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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.PlayerChatInput.InputRunnable;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.ArgumentCaptor;

/**
 * Pins the checks the {@code /worlds} subcommands share: the current-world preamble, the name-to-UUID lookup and the
 * world-name completion.
 */
@NullMarked
class WorldSubCommandPreambleTest {

    private ServerMock server;
    private Messages messages;
    private WorldServiceImpl worldService;
    private WorldStorageImpl worldStorage;
    private PlayerLookupService lookup;
    private SoundlessPlayer player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        messages = mock(Messages.class);
        worldStorage = mock(WorldStorageImpl.class);
        worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);
        lookup = spy(new PlayerLookupService(MockBukkit.createMockPlugin(), Runnable::run, Runnable::run));
        player = SoundlessPlayer.join(server, "Alex");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void currentWorld_notABuildWorld_sendsTheCommandsMissingKey() {
        new SetSpawnSubCommand(messages, worldService).execute(player, "", new String[] {"setSpawn"});

        verify(messages).sendMessage(player, "worlds_setspawn_world_not_imported");
    }

    @Test
    void currentWorld_withoutPermission_isRefusedBeforeAnythingChanges() {
        BuildWorld buildWorld = buildWorld(false);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(buildWorld);

        new RemoveSpawnSubCommand(messages, worldService).execute(player, "", new String[] {"removeSpawn"});

        verify(messages).sendPermissionError(player);
        verify(buildWorld.getData(), never()).set(eq(WorldDataKey.CUSTOM_SPAWN), anyString());
    }

    @Test
    void currentWorld_withPermission_runs() {
        BuildWorld buildWorld = buildWorld(true);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(buildWorld);

        new RemoveSpawnSubCommand(messages, worldService).execute(player, "", new String[] {"removeSpawn"});

        verify(buildWorld.getData()).set(WorldDataKey.CUSTOM_SPAWN, "");
    }

    @Test
    void lookup_onlinePlayer_skipsMojang() {
        SoundlessPlayer target = SoundlessPlayer.join(server, "Steve");
        BuildWorld buildWorld = buildWorld(true);
        when(buildWorld.getBuilders().isBuilder(target.getUniqueId())).thenReturn(true);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(buildWorld);

        removeBuilder().execute(player, "", new String[] {"removeBuilder", "Steve"});

        verify(lookup, never()).lookupUniqueId(anyString());
        verify(buildWorld.getBuilders()).removeBuilder(target.getUniqueId());
    }

    @Test
    void lookup_offlinePlayer_isResolved() {
        UUID notch = UUID.randomUUID();
        doReturn(CompletableFuture.completedFuture(notch)).when(lookup).lookupUniqueId("Notch");
        BuildWorld buildWorld = buildWorld(true);
        when(buildWorld.getBuilders().isBuilder(notch)).thenReturn(true);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(buildWorld);

        removeBuilder().execute(player, "", new String[] {"removeBuilder", "Notch"});

        verify(buildWorld.getBuilders()).removeBuilder(notch);
    }

    @Test
    void lookup_unknownPlayer_sendsTheCommandsNotFoundKey() {
        doReturn(CompletableFuture.completedFuture(null)).when(lookup).lookupUniqueId("Nobody");
        BuildWorld buildWorld = buildWorld(true);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(buildWorld);

        removeBuilder().execute(player, "", new String[] {"removeBuilder", "Nobody"});

        verify(messages).sendMessage(player, "worlds_removebuilder_player_not_found");
    }

    @Test
    void completion_offersPermittedWorldsMatchingThePrefix() {
        BuildWorld allowed = buildWorld(true);
        when(allowed.getName()).thenReturn("Lobby");
        BuildWorld otherPrefix = buildWorld(true);
        when(otherPrefix.getName()).thenReturn("Arena");
        BuildWorld denied = buildWorld(false);
        when(denied.getName()).thenReturn("Lounge");
        when(worldStorage.getBuildWorlds()).thenReturn(List.of(allowed, otherPrefix, denied));
        when(worldStorage.typedName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        EditSubCommand edit = new EditSubCommand(messages, worldService, mock(Menus.class));

        assertEquals(List.of("Lobby"), edit.complete(player, new String[] {"edit", "lo"}));
        assertEquals(List.of(), edit.complete(player, new String[] {"edit", "lo", "x"}));
    }

    @Test
    void setCreator_onlinePlayer_isStoredWithoutAskingMojang() {
        SoundlessPlayer target = SoundlessPlayer.join(server, "Steve");
        BuildWorld buildWorld = buildWorld(true);
        when(worldService.resolveWorldName(player, "world", WorldsArgument.SET_CREATOR.getPermission()))
                .thenReturn("world");
        when(worldStorage.getBuildWorld("world")).thenReturn(buildWorld);
        Prompts prompts = mock(Prompts.class);
        Prompts.Builder prompt = mock(Prompts.Builder.class, RETURNS_SELF);
        when(prompts.prompt(player)).thenReturn(prompt);
        doAnswer(invocation -> {
                    invocation.<InputRunnable>getArgument(0).run("Steve");
                    return null;
                })
                .when(prompt)
                .request(any());

        new SetCreatorSubCommand(messages, worldService, lookup, prompts, mock(SettingsService.class))
                .execute(player, "world", new String[] {"setCreator", "world"});

        verify(lookup, never()).lookupUniqueId(anyString());
        ArgumentCaptor<Builder> creator = ArgumentCaptor.forClass(Builder.class);
        verify(buildWorld.getBuilders()).setCreator(creator.capture());
        assertEquals(target.getUniqueId(), creator.getValue().getUniqueId());
        assertEquals("Steve", creator.getValue().getName());
    }

    private RemoveBuilderSubCommand removeBuilder() {
        return new RemoveBuilderSubCommand(messages, worldService, lookup, mock(Prompts.class));
    }

    private BuildWorld buildWorld(boolean permitted) {
        WorldPermissions permissions = mock(WorldPermissions.class);
        when(permissions.canPerformCommand(eq(player), any())).thenReturn(permitted);
        WorldData data = mock(WorldData.class);
        when(data.get(WorldDataKey.PERMISSION)).thenReturn("-");
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getName()).thenReturn("world");
        when(buildWorld.getPermissions()).thenReturn(permissions);
        when(buildWorld.getData()).thenReturn(data);
        when(buildWorld.getBuilders()).thenReturn(mock(Builders.class));
        return buildWorld;
    }
}
