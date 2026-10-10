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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.command.WorldToggleCommand.Toggle;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.ArgumentCaptor;

@NullMarked
class WorldToggleCommandTest {

    private ServerMock server;
    private Messages messages;
    private WorldServiceImpl worldService;
    private WorldStorageImpl worldStorage;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        messages = mock(Messages.class);
        worldStorage = mock(WorldStorageImpl.class);
        worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);
        when(worldService.resolveWorldName(any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void physicsAll_onlyTouchesWorldsThePlayerMayToggle() {
        BuildWorld allowed = buildWorld(true);
        BuildWorld alsoAllowed = buildWorld(true);
        BuildWorld denied = buildWorld(false);
        when(worldStorage.getBuildWorlds()).thenReturn(List.of(allowed, denied, alsoAllowed));

        run(Toggle.PHYSICS, "all");

        verify(allowed.getData()).set(WorldDataKey.PHYSICS, true);
        verify(alsoAllowed.getData()).set(WorldDataKey.PHYSICS, true);
        verify(denied.getData(), never()).set(eq(WorldDataKey.PHYSICS), anyBoolean());
        ArgumentCaptor<Placeholders> counts = ArgumentCaptor.forClass(Placeholders.class);
        verify(messages).sendMessage(eq(player), eq("physics_activated_all_skipped"), counts.capture());
        assertEquals("2 activated, 1 skipped", counts.getValue().applyTo("%activated% activated, %skipped% skipped"));
    }

    @Test
    void physicsAll_withPermissionEverywhere_saysAllWorlds() {
        BuildWorld allowed = buildWorld(true);
        when(worldStorage.getBuildWorlds()).thenReturn(List.of(allowed));

        run(Toggle.PHYSICS, "all");

        verify(allowed.getData()).set(WorldDataKey.PHYSICS, true);
        verify(messages).sendMessage(player, "physics_activated_all");
    }

    @Test
    void physicsAll_withoutPermissionAnywhere_isRefused() {
        BuildWorld denied = buildWorld(false);
        when(worldStorage.getBuildWorlds()).thenReturn(List.of(denied));

        run(Toggle.PHYSICS, "all");

        verify(denied.getData(), never()).set(eq(WorldDataKey.PHYSICS), anyBoolean());
        verify(messages).sendPermissionError(player);
        verify(messages, never()).sendMessage(player, "physics_activated_all");
        verify(messages, never()).sendMessage(eq(player), eq("physics_activated_all_skipped"), any());
    }

    @Test
    void explosionsAll_isAWorldName() {
        run(Toggle.EXPLOSIONS, "all");

        verify(messages).sendMessage(player, "explosions_unknown_world");
        verify(worldStorage, never()).getBuildWorlds();
    }

    @Test
    void namedWorldWithoutPermission_isRefused() {
        BuildWorld denied = buildWorld(false);
        when(worldStorage.getBuildWorld("other")).thenReturn(denied);

        run(Toggle.EXPLOSIONS, "other");

        verify(messages).sendPermissionError(player);
        verify(denied.getData(), never()).set(eq(WorldDataKey.EXPLOSIONS), anyBoolean());
    }

    @Test
    void explosions_flipsOnAndOff() {
        BuildWorld buildWorld = worldNamedMaps();
        when(buildWorld.getData().get(WorldDataKey.EXPLOSIONS)).thenReturn(false);

        run(Toggle.EXPLOSIONS, "maps");

        verify(buildWorld.getData()).set(WorldDataKey.EXPLOSIONS, true);
        verify(messages).sendMessage(eq(player), eq("explosions_activated"), any());

        when(buildWorld.getData().get(WorldDataKey.EXPLOSIONS)).thenReturn(true);

        run(Toggle.EXPLOSIONS, "maps");

        verify(buildWorld.getData()).set(WorldDataKey.EXPLOSIONS, false);
        verify(messages).sendMessage(eq(player), eq("explosions_deactivated"), any());
    }

    @Test
    void noai_activatingTurnsMobAiOffForLivingEntities() {
        BuildWorld buildWorld = worldNamedMaps();
        LivingEntity zombie = spawnZombie();
        when(buildWorld.getData().get(WorldDataKey.MOB_AI)).thenReturn(true);

        run(Toggle.NOAI, "maps");

        verify(buildWorld.getData()).set(WorldDataKey.MOB_AI, false);
        verify(messages).sendMessage(eq(player), eq("noai_activated"), any());
        assertFalse(zombie.hasAI());
    }

    @Test
    void noai_deactivatingGivesLivingEntitiesTheirAiBack() {
        BuildWorld buildWorld = worldNamedMaps();
        LivingEntity zombie = spawnZombie();
        zombie.setAI(false);
        when(buildWorld.getData().get(WorldDataKey.MOB_AI)).thenReturn(false);

        run(Toggle.NOAI, "maps");

        verify(buildWorld.getData()).set(WorldDataKey.MOB_AI, true);
        verify(messages).sendMessage(eq(player), eq("noai_deactivated"), any());
        assertTrue(zombie.hasAI());
    }

    private BuildWorld worldNamedMaps() {
        WorldMock world = server.addSimpleWorld("maps");
        BuildWorld buildWorld = buildWorld(true);
        when(buildWorld.getName()).thenReturn("maps");
        when(worldStorage.getBuildWorld("maps")).thenReturn(buildWorld);
        when(worldStorage.getBuildWorld(world)).thenReturn(buildWorld);
        return buildWorld;
    }

    private LivingEntity spawnZombie() {
        WorldMock world = (WorldMock) server.getWorld("maps");
        return (LivingEntity) world.spawnEntity(world.getSpawnLocation(), EntityType.ZOMBIE);
    }

    private void run(Toggle toggle, String... args) {
        new WorldToggleCommand(messages, mock(Logger.class), worldService, toggle)
                .run(player, toggle.name().toLowerCase(), args);
    }

    private BuildWorld buildWorld(boolean mayToggle) {
        WorldPermissions permissions = mock(WorldPermissions.class);
        when(permissions.canPerformCommand(eq(player), any())).thenReturn(mayToggle);
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getPermissions()).thenReturn(permissions);
        when(buildWorld.getData()).thenReturn(mock(WorldData.class));
        return buildWorld;
    }
}
