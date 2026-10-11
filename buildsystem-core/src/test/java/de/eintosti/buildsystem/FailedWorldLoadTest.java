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
package de.eintosti.buildsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;

import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.StorageBackend;
import de.eintosti.buildsystem.storage.StorageType;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.data.WorldDataSchema;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.WorldCreator;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockito.MockedConstruction;

/**
 * Starts the real plugin on SQLite with a worlds table it cannot read.
 */
@NullMarked
class FailedWorldLoadTest {

    private ServerMock server;
    private MockedConstruction<BuildSystemMetrics> metrics;
    private BuildSystemPlugin plugin;

    @TempDir
    File worldContainer;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new ServerMock() {
            @Override
            public File getWorldContainer() {
                return worldContainer;
            }
        });
        // bStats refuses to run unrelocated, which it only is in the shaded jar.
        metrics = mockConstruction(BuildSystemMetrics.class);
        server.addWorld(new WorldMock(new WorldCreator("arena")) {
            @Override
            public File getWorldFolder() {
                return new File(worldContainer, "arena");
            }
        });
        plugin = MockBukkit.load(BuildSystemPlugin.class);
    }

    @AfterEach
    void tearDown() {
        metrics.close();
        MockBukkit.unmock();
    }

    @Test
    void aWorldsLoadThatFails_leavesTheFolderItsWorlds() throws Exception {
        BuildSystem api = Objects.requireNonNull(server.getServicesManager().load(BuildSystem.class));
        Builder creator = Builder.of(UUID.randomUUID(), "Creator");
        Folder folder = api.getWorldService()
                .getFolderStorage()
                .createFolder(
                        "Maps",
                        api.getNavigatorCategoryRegistry()
                                .get(NavigatorCategoryRegistry.PUBLIC_ID)
                                .orElseThrow(),
                        creator);
        BuildWorldImpl world = new BuildWorldImpl(
                TestData.worldContext(),
                UUID.randomUUID(),
                "arena",
                BuildWorldType.NORMAL,
                WorldDataSchema.create("arena", TestData.ARCHIVE_STATUS),
                creator,
                List.of(),
                System.currentTimeMillis(),
                null,
                folder);
        ((WorldStorageImpl) api.getWorldService().getWorldStorage()).addBuildWorld(world);
        folder.addWorld(world);
        server.getPluginManager().disablePlugin(plugin);
        List<String> worlds = List.of(world.getUniqueId().toString());
        assertEquals(worlds, storedWorldsOf(folder));

        try (Connection connection =
                        DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "storage.db"));
                Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE buildsystem_worlds RENAME TO kept_worlds");
            statement.execute("CREATE TABLE buildsystem_worlds (id VARCHAR(64) PRIMARY KEY, payload TEXT)");
        }
        server.getPluginManager().enablePlugin(plugin);
        long deadline = System.currentTimeMillis() + 10_000;
        while (api().getWorldService().getFolderStorage().getFolders().isEmpty()) {
            assertTrue(System.currentTimeMillis() < deadline, "the folders never loaded");
            server.getScheduler().performOneTick();
            Thread.onSpinWait();
        }
        for (int i = 0; i < 20; i++) {
            server.getScheduler().performOneTick();
            Thread.sleep(10);
        }
        server.getPluginManager().disablePlugin(plugin);

        assertEquals(worlds, storedWorldsOf(folder));
    }

    private BuildSystem api() {
        return Objects.requireNonNull(server.getServicesManager().load(BuildSystem.class));
    }

    private Object storedWorldsOf(Folder folder) throws Exception {
        PluginConfig.Storage.Database settings = new ConfigService(plugin).readStorageSettings();
        assertEquals(StorageType.SQLITE, settings.type());
        StorageBackend backend =
                StorageBackend.connect(settings.type(), settings, plugin.getDataFolder(), plugin.getLogger());
        try {
            Map<String, Object> stored = backend.collection(EntityKind.FOLDERS)
                    .loadAll()
                    .get(folder.getUniqueId().toString());
            return Objects.requireNonNull(stored).get("worlds");
        } finally {
            backend.close();
        }
    }
}
