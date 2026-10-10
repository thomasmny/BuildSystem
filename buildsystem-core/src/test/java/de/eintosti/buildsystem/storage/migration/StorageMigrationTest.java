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
package de.eintosti.buildsystem.storage.migration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.yaml.YamlFolderStorage;
import de.eintosti.buildsystem.storage.yaml.YamlWorldStorage;
import de.eintosti.buildsystem.test.TestData;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Gates the v3 → v4 on-disk migration that re-keys {@code worlds.yml}/{@code folders.yml} sections by UUID. Migration
 * runs transparently when a storage loads a legacy (name-keyed, version-less) file: every entity must survive, its name
 * must be preserved exactly, folder parent links must resolve, the file must be rewritten UUID-keyed with a {@code name}
 * field and stamped {@code version: 4}, a {@code .v3.bak} backup must be left behind, and a second load must be a no-op.
 */
class StorageMigrationTest {

    @TempDir
    File dataFolder;

    private BuildSystemPlugin plugin;
    private Services services;
    private WorldStorage worldStorage;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        services = TestData.mockServices();
        worldStorage = mock(WorldStorage.class);
    }

    private YamlConfiguration readFile(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(dataFolder, fileName));
    }

    private void writeV3Worlds(YamlConfiguration yaml) throws Exception {
        yaml.save(new File(dataFolder, "worlds.yml"));
    }

    // --- Worlds ---------------------------------------------------------------------------------------------------

    @Test
    void worlds_v3NameKeyed_migratedToUuidKeyedOnLoad() throws Exception {
        copyFixture("worlds.yml");
        UUID lobby = UUID.fromString("0e7a1aca-d915-4fb9-84b1-30f8b95cc373");

        Collection<BuildWorld> loaded =
                new YamlWorldStorage(plugin, services).load().join();

        BuildWorld world = loaded.stream()
                .filter(w -> w.getUniqueId().equals(lobby))
                .findFirst()
                .orElseThrow();
        assertEquals("lobby", world.getName());
        assertEquals("Hub", world.getData().get(WorldDataKey.PROJECT));

        YamlConfiguration onDisk = readFile("worlds.yml");
        assertEquals(StorageMigration.CURRENT_VERSION, onDisk.getInt("version"));
        // Every entry is re-keyed, including Old_Map-2019, whose generator cannot load without a running server.
        assertEquals(7, onDisk.getConfigurationSection("worlds").getKeys(false).size());
        assertEquals("lobby", onDisk.getString("worlds." + lobby + ".name"));
        assertFalse(onDisk.contains("worlds.lobby"));
        assertTrue(new File(dataFolder, "worlds.yml.v3.bak").exists());
    }

    @Test
    void worlds_garbledUuid_isRegeneratedNotDropped() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("worlds.Broken.uuid", "not-a-uuid");
        yaml.set("worlds.Broken.type", "NORMAL");
        yaml.set("worlds.Broken.date", 1L);
        yaml.set("worlds.Broken.data.status", "finished");
        writeV3Worlds(yaml);

        Collection<BuildWorld> loaded =
                new YamlWorldStorage(plugin, services).load().join();

        assertEquals(1, loaded.size());
        BuildWorld world = loaded.iterator().next();
        assertEquals("Broken", world.getName());
        assertNotNull(world.getUniqueId());

        YamlConfiguration onDisk = readFile("worlds.yml");
        Set<String> keys = onDisk.getConfigurationSection("worlds").getKeys(false);
        assertEquals(1, keys.size());
        assertEquals(world.getUniqueId().toString(), keys.iterator().next());
    }

    @Test
    void worlds_secondLoadIsIdempotent() throws Exception {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("worlds.Stable.uuid", uuid.toString());
        yaml.set("worlds.Stable.type", "NORMAL");
        yaml.set("worlds.Stable.date", 1L);
        yaml.set("worlds.Stable.data.status", "finished");
        writeV3Worlds(yaml);

        new YamlWorldStorage(plugin, services).load().join();
        // The backup captures the original v3 file; a second migration must not clobber it.
        String backupAfterFirst = readBackup();
        Collection<BuildWorld> second =
                new YamlWorldStorage(plugin, services).load().join();

        assertEquals(1, second.size());
        assertEquals("Stable", second.iterator().next().getName());
        YamlConfiguration onDisk = readFile("worlds.yml");
        assertEquals(StorageMigration.CURRENT_VERSION, onDisk.getInt("version"));
        assertEquals(
                Set.of(uuid.toString()),
                onDisk.getConfigurationSection("worlds").getKeys(false));
        assertEquals(backupAfterFirst, readBackup());
    }

    private String readBackup() throws Exception {
        return java.nio.file.Files.readString(new File(dataFolder, "worlds.yml.v3.bak").toPath());
    }

    @Test
    void worlds_renameOrphanWithSharedUuid_collapsesToLiveEntry() throws Exception {
        // A pre-4.0 rename saved the world under its new name but left the old name key orphaned, both carrying the
        // same UUID. Migration must collapse them into a single UUID-keyed section, keeping the later (live) name.
        UUID uuid = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("worlds.OldName.uuid", uuid.toString());
        yaml.set("worlds.OldName.type", "NORMAL");
        yaml.set("worlds.OldName.date", 1L);
        yaml.set("worlds.OldName.data.status", "finished");
        yaml.set("worlds.NewName.uuid", uuid.toString());
        yaml.set("worlds.NewName.type", "NORMAL");
        yaml.set("worlds.NewName.date", 1L);
        yaml.set("worlds.NewName.data.status", "finished");
        writeV3Worlds(yaml);

        Collection<BuildWorld> loaded =
                new YamlWorldStorage(plugin, services).load().join();

        assertEquals(1, loaded.size());
        assertEquals("NewName", loaded.iterator().next().getName());
        assertEquals(uuid, loaded.iterator().next().getUniqueId());
        YamlConfiguration onDisk = readFile("worlds.yml");
        assertEquals(
                Set.of(uuid.toString()),
                onDisk.getConfigurationSection("worlds").getKeys(false));
    }

    @Test
    void worlds_emptyV3File_isStampedWithoutBackup() throws Exception {
        new File(dataFolder, "worlds.yml").createNewFile();

        assertTrue(new YamlWorldStorage(plugin, services).load().join().isEmpty());

        assertEquals(StorageMigration.CURRENT_VERSION, readFile("worlds.yml").getInt("version"));
        assertFalse(new File(dataFolder, "worlds.yml.v3.bak").exists());
    }

    // --- Folders --------------------------------------------------------------------------------------------------

    @Test
    void folders_v3ParentByName_isRewrittenToUuidAndLinksResolve() throws Exception {
        copyFixture("folders.yml");

        Collection<Folder> loaded =
                new YamlFolderStorage(plugin, worldStorage, services).load().join();

        assertEquals(4, loaded.size());
        Folder archive = loaded.stream()
                .filter(f -> f.getName().equals("Archive"))
                .findFirst()
                .orElseThrow();
        assertNotNull(archive.getParent());
        assertEquals("Lobbies", archive.getParent().getName());
        assertEquals("Projects", archive.getParent().getParent().getName());

        YamlConfiguration onDisk = readFile("folders.yml");
        assertEquals(StorageMigration.CURRENT_VERSION, onDisk.getInt("version"));
        String archiveKey = archive.getUniqueId().toString();
        String lobbiesKey = archive.getParent().getUniqueId().toString();
        assertTrue(
                onDisk.getConfigurationSection("folders").getKeys(false).containsAll(Set.of(archiveKey, lobbiesKey)));
        assertEquals("Archive", onDisk.getString("folders." + archiveKey + ".name"));
        assertEquals(lobbiesKey, onDisk.getString("folders." + archiveKey + ".parent"));
        assertTrue(new File(dataFolder, "folders.yml.v3.bak").exists());
    }

    /**
     *
     * Copies a file BuildSystem 3.0.2 wrote (see {@code upgrade/README.md}) into the data folder.
     *
     */
    private void copyFixture(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/upgrade/3.0.2/" + fileName)) {
            Files.copy(in, new File(dataFolder, fileName).toPath());
        }
    }
}
