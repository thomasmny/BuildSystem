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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Pins how the unloader keeps the loaded flag and the unload timer in line with the config, in particular across a
 * {@code /config reload} that changes {@code world.unload}.
 */
@NullMarked
class WorldUnloaderImplTest {

    private ServerMock server;
    private ConfigService configService;
    private WorldContext context;
    private Messages messages;
    private SpawnService spawnService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().world().unload().timeUntilUnload()).thenReturn("01:00:00");
        messages = mock(Messages.class, RETURNS_DEEP_STUBS);
        spawnService = mock(SpawnService.class);
        context = new WorldContext(
                messages,
                mock(MenuItems.class),
                configService,
                mock(PlayerServiceImpl.class),
                spawnService,
                TestData.statusRegistry(),
                mock(CustomizableIcons.class),
                new TaskScheduler(MockBukkit.createMockPlugin()),
                Logger.getLogger("WorldUnloaderImplTest"),
                new WorldOperations(messages, spawnService));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void unloadingEnabled(boolean enabled) {
        when(configService.current().world().unload().enabled()).thenReturn(enabled);
    }

    private BuildWorldImpl buildWorld(String name) {
        return new BuildWorldImpl(
                context,
                UUID.randomUUID(),
                name,
                BuildWorldType.NORMAL,
                WorldDataSchema.create(name, TestData.NOT_STARTED),
                null,
                List.of(),
                1L,
                null,
                null);
    }

    private int pendingTasks() {
        return server.getScheduler().getPendingTasks().size();
    }

    @Test
    void manageUnload_twice_keepsOneTimer() {
        unloadingEnabled(true);
        server.addSimpleWorld("timer");
        BuildWorldImpl world = buildWorld("timer");

        world.getUnloader().manageUnload();
        world.getUnloader().manageUnload();

        assertEquals(1, pendingTasks());
    }

    @Test
    void resetUnloadTask_replacesTheTimer() {
        unloadingEnabled(true);
        server.addSimpleWorld("reset");
        BuildWorldImpl world = buildWorld("reset");
        world.getUnloader().manageUnload();

        world.getUnloader().resetUnloadTask();
        world.getUnloader().resetUnloadTask();

        assertEquals(1, pendingTasks());
    }

    @Test
    void manageUnload_withUnloadingOn_flagsAnAbsentWorldUnloaded() {
        unloadingEnabled(true);
        BuildWorldImpl world = buildWorld("absent");
        world.setLoaded(true);

        world.getUnloader().manageUnload();

        assertFalse(world.isLoaded());
    }

    @Test
    void manageUnload_withUnloadingOn_flagsAPresentWorldLoaded() {
        unloadingEnabled(true);
        server.addSimpleWorld("present");
        BuildWorldImpl world = buildWorld("present");

        world.getUnloader().manageUnload();

        assertTrue(world.isLoaded());
    }

    @Test
    void reloadTurningUnloadingOff_cancelsTheTimer() {
        unloadingEnabled(true);
        server.addSimpleWorld("toggle");
        BuildWorldImpl world = buildWorld("toggle");
        world.getUnloader().manageUnload();
        assertEquals(1, pendingTasks());

        unloadingEnabled(false);
        world.getUnloader().manageUnload();

        assertEquals(0, pendingTasks());
        assertTrue(world.isLoaded());
    }

    @Test
    void reloadTurningUnloadingOff_leavesAnUnloadedWorldUnloaded() {
        unloadingEnabled(true);
        BuildWorldImpl world = buildWorld("gone");
        world.getUnloader().manageUnload();

        unloadingEnabled(false);
        // Loading would reach the world container, which MockBukkit does not implement.
        assertDoesNotThrow(() -> world.getUnloader().manageUnload());

        assertFalse(world.isLoaded());
    }

    @Test
    void unloadingByHand_cancelsThePendingTimerEvenWhenTheSpawnWorldStaysLoaded() {
        unloadingEnabled(true);
        server.addSimpleWorld("manual");
        when(spawnService.isIn("manual")).thenReturn(true);
        BuildWorldImpl world = buildWorld("manual");
        world.getUnloader().manageUnload();

        world.getUnloader().unload();

        assertTrue(world.getWorld().isPresent());
        assertEquals(0, pendingTasks());
    }

    @Test
    void loadingForAPlayer_whileBusy_tellsThemAndLoadsNothing() {
        BuildWorldImpl world = buildWorld("busy");
        context.operations().runExclusively(world, CompletableFuture::new);
        PlayerMock player = server.addPlayer();

        // Loading would reach the world container, which MockBukkit does not implement.
        assertDoesNotThrow(() -> world.getLoader().loadForPlayer(player));

        verify(messages).sendMessage(eq(player), eq("worlds_world_busy"), any(Placeholders.class));
        assertFalse(world.isLoaded());
    }

    @Test
    void timerFiring_unloadsAnEmptyWorld() {
        unloadingEnabled(true);
        when(configService.current().world().unload().timeUntilUnload()).thenReturn("00:00:01");
        server.addSimpleWorld("expire");
        BuildWorldImpl world = buildWorld("expire");
        world.getUnloader().manageUnload();

        server.getScheduler().performTicks(21);

        assertFalse(world.isLoaded());
        assertTrue(world.getWorld().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1h", "01:xx:00"})
    void malformedDelay_unloadsAfterOneHour(String delay) {
        unloadingEnabled(true);
        when(configService.current().world().unload().timeUntilUnload()).thenReturn(delay);
        server.addSimpleWorld("malformed");
        BuildWorldImpl world = buildWorld("malformed");
        world.getUnloader().manageUnload();

        server.getScheduler().performTicks(20 * 3599);
        assertTrue(world.isLoaded());
        server.getScheduler().performTicks(21);
        assertFalse(world.isLoaded());
    }
}
