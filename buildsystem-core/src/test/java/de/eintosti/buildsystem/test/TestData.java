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
package de.eintosti.buildsystem.test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.world.data.WorldStatusImpl;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Shared test fixtures for the dynamic status/category registries. Provides the six built-in statuses and three
 * built-in categories as real {@link WorldStatusImpl}/{@link NavigatorCategoryImpl} instances (matching the production
 * seeds), plus helpers to wire a mocked plugin's registries to resolve them. Centralising this keeps every test that
 * needs a status or category from re-deriving the seed values.
 */
@NullMarked
public final class TestData {

    public static final WorldStatusImpl NOT_STARTED =
            status("not_started", "Not Started", "&c", 1, true, "in_progress");
    public static final WorldStatusImpl IN_PROGRESS = status("in_progress", "In Progress", "&6", 2, true, null);
    public static final WorldStatusImpl ALMOST_FINISHED =
            status("almost_finished", "Almost Finished", "&a", 3, true, null);
    public static final WorldStatusImpl FINISHED = status("finished", "Finished", "&2", 4, true, null);
    public static final WorldStatusImpl ARCHIVE_STATUS = status("archive", "Archive", "&3", 5, false, null);
    public static final WorldStatusImpl HIDDEN = status("hidden", "Hidden", "&7", 6, true, null);

    public static final List<WorldStatusImpl> STATUSES =
            List.of(NOT_STARTED, IN_PROGRESS, ALMOST_FINISHED, FINISHED, ARCHIVE_STATUS, HIDDEN);

    private static final List<String> ACTIVE_STATUS_IDS =
            List.of("not_started", "in_progress", "almost_finished", "finished");

    public static final NavigatorCategoryImpl PUBLIC = category(
            "public", "Worlds", "&b", XMaterial.FILLED_MAP, 11, EnumSet.of(Visibility.EVERYONE), ACTIVE_STATUS_IDS);
    public static final NavigatorCategoryImpl ARCHIVE = category(
            "archive",
            "Archive",
            "&3",
            XMaterial.CYAN_DYE,
            12,
            EnumSet.of(Visibility.EVERYONE, Visibility.ADDED_PLAYERS),
            List.of("archive"));
    public static final NavigatorCategoryImpl PRIVATE = category(
            "private",
            "Private",
            "&a",
            XMaterial.PLAYER_HEAD,
            13,
            EnumSet.of(Visibility.ADDED_PLAYERS),
            ACTIVE_STATUS_IDS);

    public static final List<NavigatorCategoryImpl> CATEGORIES = List.of(PUBLIC, ARCHIVE, PRIVATE);

    private TestData() {}

    private static WorldStatusImpl status(
            String id, String name, String color, int order, boolean building, String progressesTo) {
        return WorldStatusImpl.builder(id)
                .displayName(name)
                .color(color)
                .order(order)
                .buildingAllowed(building)
                .progressesTo(progressesTo)
                .builtIn(true)
                // Mirror the production default slots (not_started=10 .. hidden=15) so the picker lays out
                // contiguously.
                .statusSlot(9 + order)
                .shownInStatusMenu(true)
                .build();
    }

    private static NavigatorCategoryImpl category(
            String id,
            String name,
            String color,
            XMaterial icon,
            int slot,
            EnumSet<Visibility> visibilities,
            List<String> statusIds) {
        return NavigatorCategoryImpl.builder(id)
                .displayName(name)
                .color(color)
                .icon(icon)
                .visibilities(visibilities)
                .navigatorSlot(slot)
                .builtIn(true)
                .statusIds(statusIds)
                .build();
    }

    /**
     * Wires a mocked plugin's {@link WorldStatusRegistryImpl} to resolve the built-in statuses by id, defaulting to
     * {@link #NOT_STARTED}. Uses lenient stubbing so tests that never touch the registry do not fail strict-stub checks.
     *
     * @param plugin The mocked plugin to wire
     * @return The mocked registry, for further stubbing if needed
     */
    public static WorldStatusRegistryImpl stubStatusRegistry(BuildSystemPlugin plugin) {
        WorldStatusRegistryImpl registry = mock(WorldStatusRegistryImpl.class);
        lenient().when(registry.getStatuses()).thenReturn(List.copyOf(STATUSES));
        lenient().when(registry.getDefaultStatus()).thenReturn(NOT_STARTED);
        lenient().when(registry.getStatus(anyString())).thenAnswer(invocation -> byId(invocation.getArgument(0)));
        when(plugin.getWorldStatusRegistry()).thenReturn(registry);
        return registry;
    }

    /**
     * Wires a mocked plugin's {@link NavigatorCategoryRegistryImpl} to resolve the built-in categories by id, defaulting
     * to {@link #PUBLIC}.
     *
     * @param plugin The mocked plugin to wire
     * @return The mocked registry, for further stubbing if needed
     */
    public static NavigatorCategoryRegistryImpl stubCategoryRegistry(BuildSystemPlugin plugin) {
        NavigatorCategoryRegistryImpl registry = mock(NavigatorCategoryRegistryImpl.class);
        lenient().when(registry.getCategories()).thenReturn(List.copyOf(CATEGORIES));
        lenient().when(registry.getDefaultCategory()).thenReturn(PUBLIC);
        lenient()
                .when(registry.getCategory(anyString()))
                .thenAnswer(invocation -> categoryById(invocation.getArgument(0)));
        when(plugin.getNavigatorCategoryRegistry()).thenReturn(registry);
        return registry;
    }

    private static Optional<BuildWorldStatus> byId(String id) {
        return STATUSES.stream()
                .filter(status -> status.getId().equals(id))
                .map(s -> (BuildWorldStatus) s)
                .findFirst();
    }

    private static Optional<NavigatorCategory> categoryById(String id) {
        return CATEGORIES.stream()
                .filter(category -> category.getId().equals(id))
                .map(c -> (NavigatorCategory) c)
                .findFirst();
    }
}
