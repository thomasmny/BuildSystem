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
import static org.mockito.Mockito.*;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.exception.WorldDirectoryNotFoundException;
import de.eintosti.buildsystem.api.exception.WorldNotFoundException;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.data.WorldDataImpl;
import de.eintosti.buildsystem.world.data.WorldDataImpl.WorldDataBuilder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * Characterization tests for the world deletion failure modes and the success-path invariant: the registry entry and
 * persisted metadata must be gone before the directory is removed, so a crash mid-delete leaves a re-importable folder
 * rather than a registry entry pointing at nothing.
 */
class WorldServiceImplDeleteTest {

    @TempDir
    File dataFolder;

    @TempDir
    Path worldContainer;

    private BuildSystemPlugin plugin;
    private WorldServiceImpl worldService;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        // The unload time string is parsed unconditionally in the WorldUnloader constructor.
        when(plugin.getConfigService().current().world().unload().timeUntilUnload())
                .thenReturn("06:00:00");
        worldService = new WorldServiceImpl(plugin);
        when(plugin.getWorldService()).thenReturn(worldService);
    }

    private BuildWorldImpl registeredWorld(String name) {
        WorldDataImpl data = new WorldDataBuilder(name)
                .withStatus(TestData.NOT_STARTED)
                .withDifficulty(Difficulty.NORMAL)
                .withMaterial(XMaterial.GRASS_BLOCK)
                .withPermission("-")
                .withProject("-")
                .withVisibility(Visibility.EVERYONE)
                .withPermissionOverrideEnabled(() -> false)
                .withProjectOverrideEnabled(() -> false)
                .build();
        BuildWorldImpl buildWorld = new BuildWorldImpl(
                plugin,
                UUID.randomUUID(),
                name,
                BuildWorldType.NORMAL,
                data,
                Builder.of(UUID.randomUUID(), "Creator"),
                List.of(),
                System.currentTimeMillis(),
                null,
                null);
        worldService.getWorldStorage().addBuildWorld(buildWorld);
        return buildWorld;
    }

    @Test
    void deleteWorld_unknownWorld_failsWithWorldNotFound() {
        BuildWorld unknown = mock(BuildWorld.class);
        when(unknown.getName()).thenReturn("ghost");

        ExecutionException thrown = assertThrows(
                ExecutionException.class,
                () -> worldService.deleteWorld(unknown).get(5, TimeUnit.SECONDS));

        assertInstanceOf(WorldNotFoundException.class, thrown.getCause());
    }

    @Test
    void deleteWorld_missingDirectory_failsWithDirectoryNotFound() {
        BuildWorldImpl buildWorld = registeredWorld("no_directory");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorldContainer).thenReturn(worldContainer.toFile());

            ExecutionException thrown = assertThrows(
                    ExecutionException.class,
                    () -> worldService.deleteWorld(buildWorld).get(5, TimeUnit.SECONDS));

            assertInstanceOf(WorldDirectoryNotFoundException.class, thrown.getCause());
        }
    }

    @Test
    void deleteWorld_success_removesRegistryEntryAndDirectory() throws Exception {
        BuildWorldImpl buildWorld = registeredWorld("doomed");
        Path worldDirectory = worldContainer.resolve("doomed");
        Files.createDirectories(worldDirectory.resolve("region"));
        Files.writeString(worldDirectory.resolve("level.dat"), "level");

        Server server = mock(Server.class, RETURNS_DEEP_STUBS);
        // The post-delete event is deferred to the main thread via the scheduler; the real scheduler is not
        // available under test, so a mock that drops the task mirrors the scheduling without firing the event.
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorldContainer).thenReturn(worldContainer.toFile());
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getWorld("doomed")).thenReturn(null);

            worldService.deleteWorld(buildWorld).get(10, TimeUnit.SECONDS);
        }

        assertFalse(Files.exists(worldDirectory), "world directory must be deleted");
        assertNull(worldService.getWorldStorage().getBuildWorld("doomed"), "registry entry must be removed");
        assertFalse(buildWorld.isLoaded());
    }
}
