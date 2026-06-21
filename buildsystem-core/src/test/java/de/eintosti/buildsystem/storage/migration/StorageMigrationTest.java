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
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.yaml.YamlFolderStorage;
import de.eintosti.buildsystem.storage.yaml.YamlWorldStorage;
import de.eintosti.buildsystem.test.TestData;
import java.io.File;
import java.util.Collection;
import java.util.List;
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
        UUID uuid = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("worlds.MyWorld.uuid", uuid.toString());
        yaml.set("worlds.MyWorld.type", "NORMAL");
        yaml.set("worlds.MyWorld.date", 1_700_000_000_000L);
        yaml.set("worlds.MyWorld.data.status", "finished");
        yaml.set("worlds.MyWorld.data.permission", "build.test");
        writeV3Worlds(yaml);

        Collection<BuildWorld> loaded =
                new YamlWorldStorage(plugin, services).load().join();

        assertEquals(1, loaded.size());
        BuildWorld world = loaded.iterator().next();
        assertEquals("MyWorld", world.getName());
        assertEquals(uuid, world.getUniqueId());
        assertEquals("build.test", world.getData().get(WorldDataKey.PERMISSION));

        YamlConfiguration onDisk = readFile("worlds.yml");
        assertEquals(StorageMigration.CURRENT_VERSION, onDisk.getInt("version"));
        assertEquals(
                Set.of(uuid.toString()),
                onDisk.getConfigurationSection("worlds").getKeys(false));
        assertEquals("MyWorld", onDisk.getString("worlds." + uuid + ".name"));
        assertFalse(onDisk.contains("worlds.MyWorld"));
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
        UUID parentUuid = UUID.randomUUID();
        UUID childUuid = UUID.randomUUID();
        Builder creator = Builder.of(UUID.randomUUID(), "FC");
        YamlConfiguration yaml = new YamlConfiguration();
        writeV3Folder(yaml, "Parent", parentUuid, creator, null);
        writeV3Folder(yaml, "Child", childUuid, creator, "Parent");
        yaml.save(new File(dataFolder, "folders.yml"));

        Collection<Folder> loaded =
                new YamlFolderStorage(plugin, worldStorage, services).load().join();

        Folder child = loaded.stream()
                .filter(f -> f.getName().equals("Child"))
                .findFirst()
                .orElseThrow();
        assertNotNull(child.getParent());
        assertEquals("Parent", child.getParent().getName());

        YamlConfiguration onDisk = readFile("folders.yml");
        assertEquals(StorageMigration.CURRENT_VERSION, onDisk.getInt("version"));
        assertEquals(
                Set.of(parentUuid.toString(), childUuid.toString()),
                onDisk.getConfigurationSection("folders").getKeys(false));
        assertEquals("Child", onDisk.getString("folders." + childUuid + ".name"));
        assertEquals(parentUuid.toString(), onDisk.getString("folders." + childUuid + ".parent"));
        assertTrue(new File(dataFolder, "folders.yml.v3.bak").exists());
    }

    private void writeV3Folder(YamlConfiguration yaml, String name, UUID uuid, Builder creator, String parentName) {
        String path = "folders." + name;
        yaml.set(path + ".uuid", uuid.toString());
        yaml.set(path + ".creator", creator.toString());
        yaml.set(path + ".creation", 1_700_000_000_000L);
        yaml.set(path + ".category", TestData.PUBLIC.getId());
        yaml.set(path + ".material", "CHEST");
        yaml.set(path + ".permission", "-");
        yaml.set(path + ".project", "-");
        yaml.set(path + ".worlds", List.of());
        if (parentName != null) {
            yaml.set(path + ".parent", parentName);
        }
    }
}
