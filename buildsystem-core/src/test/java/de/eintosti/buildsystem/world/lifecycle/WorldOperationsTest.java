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
package de.eintosti.buildsystem.world.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.lifecycle.SaveBehavior;
import de.eintosti.buildsystem.api.world.lifecycle.WorldTeleporter;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

@NullMarked
class WorldOperationsTest {

    private ServerMock server;
    private SpawnService spawnService;
    private WorldOperations operations;
    private WorldMock mainWorld;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        mainWorld = server.addSimpleWorld("main");
        spawnService = mock(SpawnService.class);
        Messages messages = mock(Messages.class);
        when(messages.getString(anyString(), any(Player.class))).thenReturn("moved");
        operations = new WorldOperations(messages, spawnService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerMock playerIn(WorldMock world) {
        PlayerMock player = server.addPlayer();
        player.teleport(world.getSpawnLocation());
        return player;
    }

    @Test
    void evacuate_movesPlayersToTheSpawnInAnotherWorld() {
        WorldMock lobby = server.addSimpleWorld("lobby");
        WorldMock doomed = server.addSimpleWorld("doomed");
        Location spawn = new Location(lobby, 5, 70, 5);
        when(spawnService.getSpawn()).thenReturn(spawn);
        PlayerMock player = playerIn(doomed);

        List<Player> moved = operations.evacuate("doomed", "key");

        assertEquals(List.of(player), moved);
        assertEquals(lobby, player.getWorld());
        assertTrue(doomed.getPlayers().isEmpty());
    }

    @Test
    void evacuate_ignoresASpawnInsideTheWorldBeingEmptied() {
        WorldMock doomed = server.addSimpleWorld("doomed");
        when(spawnService.isIn("doomed")).thenReturn(true);
        when(spawnService.getSpawn()).thenReturn(new Location(doomed, 0, 70, 0));
        PlayerMock player = playerIn(doomed);

        operations.evacuate("doomed", "key");

        assertEquals(mainWorld, player.getWorld());
        assertTrue(doomed.getPlayers().isEmpty());
    }

    @Test
    void evacuate_withNowhereToGo_kicksThePlayers() {
        when(spawnService.isIn("main")).thenReturn(true);
        PlayerMock player = playerIn(mainWorld);

        List<Player> moved = operations.evacuate("main", "key");

        assertTrue(moved.isEmpty());
        assertFalse(player.isOnline());
    }

    @Test
    void secondOperationOnABusyWorld_isRefusedWithoutRunning() {
        BuildWorld world = mock(BuildWorld.class);
        when(world.getUniqueId()).thenReturn(UUID.randomUUID());
        when(world.getName()).thenReturn("arena");
        CompletableFuture<Void> first = new CompletableFuture<>();
        operations.runExclusively(world, () -> first);
        AtomicBoolean secondRan = new AtomicBoolean();

        CompletableFuture<Void> second = operations.runExclusively(world, () -> {
            secondRan.set(true);
            return CompletableFuture.completedFuture(null);
        });

        assertFalse(secondRan.get());
        ExecutionException refused = assertThrows(ExecutionException.class, second::get);
        assertEquals("worlds_world_busy", ((WorldOperationRefusedException) refused.getCause()).messageKey());
        assertTrue(operations.isBusy(world));
    }

    @Test
    void finishedOrThrowingOperation_freesTheWorld() {
        BuildWorld world = mock(BuildWorld.class);
        when(world.getUniqueId()).thenReturn(UUID.randomUUID());
        CompletableFuture<Void> first = new CompletableFuture<>();
        operations.runExclusively(world, () -> first);

        first.complete(null);
        assertFalse(operations.isBusy(world));

        operations.runExclusively(world, () -> {
            throw new IllegalStateException("boom");
        });
        assertFalse(operations.isBusy(world));
    }

    @Test
    void operationReturningNoFutureOrThrowingAnError_freesTheWorld() {
        BuildWorld world = mock(BuildWorld.class);
        when(world.getUniqueId()).thenReturn(UUID.randomUUID());

        CompletableFuture<Void> none = operations.runExclusively(world, () -> null);
        assertThrows(ExecutionException.class, none::get);
        assertFalse(operations.isBusy(world));

        assertThrows(
                StackOverflowError.class,
                () -> operations.runExclusively(world, () -> {
                    throw new StackOverflowError();
                }));
        assertFalse(operations.isBusy(world));
    }

    @Test
    void takeOffline_whenTheUnloadIsRefused_sendsThePlayersBack() {
        WorldMock lobby = server.addSimpleWorld("lobby");
        WorldMock arena = server.addSimpleWorld("arena");
        when(spawnService.getSpawn()).thenReturn(new Location(lobby, 5, 70, 5));
        PlayerMock player = playerIn(arena);
        BuildWorld world = mock(BuildWorld.class, RETURNS_DEEP_STUBS);
        when(world.getName()).thenReturn("arena");
        WorldTeleporter teleporter = world.getTeleporter();

        WorldOperationRefusedException refused = assertThrows(
                WorldOperationRefusedException.class, () -> operations.takeOffline(world, "key", SaveBehavior.SAVE));

        assertEquals("worlds_world_unload_failed", refused.messageKey());
        verify(world.getUnloader()).forceUnload(SaveBehavior.SAVE);
        verify(teleporter).teleport(player);
    }

    @Test
    void takeOffline_ofAnUnloadedWorld_movesNobody() {
        BuildWorld world = mock(BuildWorld.class, RETURNS_DEEP_STUBS);
        when(world.getName()).thenReturn("absent");

        assertEquals(List.of(), operations.takeOffline(world, "key", SaveBehavior.DISCARD));
    }
}
