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
package de.eintosti.buildsystem.command.subcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.SoundlessPlayer;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * Pins the checks a {@link WorldSubCommand} runs before it acts, and the order they answer in.
 */
@NullMarked
class WorldTargetTest {

    private static final String PERMISSION = "buildsystem.edit";
    private static final WorldTarget TYPED = WorldTarget.argument(2, "worlds_edit");
    private static final WorldTarget CURRENT = WorldTarget.current("worlds_setspawn_world_not_imported");

    private Messages messages;
    private WorldServiceImpl worldService;
    private WorldStorageImpl worldStorage;
    private SoundlessPlayer player;

    @BeforeEach
    void setUp() {
        player = SoundlessPlayer.join(MockBukkit.mock(), "Alex");
        messages = mock(Messages.class);
        worldStorage = mock(WorldStorageImpl.class);
        worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void typed_aNameTheResolverAnswered_stopsWithoutAnotherMessage() {
        when(worldService.resolveWorldName(player, "lobby", PERMISSION)).thenReturn(null);

        assertNull(resolve(TYPED, "lobby", "edit", "lobby"));
        verifyNoInteractions(messages);
    }

    @Test
    void typed_withoutPermission_isRefusedBeforeTheUsageIsChecked() {
        typedWorld("lobby", false);

        assertNull(resolve(TYPED, "lobby", "edit", "lobby", "extra"));
        verify(messages).sendPermissionError(player);
        verify(messages, never()).sendMessage(eq(player), any(String.class));
    }

    @Test
    void typed_tooManyArguments_sendsTheUsageBeforeTheWorldIsMissed() {
        when(worldService.resolveWorldName(player, "nowhere", PERMISSION)).thenReturn("nowhere");

        assertNull(resolve(TYPED, "nowhere", "edit", "nowhere", "extra"));
        verify(messages).sendMessage(player, "worlds_edit_usage");
        verify(messages, never()).sendMessage(player, "worlds_edit_unknown_world");
    }

    @Test
    void typed_unknownWorld_sendsTheUnknownWorldKey() {
        when(worldService.resolveWorldName(player, "nowhere", PERMISSION)).thenReturn("nowhere");

        assertNull(resolve(TYPED, "nowhere", "edit", "nowhere"));
        verify(messages).sendMessage(player, "worlds_edit_unknown_world");
    }

    @Test
    void typed_permittedWorld_isFound() {
        BuildWorld lobby = typedWorld("lobby", true);

        assertSame(lobby, resolve(TYPED, "lobby", "edit", "lobby"));
        verifyNoInteractions(messages);
    }

    @Test
    void current_notABuildWorld_sendsTheMissingKey() {
        assertNull(resolve(CURRENT, "world", "setSpawn"));
        verify(messages).sendMessage(player, "worlds_setspawn_world_not_imported");
    }

    @Test
    void current_withoutPermission_isRefused() {
        BuildWorld world = buildWorld(false);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(world);

        assertNull(resolve(CURRENT, "world", "setSpawn"));
        verify(messages).sendPermissionError(player);
    }

    @Test
    void current_permittedWorld_isFound() {
        BuildWorld world = buildWorld(true);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(world);

        assertSame(world, resolve(CURRENT, "world", "setSpawn"));
        verifyNoInteractions(messages);
    }

    @Test
    void subCommand_runsOnlyOnceTheTargetFoundTheWorld() {
        List<BuildWorld> ran = new ArrayList<>();
        WorldSubCommand subCommand = new WorldSubCommand(messages, worldService, CURRENT) {
            @Override
            protected void execute(Player player, BuildWorld buildWorld, String[] args) {
                ran.add(buildWorld);
            }

            @Override
            public Argument getArgument() {
                return WorldsArgument.EDIT;
            }
        };

        subCommand.execute(player, "world", new String[] {"edit"});
        BuildWorld world = buildWorld(true);
        when(worldStorage.getBuildWorld(player.getWorld())).thenReturn(world);
        subCommand.execute(player, "world", new String[] {"edit"});

        assertEquals(List.of(world), ran);
    }

    private BuildWorld typedWorld(String name, boolean permitted) {
        BuildWorld world = buildWorld(permitted);
        when(worldService.resolveWorldName(player, name, PERMISSION)).thenReturn(name);
        when(worldStorage.getBuildWorld(name)).thenReturn(world);
        return world;
    }

    private BuildWorld buildWorld(boolean permitted) {
        WorldPermissions permissions = mock(WorldPermissions.class);
        when(permissions.canPerformCommand(player, PERMISSION)).thenReturn(permitted);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getPermissions()).thenReturn(permissions);
        return buildWorld;
    }

    private @Nullable BuildWorld resolve(WorldTarget target, String worldName, String... args) {
        return target.resolve(player, worldName, args, messages, worldService, PERMISSION);
    }
}
