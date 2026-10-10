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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Pins what the {@code /worlds} subcommands do once their world is resolved: the builder lookups and the world-name
 * completion. The checks before that are pinned by {@code WorldTargetTest}.
 */
@NullMarked
class WorldSubCommandsTest {

    private static final UUID NOTCH = UUID.randomUUID();

    private Messages messages;
    private WorldServiceImpl worldService;
    private WorldStorageImpl worldStorage;
    private PlayerLookupService lookup;
    private SoundlessPlayer player;
    private BuildWorld buildWorld;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        worldStorage = mock(WorldStorageImpl.class);
        worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);
        player = SoundlessPlayer.join(MockBukkit.mock(), "Alex");
        buildWorld = buildWorld(true);

        lookup = mock(PlayerLookupService.class);
        doAnswer(invocation -> {
                    if (invocation.<String>getArgument(0).equals("Notch")) {
                        invocation.<Consumer<Builder>>getArgument(1).accept(Builder.of(NOTCH, "Notch"));
                    } else {
                        invocation.<Runnable>getArgument(2).run();
                    }
                    return null;
                })
                .when(lookup)
                .resolve(anyString(), any(), any());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void removeBuilder_knownPlayer_isRemoved() {
        when(buildWorld.getBuilders().isBuilder(NOTCH)).thenReturn(true);

        new RemoveBuilderSubCommand(messages, worldService, lookup, mock(Prompts.class))
                .execute(player, buildWorld, new String[] {"removeBuilder", "Notch"});

        verify(buildWorld.getBuilders()).removeBuilder(NOTCH);
    }

    @Test
    void removeBuilder_unknownPlayer_sendsTheNotFoundKey() {
        new RemoveBuilderSubCommand(messages, worldService, lookup, mock(Prompts.class))
                .execute(player, buildWorld, new String[] {"removeBuilder", "Nobody"});

        verify(messages).sendMessage(player, "worlds_removebuilder_player_not_found");
    }

    @Test
    void setCreator_storesTheResolvedPlayer() {
        Prompts prompts = mock(Prompts.class);
        Prompts.Builder prompt = mock(Prompts.Builder.class, RETURNS_SELF);
        when(prompts.prompt(player)).thenReturn(prompt);
        doAnswer(invocation -> {
                    invocation.<InputRunnable>getArgument(0).run("Notch");
                    return null;
                })
                .when(prompt)
                .request(any());

        new SetCreatorSubCommand(messages, worldService, lookup, prompts, mock(SettingsService.class))
                .execute(player, buildWorld, new String[] {"setCreator"});

        verify(buildWorld.getBuilders())
                .setCreator(argThat(creator ->
                        creator.getUniqueId().equals(NOTCH) && creator.getName().equals("Notch")));
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
