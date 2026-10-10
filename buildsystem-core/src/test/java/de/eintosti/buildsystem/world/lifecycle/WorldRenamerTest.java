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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * The name a player types is cleaned up before the rename checks it; these pin that the checks run against the
 * cleaned name, so a rename can never take over another world's name or merge into its folder.
 */
@NullMarked
class WorldRenamerTest {

    @TempDir
    Path worldContainer;

    private MockedStatic<Bukkit> bukkit;
    private WorldStorageImpl worldStorage;
    private Messages messages;
    private Prompts prompts;
    private WorldOperations operations;
    private WorldRenamer renamer;
    private Player player;
    private BuildWorld buildWorld;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(worldContainer.toFile());
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of());

        worldStorage = mock(WorldStorageImpl.class);
        when(worldStorage.isNameTaken(anyString())).thenCallRealMethod();
        when(worldStorage.renamedWorldName(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        messages = mock(Messages.class);
        prompts = mock(Prompts.class);
        // Mimics the sanitizer: the typed "Taken!" is cleaned up to "taken".
        when(prompts.sanitizeWorldName(any(), anyString())).thenReturn("taken");
        operations = new WorldOperations(messages, mock(SpawnService.class));
        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.operations()).thenReturn(operations);

        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(scheduler.mainThread()).thenReturn(Runnable::run);
        when(scheduler.background()).thenReturn(Runnable::run);
        renamer = new WorldRenamer(
                mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS),
                worldService,
                worldStorage,
                messages,
                prompts,
                mock(SpawnService.class),
                scheduler);

        player = mock(Player.class);
        when(player.getLocation()).thenReturn(new Location(null, 0, 0, 0));
        buildWorld = mock(BuildWorld.class, RETURNS_DEEP_STUBS);
        when(buildWorld.getName()).thenReturn("old");
        when(buildWorld.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void cleanedNameOfARegisteredWorld_isRefused() {
        when(worldStorage.worldExists("taken")).thenReturn(true);

        renamer.rename(player, buildWorld, "Taken!");

        verify(messages).sendMessage(player, "worlds_world_exists");
        assertNothingStarted();
    }

    @Test
    void cleanedNameOfAnUnregisteredFolder_isRefused() throws IOException {
        Files.createDirectories(worldContainer.resolve("taken"));

        renamer.rename(player, buildWorld, "Taken!");

        verify(messages).sendMessage(player, "worlds_world_exists");
        assertNothingStarted();
        assertTrue(Files.isDirectory(worldContainer.resolve("taken")));
    }

    @Test
    void unloadThatDoesNotHappen_leavesTheWorldWhereItIs() throws IOException {
        when(prompts.sanitizeWorldName(any(), anyString())).thenReturn("fresh");
        World oldWorld = mock(World.class);
        when(oldWorld.getPlayers()).thenReturn(List.of());
        when(oldWorld.getWorldFolder()).thenReturn(worldContainer.resolve("old").toFile());
        // The unload is refused (an event cancelled it), so the world stays loaded.
        bukkit.when(() -> Bukkit.getWorld("old")).thenReturn(oldWorld);
        Files.createDirectories(worldContainer.resolve("old"));

        renamer.rename(player, buildWorld, "fresh");

        verify(messages).sendMessage(eq(player), eq("worlds_world_unload_failed"), any(Placeholders.class));
        assertFalse(operations.isBusy(buildWorld), "the world must not stay locked");
        assertTrue(Files.isDirectory(worldContainer.resolve("old")));
        assertFalse(Files.exists(worldContainer.resolve("fresh")));
        verify(worldStorage, never()).rename(any(), anyString(), anyString());
    }

    @Test
    void worldBusyWithAnotherOperation_isNotRenamed() throws IOException {
        when(prompts.sanitizeWorldName(any(), anyString())).thenReturn("fresh");
        Files.createDirectories(worldContainer.resolve("old"));
        operations.runExclusively(buildWorld, CompletableFuture::new);

        renamer.rename(player, buildWorld, "fresh");

        verify(messages).sendMessage(eq(player), eq("worlds_world_busy"), any(Placeholders.class));
        verify(buildWorld.getUnloader(), never()).forceUnload(any());
        assertTrue(Files.isDirectory(worldContainer.resolve("old")));
        assertFalse(Files.exists(worldContainer.resolve("fresh")));
    }

    @Test
    void worldWithoutAFolder_isReportedUnknown() {
        when(prompts.sanitizeWorldName(any(), anyString())).thenReturn("fresh");

        renamer.rename(player, buildWorld, "fresh");

        verify(messages).sendMessage(player, "worlds_rename_unknown_world");
        assertNothingStarted();
    }

    private void assertNothingStarted() {
        assertFalse(operations.isBusy(buildWorld));
        verify(buildWorld.getUnloader(), never()).forceUnload(any());
        bukkit.verify(() -> Bukkit.unloadWorld(any(World.class), any(Boolean.class)), never());
    }
}
