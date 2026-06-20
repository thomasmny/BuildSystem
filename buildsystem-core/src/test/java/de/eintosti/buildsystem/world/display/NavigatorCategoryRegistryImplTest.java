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
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import java.io.File;
import java.util.List;
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
        registry = new NavigatorCategoryRegistryImpl(plugin);
    }

    @Test
    void seedsThreeBuiltInCategories() {
        assertEquals(3, registry.getCategories().size());
        assertTrue(registry.getCategory(NavigatorCategoryRegistry.PUBLIC_ID).isPresent());
        assertTrue(registry.getCategory(NavigatorCategoryRegistry.PRIVATE_ID).isPresent());
        assertTrue(registry.getCategory(NavigatorCategoryRegistry.ARCHIVE_ID).isPresent());
    }

    @Test
    void defaultCategoryIsPublic() {
        assertEquals(
                NavigatorCategoryRegistry.PUBLIC_ID,
                registry.getDefaultCategory().getId());
    }

    @Test
    void privateCategoryGroupsAddedPlayers() {
        NavigatorCategory privateCategory =
                registry.getCategory(NavigatorCategoryRegistry.PRIVATE_ID).orElseThrow();
        assertTrue(privateCategory.getVisibilities().contains(Visibility.ADDED_PLAYERS));
        assertFalse(privateCategory.getVisibilities().contains(Visibility.EVERYONE));
    }

    @Test
    void createCategory_addsCustomCategory() {
        NavigatorCategory created = registry.createCategory("Administration");

        assertEquals("administration", created.getId());
        assertFalse(created.isBuiltIn());
        assertTrue(registry.getCategory("administration").isPresent());
    }

    @Test
    void deleteCategory_removesCustomCategory() {
        NavigatorCategory created = registry.createCategory("Temporary");

        assertTrue(registry.deleteCategory(created.getId()));
        assertFalse(registry.getCategory(created.getId()).isPresent());
    }

    @Test
    void deleteCategory_allowsBuiltIn() {
        assertTrue(registry.deleteCategory(NavigatorCategoryRegistry.PUBLIC_ID));
        assertFalse(registry.getCategory(NavigatorCategoryRegistry.PUBLIC_ID).isPresent());
    }

    @Test
    void deleteCategory_allowsDeletingEveryCategory() {
        for (NavigatorCategory category : List.copyOf(registry.getCategories())) {
            assertTrue(registry.deleteCategory(category.getId()));
        }
        assertEquals(0, registry.getCategories().size());
    }

    @Test
    void deleteCategory_returnsFalseForUnknownId() {
        assertFalse(registry.deleteCategory("does-not-exist"));
    }

    @Test
    void getDefaultCategory_reseedsBuiltInsWhenEmpty() {
        for (NavigatorCategory category : List.copyOf(registry.getCategories())) {
            registry.deleteCategory(category.getId());
        }
        assertEquals(0, registry.getCategories().size());

        // Folders always need a home category, so the default reseeds the built-ins on demand.
        assertEquals(
                NavigatorCategoryRegistry.PUBLIC_ID,
                registry.getDefaultCategory().getId());
        assertEquals(3, registry.getCategories().size());
    }

    @Test
    void resetToDefaults_restoresBuiltIns() {
        registry.deleteCategory(NavigatorCategoryRegistry.PUBLIC_ID);
        registry.resetToDefaults();
        assertEquals(3, registry.getCategories().size());
        assertTrue(registry.getCategory(NavigatorCategoryRegistry.PUBLIC_ID).isPresent());
    }

    @Test
    void addStatusToDefaultCategory_makesItReachable() {
        registry.addStatusToDefaultCategory("custom_status");

        assertTrue(registry.getDefaultCategory().getStatusIds().contains("custom_status"));
    }

    @Test
    void removeStatusFromCategories_clearsEverywhere() {
        registry.addStatusToDefaultCategory("custom_status");
        registry.removeStatusFromCategories("custom_status");

        assertFalse(registry.getDefaultCategory().getStatusIds().contains("custom_status"));
    }
}
