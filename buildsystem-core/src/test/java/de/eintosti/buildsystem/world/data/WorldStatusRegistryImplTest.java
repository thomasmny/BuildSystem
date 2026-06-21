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
        NavigatorCategoryRegistryImpl categories = new NavigatorCategoryRegistryImpl(plugin, plugin::getWorldService);
        registry = new WorldStatusRegistryImpl(plugin, categories, plugin.getMessages(), plugin::getWorldService);
    }

    @Test
    void seedsTheSixBuiltInStatuses() {
        assertEquals(6, registry.getStatuses().size());
        assertTrue(registry.getStatus(WorldStatusRegistry.NOT_STARTED_ID).isPresent());
        assertTrue(registry.getStatus(WorldStatusRegistry.ARCHIVE_ID).isPresent());
    }

    @Test
    void defaultStatusIsNotStarted() {
        assertEquals(
                WorldStatusRegistry.NOT_STARTED_ID, registry.getDefaultStatus().getId());
    }

    @Test
    void createStatus_addsCustomStatusWithUniqueId() {
        BuildWorldStatus created = registry.createStatus("Needs Review");

        assertEquals("needs_review", created.getId());
        assertFalse(created.isBuiltIn());
        assertTrue(registry.getStatus("needs_review").isPresent());
    }

    @Test
    void createStatus_twiceYieldsDistinctIds() {
        BuildWorldStatus first = registry.createStatus("Review");
        BuildWorldStatus second = registry.createStatus("Review");

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void deleteStatus_removesCustomStatus() {
        BuildWorldStatus created = registry.createStatus("Temporary");

        assertTrue(registry.deleteStatus(created.getId()));
        assertFalse(registry.getStatus(created.getId()).isPresent());
    }

    @Test
    void deleteStatus_allowsBuiltIn() {
        assertTrue(registry.deleteStatus(WorldStatusRegistry.NOT_STARTED_ID));
        assertFalse(registry.getStatus(WorldStatusRegistry.NOT_STARTED_ID).isPresent());
    }

    @Test
    void deleteStatus_clearsDanglingProgressionTarget() {
        WorldStatusImpl source = registry.createStatus("Source");
        WorldStatusImpl target = registry.createStatus("Target");
        source.setProgressesTo(target.getId());
        registry.persist(source);

        assertTrue(registry.deleteStatus(target.getId()));

        BuildWorldStatus survivor = registry.getStatus(source.getId()).orElseThrow();
        assertTrue(survivor.getProgressesTo().isEmpty(), "progressesTo should be cleared, not left dangling");
    }

    @Test
    void deleteStatus_refusesLastRemaining() {
        // Delete down to a single status; the final one must never be removable.
        for (BuildWorldStatus status : List.copyOf(registry.getStatuses())) {
            registry.deleteStatus(status.getId());
        }
        assertEquals(1, registry.getStatuses().size());
        String lastId = registry.getStatuses().iterator().next().getId();
        assertFalse(registry.deleteStatus(lastId));
    }

    @Test
    void resetToDefaults_restoresBuiltIns() {
        registry.deleteStatus(WorldStatusRegistry.NOT_STARTED_ID);
        registry.resetToDefaults();
        assertEquals(6, registry.getStatuses().size());
        assertTrue(registry.getStatus(WorldStatusRegistry.NOT_STARTED_ID).isPresent());
    }

    @Test
    void createdStatus_survivesReload() {
        registry.createStatus("Persisted");

        WorldStatusRegistryImpl reloaded = reloadRegistry();
        assertTrue(reloaded.getStatus("persisted").isPresent());
    }

    @Test
    void seededStatuses_getDefaultPickerSlots() {
        BuildWorldStatus notStarted =
                registry.getStatus(WorldStatusRegistry.NOT_STARTED_ID).orElseThrow();
        assertEquals(10, notStarted.getStatusSlot());
        assertTrue(notStarted.isShownInStatusMenu());
        assertEquals(15, registry.getStatus("hidden").orElseThrow().getStatusSlot());
    }

    @Test
    void createStatus_isPlacedInThePicker() {
        BuildWorldStatus created = registry.createStatus("Needs Review");
        assertTrue(created.getStatusSlot() >= 0);
        assertTrue(created.isShownInStatusMenu());
    }

    @Test
    void resetStatusLayout_restoresBuiltInSlotsAndHidesCustom() {
        WorldStatusImpl notStarted = (WorldStatusImpl)
                registry.getStatus(WorldStatusRegistry.NOT_STARTED_ID).orElseThrow();
        notStarted.setStatusSlot(25);
        registry.persist(notStarted);
        WorldStatusImpl custom = registry.createStatus("Custom");

        registry.resetStatusLayout();

        assertEquals(
                10,
                registry.getStatus(WorldStatusRegistry.NOT_STARTED_ID)
                        .orElseThrow()
                        .getStatusSlot());
        assertFalse(registry.getStatus(custom.getId()).orElseThrow().isShownInStatusMenu());
    }

    @Test
    void unplacedStatus_isGivenASlotOnReload() {
        WorldStatusImpl notStarted = (WorldStatusImpl)
                registry.getStatus(WorldStatusRegistry.NOT_STARTED_ID).orElseThrow();
        notStarted.setStatusSlot(-1);
        registry.persist(notStarted);

        WorldStatusRegistryImpl reloaded = reloadRegistry();
        assertTrue(reloaded.getStatus(WorldStatusRegistry.NOT_STARTED_ID)
                        .orElseThrow()
                        .getStatusSlot()
                >= 0);
    }

    private WorldStatusRegistryImpl reloadRegistry() {
        BuildSystemPlugin plugin = mock(BuildSystemPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        return new WorldStatusRegistryImpl(
                plugin,
                new NavigatorCategoryRegistryImpl(plugin, plugin::getWorldService),
                plugin.getMessages(),
                plugin::getWorldService);
    }
}
