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
package de.eintosti.buildsystem.world.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldStatusRegistry;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * CRUD and built-in-protection behaviour of the {@link WorldStatusRegistryImpl}, backed by a temp-directory storage.
 */
class WorldStatusRegistryImplTest {

    @TempDir
    File dataFolder;

    private WorldStatusRegistryImpl registry;

    @BeforeEach
    void setUp() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        // The delete/reset cascades walk worldService.getWorldStorage()/getFolderStorage(); deep-stub mocks yield
        // empty collections so those cascades are no-ops rather than NPEs.
        NavigatorCategoryRegistryImpl categories =
                new NavigatorCategoryRegistryImpl(plugin, () -> mock(WorldServiceImpl.class, RETURNS_DEEP_STUBS));
        registry = new WorldStatusRegistryImpl(
                plugin,
                categories,
                mock(Messages.class, RETURNS_DEEP_STUBS),
                () -> mock(WorldServiceImpl.class, RETURNS_DEEP_STUBS));
    }

    @Test
    void seedsTheSixBuiltInStatuses() {
        assertEquals(6, registry.getAll().size());
        assertTrue(registry.get(WorldStatusRegistry.NOT_STARTED_ID).isPresent());
        assertTrue(registry.get(WorldStatusRegistry.ARCHIVE_ID).isPresent());
    }

    @Test
    void defaultStatusIsNotStarted() {
        assertEquals(WorldStatusRegistry.NOT_STARTED_ID, registry.getDefault().getId());
    }

    @Test
    void createStatus_addsCustomStatusWithUniqueId() {
        BuildWorldStatus created = registry.create("Needs Review");

        assertEquals("needs_review", created.getId());
        assertFalse(created.isBuiltIn());
        assertTrue(registry.get("needs_review").isPresent());
    }

    @Test
    void createStatus_twiceYieldsDistinctIds() {
        BuildWorldStatus first = registry.create("Review");
        BuildWorldStatus second = registry.create("Review");

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void deleteStatus_removesCustomStatus() {
        BuildWorldStatus created = registry.create("Temporary");

        assertTrue(registry.delete(created.getId()));
        assertFalse(registry.get(created.getId()).isPresent());
    }

    @Test
    void deleteStatus_allowsBuiltIn() {
        assertTrue(registry.delete(WorldStatusRegistry.NOT_STARTED_ID));
        assertFalse(registry.get(WorldStatusRegistry.NOT_STARTED_ID).isPresent());
    }

    @Test
    void deleteStatus_clearsDanglingProgressionTarget() {
        WorldStatusImpl source = registry.create("Source");
        WorldStatusImpl target = registry.create("Target");
        source.setProgressesTo(target.getId());
        registry.persist(source);

        assertTrue(registry.delete(target.getId()));

        BuildWorldStatus survivor = registry.get(source.getId()).orElseThrow();
        assertTrue(survivor.getProgressesTo().isEmpty(), "progressesTo should be cleared, not left dangling");
    }

    @Test
    void deleteStatus_refusesLastRemaining() {
        // Delete down to a single status; the final one must never be removable.
        for (BuildWorldStatus status : List.copyOf(registry.getAll())) {
            registry.delete(status.getId());
        }
        assertEquals(1, registry.getAll().size());
        String lastId = registry.getAll().iterator().next().getId();
        assertFalse(registry.delete(lastId));
    }

    @Test
    void resetToDefaults_restoresBuiltIns() {
        registry.delete(WorldStatusRegistry.NOT_STARTED_ID);
        registry.resetToDefaults();
        assertEquals(6, registry.getAll().size());
        assertTrue(registry.get(WorldStatusRegistry.NOT_STARTED_ID).isPresent());
    }

    @Test
    void createdStatus_survivesReload() {
        registry.create("Persisted");

        WorldStatusRegistryImpl reloaded = reloadRegistry();
        assertTrue(reloaded.get("persisted").isPresent());
    }

    @Test
    void seededStatuses_getDefaultPickerSlots() {
        BuildWorldStatus notStarted =
                registry.get(WorldStatusRegistry.NOT_STARTED_ID).orElseThrow();
        assertEquals(10, notStarted.getSlot());
        assertTrue(notStarted.isShown());
        assertEquals(15, registry.get("hidden").orElseThrow().getSlot());
    }

    @Test
    void createStatus_isPlacedInThePicker() {
        BuildWorldStatus created = registry.create("Needs Review");
        assertTrue(created.getSlot() >= 0);
        assertTrue(created.isShown());
    }

    @Test
    void resetStatusLayout_restoresBuiltInSlotsAndHidesCustom() {
        WorldStatusImpl notStarted = (WorldStatusImpl)
                registry.get(WorldStatusRegistry.NOT_STARTED_ID).orElseThrow();
        notStarted.setSlot(25);
        registry.persist(notStarted);
        WorldStatusImpl custom = registry.create("Custom");

        registry.resetLayout();

        assertEquals(
                10,
                registry.get(WorldStatusRegistry.NOT_STARTED_ID).orElseThrow().getSlot());
        assertFalse(registry.get(custom.getId()).orElseThrow().isShown());
    }

    @Test
    void unplacedStatus_isGivenASlotOnReload() {
        WorldStatusImpl notStarted = (WorldStatusImpl)
                registry.get(WorldStatusRegistry.NOT_STARTED_ID).orElseThrow();
        notStarted.setSlot(-1);
        registry.persist(notStarted);

        WorldStatusRegistryImpl reloaded = reloadRegistry();
        assertTrue(
                reloaded.get(WorldStatusRegistry.NOT_STARTED_ID).orElseThrow().getSlot() >= 0);
    }

    private WorldStatusRegistryImpl reloadRegistry() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        return new WorldStatusRegistryImpl(
                plugin,
                new NavigatorCategoryRegistryImpl(plugin, () -> mock(WorldServiceImpl.class)),
                mock(Messages.class, RETURNS_DEEP_STUBS),
                () -> mock(WorldServiceImpl.class));
    }
}
