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
package de.eintosti.buildsystem.world.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * CRUD, built-in protection and status-membership behaviour of the {@link NavigatorCategoryRegistryImpl}, backed by a
 * temp-directory storage.
 */
class NavigatorCategoryRegistryImplTest {

    @TempDir
    File dataFolder;

    private NavigatorCategoryRegistryImpl registry;

    @BeforeEach
    void setUp() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        // The delete/reset cascades re-home the folder storage's folders; the mock has none, so they are no-ops.
        registry = new NavigatorCategoryRegistryImpl(plugin, mock(FolderStorageImpl.class));
    }

    @Test
    void seedsThreeBuiltInCategories() {
        assertEquals(3, registry.getAll().size());
        assertTrue(registry.get(NavigatorCategoryRegistry.PUBLIC_ID).isPresent());
        assertTrue(registry.get(NavigatorCategoryRegistry.PRIVATE_ID).isPresent());
        assertTrue(registry.get(NavigatorCategoryRegistry.ARCHIVE_ID).isPresent());
    }

    @Test
    void defaultCategoryIsPublic() {
        assertEquals(NavigatorCategoryRegistry.PUBLIC_ID, registry.getDefault().getId());
    }

    @Test
    void privateCategoryGroupsAddedPlayers() {
        NavigatorCategory privateCategory =
                registry.get(NavigatorCategoryRegistry.PRIVATE_ID).orElseThrow();
        assertTrue(privateCategory.getVisibilities().contains(Visibility.ADDED_PLAYERS));
        assertFalse(privateCategory.getVisibilities().contains(Visibility.EVERYONE));
    }

    @Test
    void createCategory_addsCustomCategory() {
        NavigatorCategory created = registry.create("Administration");

        assertEquals("administration", created.getId());
        assertFalse(created.isBuiltIn());
        assertTrue(registry.get("administration").isPresent());
    }

    @Test
    void deleteCategory_removesCustomCategory() {
        NavigatorCategory created = registry.create("Temporary");

        assertTrue(registry.delete(created.getId()));
        assertFalse(registry.get(created.getId()).isPresent());
    }

    @Test
    void deleteCategory_allowsBuiltIn() {
        assertTrue(registry.delete(NavigatorCategoryRegistry.PUBLIC_ID));
        assertFalse(registry.get(NavigatorCategoryRegistry.PUBLIC_ID).isPresent());
    }

    @Test
    void deleteCategory_allowsDeletingEveryCategory() {
        for (NavigatorCategory category : List.copyOf(registry.getAll())) {
            assertTrue(registry.delete(category.getId()));
        }
        assertEquals(0, registry.getAll().size());
    }

    @Test
    void deleteCategory_returnsFalseForUnknownId() {
        assertFalse(registry.delete("does-not-exist"));
    }

    @Test
    void getDefault_reseedsBuiltInsWhenEmpty() {
        for (NavigatorCategory category : List.copyOf(registry.getAll())) {
            registry.delete(category.getId());
        }
        assertEquals(0, registry.getAll().size());

        // Folders always need a home category, so the default reseeds the built-ins on demand.
        assertEquals(NavigatorCategoryRegistry.PUBLIC_ID, registry.getDefault().getId());
        assertEquals(3, registry.getAll().size());
    }

    @Test
    void resetToDefaults_restoresBuiltIns() {
        registry.delete(NavigatorCategoryRegistry.PUBLIC_ID);
        registry.resetToDefaults();
        assertEquals(3, registry.getAll().size());
        assertTrue(registry.get(NavigatorCategoryRegistry.PUBLIC_ID).isPresent());
    }

    @Test
    void resetToDefaults_reHomesFoldersOfDiscardedCategories() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        FolderStorageImpl folderStorage = mock(FolderStorageImpl.class);
        NavigatorCategoryRegistryImpl categoryRegistry = new NavigatorCategoryRegistryImpl(plugin, folderStorage);

        NavigatorCategory custom = categoryRegistry.create("Administration");
        FolderImpl folder = mock(FolderImpl.class);
        when(folder.getCategory()).thenReturn(custom);
        when(folderStorage.getFolders()).thenReturn(List.of(folder));

        categoryRegistry.resetToDefaults();

        // Without the cascade the folder keeps a category the registry no longer lists, so it renders in none of them.
        verify(folder).rehome(categoryRegistry.getDefault());
        verify(folderStorage).save(List.of(folder));
    }

    @Test
    void deletingTheDefault_keepsAFolderCategoryThatCouldNotBeResolved() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        FolderStorageImpl folderStorage = mock(FolderStorageImpl.class);
        NavigatorCategoryRegistryImpl categoryRegistry = new NavigatorCategoryRegistryImpl(plugin, folderStorage);
        NavigatorCategory oldDefault = categoryRegistry.getDefault();
        // Loaded with a category that no longer exists, so it shows the default.
        FolderImpl folder = FolderImpl.builder(TestData.worldContext(), UUID.randomUUID())
                .name("Lobbies")
                .creator(Builder.of(UUID.randomUUID(), "Alex"))
                .category(oldDefault)
                .build();
        folder.keepUnresolved("retired", null);
        when(folderStorage.getFolders()).thenReturn(List.of(folder));

        assertTrue(categoryRegistry.delete(oldDefault.getId()));

        assertEquals(categoryRegistry.getDefault(), folder.getCategory());
        assertEquals("retired", folder.getUnresolvedCategory());
    }

    @Test
    void addStatusToDefaultCategory_makesItReachable() {
        registry.addStatusToDefaultCategory("custom_status");

        assertTrue(registry.getDefault().getStatusIds().contains("custom_status"));
    }

    @Test
    void editedCategory_keepsEveryFieldAfterReload() {
        NavigatorCategoryImpl category = registry.create("Events");
        category.setDisplayName("Event Maps");
        category.setColor("&e");
        category.setIcon(Material.CAKE);
        category.setIconSkullTexture("texture");
        category.toggleVisibility(Visibility.ADDED_PLAYERS);
        category.addStatusId("finished");
        category.setSlot(22);
        category.setShown(false);
        registry.persist(category);
        registry.setSettingsSlot(8);

        NavigatorCategoryRegistryImpl reloaded = reload();
        NavigatorCategory loaded = reloaded.get(category.getId()).orElseThrow();
        assertEquals("Event Maps", loaded.getDisplayName());
        assertEquals("&e", loaded.getColor());
        assertEquals(Material.CAKE, loaded.getIcon());
        assertEquals("texture", loaded.getIconSkullTexture());
        assertEquals(Set.of(Visibility.EVERYONE, Visibility.ADDED_PLAYERS), loaded.getVisibilities());
        assertEquals(List.of("finished"), loaded.getStatusIds());
        assertEquals(22, loaded.getSlot());
        assertFalse(loaded.isShown());
        assertFalse(loaded.isBuiltIn());
        assertTrue(
                reloaded.get(NavigatorCategoryRegistry.PUBLIC_ID).orElseThrow().isBuiltIn());
        assertEquals(8, reloaded.getSettingsSlot());
    }

    private NavigatorCategoryRegistryImpl reload() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        return new NavigatorCategoryRegistryImpl(plugin, mock(FolderStorageImpl.class));
    }

    @Test
    void removeStatusFromCategories_clearsEverywhere() {
        registry.addStatusToDefaultCategory("custom_status");
        registry.removeStatusFromCategories("custom_status");

        assertFalse(registry.getDefault().getStatusIds().contains("custom_status"));
    }
}
