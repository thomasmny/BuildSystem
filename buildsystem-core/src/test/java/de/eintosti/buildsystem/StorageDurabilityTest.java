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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;

import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedConstruction;

/**
 * What the real plugin has stored while it runs, read from the database behind its back: a crash skips the save on
 * disable, so only what these tests find survives one.
 */
@NullMarked
class StorageDurabilityTest {

    private static final long ONE_MINUTE_TICKS = 60L * 20L;

    private ServerMock server;
    private MockedConstruction<BuildSystemMetrics> metrics;
    private BuildSystemPlugin plugin;

    @TempDir
    File worldContainer;

    @BeforeEach
    void setUp() {
        // MockBukkit has no world container and its worlds have no folder; creating a world needs both.
        server = MockBukkit.mock(new ServerMock() {
            @Override
            public File getWorldContainer() {
                return worldContainer;
            }

            @Override
            public @Nullable World createWorld(WorldCreator creator) {
                World created = super.createWorld(creator);
                if (created == null) {
                    return null;
                }
                World world = spy(created);
                doReturn(new File(worldContainer, creator.name())).when(world).getWorldFolder();
                return world;
            }
        });
        // bStats refuses to run unrelocated, which it only is in the shaded jar.
        metrics = mockConstruction(BuildSystemMetrics.class);
        plugin = MockBukkit.load(BuildSystemPlugin.class);
    }

    @AfterEach
    void tearDown() {
        metrics.close();
        MockBukkit.unmock();
    }

    @Test
    void aNewWorld_isStoredRightAway() throws SQLException {
        BuildWorld world = newWorld(null);

        flush();

        assertNotNull(row("buildsystem_worlds", world.getUniqueId()));
    }

    @Test
    void aNewFolder_isStoredRightAway() throws SQLException {
        Folder folder = newFolder();

        flush();

        assertNotNull(row("buildsystem_folders", folder.getUniqueId()));
    }

    @Test
    void aNewWorldInAFolder_isStoredInItRightAway() throws SQLException {
        Folder folder = newFolder();
        BuildWorld world = newWorld(folder);

        flush();

        String stored = Objects.requireNonNull(row("buildsystem_folders", folder.getUniqueId()));
        assertTrue(stored.contains(world.getUniqueId().toString()), stored);
    }

    @Test
    void aChangedSetting_isStoredWithinAMinute() throws SQLException {
        BuildWorld world = newWorld(null);
        flush();

        world.getData().set(WorldDataKey.PROJECT, "changed project");
        server.getScheduler().performTicks(ONE_MINUTE_TICKS);
        flush();

        String stored = Objects.requireNonNull(row("buildsystem_worlds", world.getUniqueId()));
        assertTrue(stored.contains("changed project"), stored);
    }

    private BuildWorld newWorld(@Nullable Folder folder) {
        BuildWorld world =
                api().getWorldService().importWorld("durable").folder(folder).build();
        return Objects.requireNonNull(world);
    }

    private Folder newFolder() {
        return api().getWorldService()
                .getFolderStorage()
                .createFolder(
                        "durable folder",
                        api().getNavigatorCategoryRegistry().getDefault(),
                        Builder.of(UUID.randomUUID(), "Creator"));
    }

    /**
     * Waits for every queued write: writes run in order, so an empty save lands after them.
     */
    private void flush() {
        api().getWorldService().getWorldStorage().save(List.<BuildWorld>of()).join();
        api().getWorldService().getFolderStorage().save(List.<Folder>of()).join();
    }

    private @Nullable String row(String table, UUID id) throws SQLException {
        try (Connection connection =
                        DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "storage.db"));
                PreparedStatement statement =
                        connection.prepareStatement("SELECT data FROM " + table + " WHERE id = ?")) {
            statement.setString(1, id.toString());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString(1) : null;
            }
        }
    }

    private BuildSystem api() {
        return Objects.requireNonNull(server.getServicesManager().load(BuildSystem.class));
    }
}
