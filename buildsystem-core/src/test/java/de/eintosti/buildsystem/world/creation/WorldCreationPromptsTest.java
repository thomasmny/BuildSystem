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
package de.eintosti.buildsystem.world.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Pins that creating a world from a template in game copies the template off the main thread.
 */
@NullMarked
class WorldCreationPromptsTest {

    @TempDir
    File dataFolder;

    @TempDir
    Path worldContainer;

    private final List<Runnable> backgroundTasks = new ArrayList<>();
    private final Player player = mock(Player.class);
    private final WorldStorageImpl worldStorage = mock(WorldStorageImpl.class);
    private final Prompts prompts = mock(Prompts.class);
    private final Prompts.PromptFlow flow = mock(Prompts.PromptFlow.class, RETURNS_SELF);
    private Messages messages;
    private MockedStatic<Bukkit> bukkit;
    private WorldCreationPrompts creationPrompts;

    @BeforeEach
    void setUp() throws Exception {
        Path template = dataFolder.toPath().resolve("templates/castle");
        Files.createDirectories(template.resolve("region"));
        Files.writeString(template.resolve("level.dat"), "level");

        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(scheduler.background()).thenReturn(backgroundTasks::add);
        when(scheduler.mainThread()).thenReturn(Runnable::run);
        WorldContext base = TestData.worldContext();
        WorldContext context = new WorldContext(
                base.messages(),
                base.menuItems(),
                base.configService(),
                base.playerService(),
                base.spawnService(),
                base.statusRegistry(),
                base.customizableIcons(),
                scheduler,
                base.logger(),
                base.operations());
        messages = context.messages();

        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);
        when(worldStorage.newWorldName("copy")).thenReturn("copy");
        when(prompts.sanitizeWorldName(player, "copy")).thenReturn("copy");
        WorldBuilderImpl builder = new WorldBuilderImpl(context, worldStorage, dataFolder, "copy");
        when(worldService.newWorld("copy")).thenReturn(builder);
        when(prompts.flow(player)).thenReturn(flow);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(worldContainer.toFile());
        bukkit.when(Bukkit::getServer).thenReturn(mock(Server.class, RETURNS_DEEP_STUBS));

        creationPrompts = new WorldCreationPrompts(worldService, prompts, messages);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void templateCreation_copiesOffTheMainThread() {
        Runnable onComplete = enterName();

        onComplete.run();

        assertEquals(1, backgroundTasks.size(), "the template must be copied in the background");
        assertFalse(Files.exists(worldContainer.resolve("copy/level.dat")), "nothing may be copied on the main thread");
        verify(messages).sendMessage(eq(player), eq("worlds_template_creation_started"), any(Placeholders.class));
    }

    @Test
    void templateCreation_failedCopy_reportsErrorAndRegistersNothing() throws Exception {
        Runnable onComplete = enterName();
        onComplete.run();

        // The template disappearing between the prompt and the copy makes the copy fail.
        FileUtils.deleteDirectory(dataFolder.toPath().resolve("templates/castle"));
        backgroundTasks.removeFirst().run();

        verify(messages).sendMessage(eq(player), eq("worlds_template_creation_error"), any(Placeholders.class));
        verify(messages, never()).sendMessage(eq(player), eq("worlds_creation_finished"), any(Placeholders.class));
        verify(worldStorage, never()).addBuildWorld(any());
        // The folder reserved for the copy is deleted in the background.
        assertEquals(1, backgroundTasks.size());
        backgroundTasks.removeFirst().run();
        assertFalse(Files.exists(worldContainer.resolve("copy")), "no half-created world folder may be left");
    }

    /**
     * Starts the template flow and enters the world name, returning what the flow runs once every step accepted.
     */
    private Runnable enterName() {
        creationPrompts.startWorldNameInput(player, BuildWorldType.TEMPLATE, "castle", false, false, null);

        ArgumentCaptor<Prompts.PromptFlow.StepValidator> nameStep =
                ArgumentCaptor.forClass(Prompts.PromptFlow.StepValidator.class);
        verify(flow).step(eq("enter_world_name"), nameStep.capture());
        assertTrue(nameStep.getValue().validate("copy"));

        ArgumentCaptor<Runnable> onComplete = ArgumentCaptor.forClass(Runnable.class);
        verify(flow).start(onComplete.capture());
        return onComplete.getValue();
    }
}
