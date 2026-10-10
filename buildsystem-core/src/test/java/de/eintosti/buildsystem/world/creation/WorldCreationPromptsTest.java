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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.lifecycle.WorldTeleporter;
import de.eintosti.buildsystem.config.PluginConfig;
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
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final Prompts prompts = mock(Prompts.class);
    private final Prompts.PromptFlow flow = mock(Prompts.PromptFlow.class, RETURNS_SELF);
    private WorldContext context;
    private WorldStorageImpl worldStorage;
    private WorldBuilderImpl builder;
    private Messages messages;
    private MockedStatic<Bukkit> bukkit;
    private WorldCreationPrompts creationPrompts;

    @BeforeEach
    void setUp() throws Exception {
        Path template = dataFolder.toPath().resolve("templates/castle");
        Files.createDirectories(template.resolve("region"));
        Files.writeString(template.resolve("level.dat"), "level");

        when(scheduler.background()).thenReturn(backgroundTasks::add);
        when(scheduler.mainThread()).thenReturn(Runnable::run);
        WorldContext base = TestData.worldContext();
        context = new WorldContext(
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

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(worldContainer.toFile());
        bukkit.when(Bukkit::getServer).thenReturn(mock(Server.class, RETURNS_DEEP_STUBS));

        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        worldStorage =
                spy(TestData.worldService(plugin, TestData.mockServices()).getWorldStorage());
        doReturn("copy").when(worldStorage).newWorldName("copy");

        WorldServiceImpl worldService = mock(WorldServiceImpl.class);
        when(worldService.getWorldStorage()).thenReturn(worldStorage);
        when(prompts.sanitizeWorldName(player, "copy")).thenReturn("copy");
        builder = spy(new WorldBuilderImpl(context, worldStorage, dataFolder, "copy"));
        when(worldService.newWorld("copy")).thenReturn(builder);
        when(prompts.flow(player)).thenReturn(flow);

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
        // The folder reserved for the copy is deleted in the background, and the name is held until it is gone.
        assertEquals(1, backgroundTasks.size());
        assertTrue(worldStorage.isBeingCreated("copy"));
        backgroundTasks.removeFirst().run();
        assertFalse(Files.exists(worldContainer.resolve("copy")), "no half-created world folder may be left");
        assertFalse(worldStorage.isBeingCreated("copy"));
    }

    @Test
    void templateCreation_copiedButUnregisteredWorld_cannotBeImported() {
        List<Runnable> mainThreadTasks = new ArrayList<>();
        when(scheduler.mainThread()).thenReturn(mainThreadTasks::add);
        // With a main world, new worlds go to dimensions/minecraft, where the import lists them.
        World mainWorld = mock(World.class);
        when(mainWorld.getName()).thenReturn("world");
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(mainWorld));
        // What registering a world reads, so registering the copy can be told apart from the import refusing it.
        PluginConfig.World.Defaults defaults =
                context.configService().current().world().defaults();
        when(defaults.permission().publicPermission()).thenReturn("-");
        when(defaults.difficulty()).thenReturn(Difficulty.PEACEFUL);
        when(context.customizableIcons().getIcon(any())).thenReturn(Material.GRASS_BLOCK);
        enterName().run();

        backgroundTasks.removeFirst().run();
        assertTrue(FileUtils.isWorldDirectory(FileUtils.worldFolder("copy")), "the copy is done");
        assertEquals(1, mainThreadTasks.size(), "but the world is not registered yet");

        assertTrue(worldStorage.isBeingCreated("copy"));
        assertEquals(List.of(), worldStorage.unimportedWorldNames(), "a world being created is not importable");
        assertNull(new WorldImporterImpl(context, worldStorage, "copy")
                .notify(player)
                .build());
        verify(messages).sendMessage(eq(player), eq("worlds_world_exists"), any(Placeholders.class));
        verify(worldStorage, never()).addBuildWorld(any());

        // Generating the world fails here, so its folder is deleted, and the name is held until it is gone.
        mainThreadTasks.removeFirst().run();
        assertEquals(1, backgroundTasks.size());
        assertTrue(worldStorage.isBeingCreated("copy"));
        backgroundTasks.removeFirst().run();
        assertFalse(worldStorage.isBeingCreated("copy"));
    }

    @Test
    void templateCreation_registersTheCopiedWorldThenTeleports() {
        BuildWorld created = mock(BuildWorld.class);
        WorldTeleporter teleporter = mock(WorldTeleporter.class);
        when(created.getTeleporter()).thenReturn(teleporter);
        when(player.isOnline()).thenReturn(true);
        doAnswer(invocation -> {
                    assertTrue(Files.isRegularFile(worldContainer.resolve("copy/level.dat")), "copied first");
                    assertTrue(worldStorage.isBeingCreated("copy"));
                    return created;
                })
                .when(builder)
                .generateAndRegister(true);
        enterName().run();

        backgroundTasks.removeFirst().run();

        InOrder order = inOrder(builder, teleporter);
        order.verify(builder).generateAndRegister(true);
        order.verify(teleporter).teleport(player);
        verify(messages).sendMessage(eq(player), eq("worlds_creation_finished"), any(Placeholders.class));
        assertFalse(worldStorage.isBeingCreated("copy"));
    }

    @Test
    void templateCreation_generationThrows_tellsThePlayer() {
        doThrow(new IllegalStateException("generation failed")).when(builder).generateAndRegister(true);
        enterName().run();

        backgroundTasks.removeFirst().run();

        verify(messages).sendMessage(eq(player), eq("worlds_creation_failed"), any(Placeholders.class));
        verify(messages, never()).sendMessage(eq(player), eq("worlds_creation_finished"), any(Placeholders.class));
        assertFalse(worldStorage.isBeingCreated("copy"));
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
