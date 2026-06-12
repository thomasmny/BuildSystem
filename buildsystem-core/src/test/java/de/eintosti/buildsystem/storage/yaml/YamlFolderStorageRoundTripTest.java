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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Round-trip tests for {@link YamlFolderStorage}: a folder serialized and saved must deserialize back with all fields
 * intact, including parent references resolved in the second load pass.
 */
class YamlFolderStorageRoundTripTest {

    @TempDir
    File dataFolder;

    private BuildSystemPlugin plugin;
    private WorldStorage worldStorage;

    @BeforeEach
    void setUp() {
        plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        worldStorage = Mockito.mock(WorldStorage.class);
    }

    private YamlFolderStorage newStorage() {
        return new YamlFolderStorage(plugin, worldStorage);
    }

    private FolderImpl folder(String name, NavigatorCategory category, List<UUID> worlds) {
        Builder creator = Builder.of(UUID.randomUUID(), "FolderCreator");
        return new FolderImpl(
                plugin,
                name,
                1_700_000_000_000L,
                category,
                null,
                creator,
                XMaterial.CHEST,
                "perm.test",
                "ProjectX",
                worlds,
                new ArrayList<>());
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
        newStorage().save(folder("MyFolder", NavigatorCategory.PUBLIC, worlds)).join();

        Folder loaded = findByName(newStorage().load().join(), "MyFolder");
        assertEquals("MyFolder", loaded.getName());
        assertEquals(NavigatorCategory.PUBLIC, loaded.getCategory());
        assertEquals("perm.test", loaded.getPermission());
        assertEquals("ProjectX", loaded.getProject());
        assertEquals("FolderCreator", loaded.getCreator().getName());
        assertEquals(worlds, loaded.getWorldUUIDs());
    }

    @Test
    void roundTrip_emptyWorldList() {
        newStorage().save(folder("Empty", NavigatorCategory.ARCHIVE, List.of())).join();

        Folder loaded = findByName(newStorage().load().join(), "Empty");
        assertEquals(NavigatorCategory.ARCHIVE, loaded.getCategory());
        assertTrue(loaded.getWorldUUIDs().isEmpty());
    }

    @Test
    void roundTrip_resolvesParentReference() {
        FolderImpl parent = folder("Parent", NavigatorCategory.PUBLIC, List.of());
        FolderImpl child = folder("Child", NavigatorCategory.PUBLIC, List.of());
        child.setParent(parent);

        newStorage().save(List.of(parent, child)).join();

        Collection<Folder> loaded = newStorage().load().join();
        Folder loadedChild = findByName(loaded, "Child");
        assertNotNull(loadedChild.getParent());
        assertEquals("Parent", loadedChild.getParent().getName());
    }
}
