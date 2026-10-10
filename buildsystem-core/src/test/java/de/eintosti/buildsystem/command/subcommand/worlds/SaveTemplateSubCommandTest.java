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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldOperations;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * Pins that saving a template holds the world against a delete, rename or restore and keeps the chunk writer out of its
 * folder for as long as the copy reads it.
 */
@NullMarked
class SaveTemplateSubCommandTest {

    @TempDir
    File dataFolder;

    @TempDir
    Path worldFolder;

    private final Messages messages = mock(Messages.class);
    private final WorldServiceImpl worldService = mock(WorldServiceImpl.class);
    private final WorldOperations operations = new WorldOperations(messages, mock(SpawnService.class));
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final List<Runnable> backgroundTasks = new ArrayList<>();
    private final Player player = mock(Player.class);
    private final BuildWorld buildWorld = mock(BuildWorld.class);
    private final World world = mock(World.class);
    private MockedStatic<Bukkit> bukkit;
    private SaveTemplateSubCommand command;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(worldFolder.resolve("level.dat"), "level");

        when(worldService.operations()).thenReturn(operations);
        when(scheduler.background()).thenReturn(backgroundTasks::add);
        when(scheduler.mainThread()).thenReturn(Runnable::run);

        when(buildWorld.getName()).thenReturn("castle");
        when(buildWorld.getUniqueId()).thenReturn(UUID.randomUUID());
        when(buildWorld.getWorld()).thenReturn(Optional.of(world));
        when(world.getWorldFolder()).thenReturn(worldFolder.toFile());
        when(world.isAutoSave()).thenReturn(true);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld("castle")).thenReturn(world);

        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        when(configService.current().world().invalidCharacters()).thenReturn("");
        command = new SaveTemplateSubCommand(
                messages, worldService, configService, dataFolder, Logger.getLogger("test"), scheduler);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void execute_holdsWorldAndPausesWritesUntilCopyFinishes() {
        command.execute(player, buildWorld, new String[] {"savetemplate", "castle", "keep"});

        assertEquals(1, backgroundTasks.size(), "the copy must run in the background");
        assertTrue(operations.isBusy(buildWorld), "a delete or rename must be refused while the copy runs");
        verify(world).setAutoSave(false);
        verify(world, never()).setAutoSave(true);

        backgroundTasks.removeFirst().run();

        assertFalse(operations.isBusy(buildWorld), "the world must be free once the copy is done");
        verify(world).setAutoSave(true);
        assertTrue(new File(dataFolder, "templates/keep/level.dat").isFile());
        verify(messages).sendMessage(eq(player), eq("worlds_savetemplate_finished"), any(Placeholders.class));
    }

    @Test
    void execute_failedCopy_resumesWritesAndFreesWorld() throws Exception {
        // A file where the templates folder belongs makes the copy fail.
        Files.writeString(dataFolder.toPath().resolve("templates"), "");

        command.execute(player, buildWorld, new String[] {"savetemplate", "castle", "keep"});
        backgroundTasks.removeFirst().run();

        assertFalse(operations.isBusy(buildWorld), "the world must be free once the copy failed");
        verify(world).setAutoSave(true);
        verify(messages).sendMessage(eq(player), eq("worlds_savetemplate_error"), any(Placeholders.class));
        verify(messages, never()).sendMessage(eq(player), eq("worlds_savetemplate_finished"), any(Placeholders.class));
    }

    @Test
    void execute_whileWorldBusy_isRefused() {
        operations.runExclusively(buildWorld, CompletableFuture::new);

        command.execute(player, buildWorld, new String[] {"savetemplate", "castle", "keep"});

        assertTrue(backgroundTasks.isEmpty(), "nothing may be copied while another operation holds the world");
        verify(messages).sendMessage(eq(player), eq("worlds_world_busy"), any(Placeholders.class));
        verify(messages, never()).sendMessage(eq(player), eq("worlds_savetemplate_started"), any(Placeholders.class));
        verify(world, never()).setAutoSave(false);
        assertFalse(new File(dataFolder, "templates/keep").exists());
    }
}
