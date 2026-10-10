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
package de.eintosti.buildsystem.storage.yaml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

/**
 * Round-trip tests for {@link FolderStorageImpl}: a folder serialized and saved must deserialize back with all fields
 * intact, including parent references resolved in the second load pass.
 */
class YamlFolderStorageRoundTripTest {

    @TempDir
    File dataFolder;

    private BuildSystemPlugin plugin;
    private Services services;
    private WorldContext context;
    private WorldStorage worldStorage;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        services = TestData.mockServices();
        context = services.worldContext();
        worldStorage = Mockito.mock(WorldStorage.class);
    }

    private FolderStorageImpl newStorage() {
        return new FolderStorageImpl(plugin, worldStorage, services);
    }

    private FolderImpl folder(String name, NavigatorCategory category, List<UUID> worlds) {
        Builder creator = Builder.of(UUID.randomUUID(), "FolderCreator");
        return FolderImpl.builder(context, UUID.randomUUID())
                .name(name)
                .creation(1_700_000_000_000L)
                .category(category)
                .creator(creator)
                .permission("perm.test")
                .project("ProjectX")
                .worlds(worlds)
                .build();
    }

    private Folder findByName(Collection<Folder> folders, String name) {
        return folders.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void roundTrip_preservesFields() {
        List<UUID> worlds = List.of(UUID.randomUUID(), UUID.randomUUID());
        Folder original = folder("MyFolder", TestData.PUBLIC, worlds);
        newStorage().save(original).join();

        Folder loaded = findByName(newStorage().load().join(), "MyFolder");
        assertEquals(original.getUniqueId(), loaded.getUniqueId());
        assertEquals("MyFolder", loaded.getName());
        assertEquals(TestData.PUBLIC, loaded.getCategory());
        assertEquals("perm.test", loaded.getPermission());
        assertEquals("ProjectX", loaded.getProject());
        assertEquals("FolderCreator", loaded.getCreator().getName());
        assertEquals(worlds, loaded.getWorldUUIDs());
    }

    @Test
    void load_legacyFolderWithoutUuid_getsGeneratedId() throws Exception {
        YamlConfiguration legacy = new YamlConfiguration();
        String path = "folders.Legacy";
        legacy.set(path + ".creator", Builder.of(UUID.randomUUID(), "Creator").toString());
        legacy.set(path + ".creation", 1_700_000_000_000L);
        legacy.set(path + ".category", TestData.PUBLIC.getId());
        legacy.set(path + ".material", Material.CHEST.name());
        legacy.set(path + ".permission", "-");
        legacy.set(path + ".project", "-");
        legacy.set(path + ".worlds", List.of());
        legacy.save(new File(dataFolder, "folders.yml"));

        Folder loaded = findByName(newStorage().load().join(), "Legacy");
        assertNotNull(loaded.getUniqueId());
    }

    @Test
    void removedFolder_isGoneAfterARestart() {
        FolderImpl kept = folder("Kept", TestData.PUBLIC, List.of());
        FolderImpl removed = folder("Removed", TestData.PUBLIC, List.of());
        newStorage().save(List.of(kept, removed)).join();

        FolderStorageImpl storage = new FolderStorageImpl(plugin, worldStorage, services) {
            @Override
            protected void fireEvent(Event event) {}
        };
        storage.loadFolders();
        storage.removeFolder("Removed");
        // Queued behind the delete, so joining it means the delete has been written too.
        storage.save(storage.getFolders()).join();

        Collection<Folder> afterRestart = newStorage().load().join();
        assertEquals(List.of("Kept"), afterRestart.stream().map(Folder::getName).toList());
    }

    @Test
    void deleteRightAfterSave_isNeverUndoneByTheSave() {
        FolderStorageImpl storage = newStorage();
        for (int i = 0; i < 50; i++) {
            FolderImpl folder = folder("Folder" + i, TestData.PUBLIC, List.of());
            storage.save(folder);
            storage.delete(folder);
        }
        storage.save(List.of()).join();

        assertTrue(newStorage().load().join().isEmpty());
    }

    @Test
    void roundTrip_emptyWorldList() {
        newStorage().save(folder("Empty", TestData.ARCHIVE, List.of())).join();

        Folder loaded = findByName(newStorage().load().join(), "Empty");
        assertEquals(TestData.ARCHIVE, loaded.getCategory());
        assertTrue(loaded.getWorldUUIDs().isEmpty());
    }

    @Test
    void roundTrip_resolvesParentReference() {
        FolderImpl parent = folder("Parent", TestData.PUBLIC, List.of());
        FolderImpl child = folder("Child", TestData.PUBLIC, List.of());
        child.setParent(parent);

        newStorage().save(List.of(parent, child)).join();

        Collection<Folder> loaded = newStorage().load().join();
        Folder loadedChild = findByName(loaded, "Child");
        assertNotNull(loadedChild.getParent());
        assertEquals("Parent", loadedChild.getParent().getName());
    }
}
