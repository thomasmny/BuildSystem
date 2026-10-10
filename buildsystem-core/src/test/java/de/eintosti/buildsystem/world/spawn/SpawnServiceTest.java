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
package de.eintosti.buildsystem.world.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The spawn is read while the plugin enables, before any build world is registered (worlds register a tick after
 * {@code WorldServiceImpl.init()}). These tests pin that a stored spawn survives that order, and that removing it is
 * written to disk.
 */
@NullMarked
class SpawnServiceTest {

    @TempDir
    File dataFolder;

    private BuildSystemPlugin plugin;
    private WorldServiceImpl worldService;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("SpawnServiceTest"));
        worldService = mock(WorldServiceImpl.class);
        // No build world is registered yet, just like during onEnable.
        when(worldService.getWorldStorage()).thenReturn(mock(WorldStorageImpl.class));
    }

    private SpawnService newService() {
        return new SpawnService(plugin, worldService, new TaskScheduler(mock(Plugin.class)));
    }

    private void writeSpawn(String value) throws IOException {
        Files.writeString(new File(dataFolder, "spawn.yml").toPath(), "spawn: " + value + "\n");
    }

    private YamlConfiguration readSpawnFile() {
        return YamlConfiguration.loadConfiguration(new File(dataFolder, "spawn.yml"));
    }

    @Test
    void storedSpawn_survivesLoadingBeforeWorldsAreRegistered() throws IOException {
        writeSpawn("lobby:10.5:64.0:-3.5:90.0:0.0");

        SpawnService service = newService();

        assertTrue(service.spawnExists());
        assertTrue(service.isIn("lobby"));
    }

    @Test
    void storedSpawn_inNamespacedWorld_keepsNamespace() throws IOException {
        writeSpawn("maps:lobby:1.0:2.0:3.0:0.0:0.0");

        SpawnService service = newService();

        assertTrue(service.isIn("maps:lobby"));
    }

    @Test
    void noStoredSpawn_doesNotExist() {
        SpawnService service = newService();

        assertFalse(service.spawnExists());
        assertFalse(service.isIn("lobby"));
    }

    @Test
    void savedSpawn_isReadBackByTheNextStart() {
        SpawnService service = newService();
        service.set(new Location(null, 1.5, 70, -2.5, 45f, 10f), "lobby");
        service.save().join();

        assertEquals("lobby:1.5:70.0:-2.5:45.0:10.0", readSpawnFile().getString("spawn"));
        assertTrue(newService().isIn("lobby"));
    }

    @Test
    void removedSpawn_isRemovedFromDisk() throws IOException {
        writeSpawn("lobby:10.5:64.0:-3.5:90.0:0.0");
        SpawnService service = newService();

        service.remove();
        service.save().join();

        assertFalse(service.spawnExists());
        assertNull(readSpawnFile().getString("spawn"));
        assertFalse(newService().spawnExists());
    }

    @Test
    void unparseableSpawn_isLeftOnDiskBySaving() throws IOException {
        writeSpawn("lobby:not-a-number");
        SpawnService service = newService();

        service.save().join();

        assertFalse(service.spawnExists());
        assertEquals("lobby:not-a-number", readSpawnFile().getString("spawn"));
    }

    @Test
    void isIn_comparesWorldNamesIgnoringCase() throws IOException {
        writeSpawn("Lobby:10.5:64.0:-3.5:90.0:0.0");
        SpawnService service = newService();

        assertTrue(service.isIn("lobby"));
        assertFalse(service.isIn("other"));
    }

    @Test
    void renameWorld_movesTheSpawnWithTheWorld() throws IOException {
        writeSpawn("lobby:10.5:64.0:-3.5:90.0:0.0");
        SpawnService service = newService();

        service.renameWorld("lobby", "hub");

        assertTrue(service.isIn("hub"));
        assertFalse(service.isIn("lobby"));
        service.renameWorld("other", "elsewhere");
        assertTrue(service.isIn("hub"));
    }
}
