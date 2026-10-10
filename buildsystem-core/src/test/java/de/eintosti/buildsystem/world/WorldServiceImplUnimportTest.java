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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.event.world.BuildWorldUnloadEvent;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * An unimport that does not happen must tell the player why and leave the world registered.
 */
@NullMarked
class WorldServiceImplUnimportTest {

    @TempDir
    File dataFolder;

    private Services services;
    private WorldServiceImpl worldService;
    private Player player;

    @BeforeEach
    void setUp() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        services = TestData.mockServices();
        worldService = TestData.worldService(plugin, services);
        player = mock(Player.class);
    }

    private BuildWorldImpl registeredWorld(String name) {
        BuildWorldImpl buildWorld = new BuildWorldImpl(
                services.worldContext(),
                UUID.randomUUID(),
                name,
                BuildWorldType.NORMAL,
                WorldDataSchema.create(name, TestData.NOT_STARTED),
                null,
                List.of(),
                1L,
                null,
                null);
        worldService.getWorldStorage().addBuildWorld(buildWorld);
        return buildWorld;
    }

    @Test
    void busyWorld_tellsThePlayerAndStaysRegistered() {
        BuildWorldImpl buildWorld = registeredWorld("busy");
        worldService.operations().runExclusively(buildWorld, CompletableFuture::new);

        worldService.unimportWorld(player, buildWorld);

        verify(services.messages()).sendMessage(eq(player), eq("worlds_world_busy"), any(Placeholders.class));
        assertSame(buildWorld, worldService.getWorldStorage().getBuildWorld("busy"));
    }

    @Test
    void cancelledUnload_tellsThePlayerAndStaysRegistered() {
        BuildWorldImpl buildWorld = registeredWorld("held");
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);
        doAnswer(invocation -> {
                    if (invocation.getArgument(0) instanceof BuildWorldUnloadEvent event) {
                        event.setCancelled(true);
                    }
                    return null;
                })
                .when(pluginManager)
                .callEvent(any());
        World world = mock(World.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(() -> Bukkit.getWorld("held")).thenReturn(world);

            worldService.unimportWorld(player, buildWorld);
        }

        verify(services.messages()).sendMessage(eq(player), eq("worlds_world_unload_failed"), any(Placeholders.class));
        assertSame(buildWorld, worldService.getWorldStorage().getBuildWorld("held"));
        assertFalse(worldService.operations().isBusy(buildWorld));
    }
}
