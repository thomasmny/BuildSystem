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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.eintosti.buildsystem.api.event.world.BuildWorldLoadEvent;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * A restore that could not put the old world back leaves it unloaded. Nothing may load it again until an admin has
 * looked, or the server would generate a new world in its place.
 */
@NullMarked
class WorldLoaderImplTest {

    @TempDir
    Path worldContainer;

    private ServerMock server;
    private Messages messages;
    private BuildWorldImpl buildWorld;
    private final AtomicBoolean loadAttempted = new AtomicBoolean();

    /**
     * Records a load that got as far as its event and cancels it, since MockBukkit cannot generate worlds.
     */
    public final class LoadRecorder implements Listener {

        @EventHandler
        public void onLoad(BuildWorldLoadEvent event) {
            loadAttempted.set(true);
            event.setCancelled(true);
        }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new ServerMock() {
            @Override
            public File getWorldContainer() {
                return worldContainer.toFile();
            }
        });
        server.getPluginManager().registerEvents(new LoadRecorder(), MockBukkit.createMockPlugin());
        messages = mock(Messages.class, RETURNS_DEEP_STUBS);
        SpawnService spawnService = mock(SpawnService.class);
        WorldContext context = new WorldContext(
                messages,
                mock(MenuItems.class),
                mock(ConfigService.class, RETURNS_DEEP_STUBS),
                mock(PlayerServiceImpl.class),
                spawnService,
                TestData.statusRegistry(),
                mock(CustomizableIcons.class),
                new TaskScheduler(MockBukkit.createMockPlugin()),
                Logger.getLogger("WorldLoaderImplTest"),
                new WorldOperations(messages, spawnService));
        buildWorld = new BuildWorldImpl(
                context,
                UUID.randomUUID(),
                "arena",
                BuildWorldType.NORMAL,
                WorldDataSchema.create("arena", TestData.NOT_STARTED),
                null,
                List.of(),
                1L,
                null,
                null);
    }

    /**
     * Leaves the state of a restore that moved the old world aside and could not move it back: the world folder is
     * missing.
     *
     * @return The missing world folder
     */
    private Path failedRestore() throws IOException {
        Path worldFolder = FileUtils.worldFolder("arena").toPath();
        Files.createDirectories(
                worldFolder.resolveSibling(".buildsystem-restore").resolve("arena.replaced"));
        return worldFolder;
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void afterAFailedRestore_loadIsRefused() throws IOException {
        Path worldFolder = failedRestore();

        buildWorld.getLoader().load();

        assertFalse(loadAttempted.get());
        assertFalse(Files.exists(worldFolder));
    }

    @Test
    void afterAFailedRestore_aPlayerIsToldWhy() throws IOException {
        PlayerMock player = server.addPlayer();
        failedRestore();

        buildWorld.getLoader().loadForPlayer(player);

        assertFalse(loadAttempted.get());
        verify(messages).sendMessage(eq(player), eq("worlds_world_restore_failed"), any(Placeholders.class));
    }
}
