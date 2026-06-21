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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.data.WorldStatusImpl;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.display.NavigatorCategoryImpl;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
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
    public static WorldStatusRegistryImpl statusRegistry() {
        WorldStatusRegistryImpl registry = mock(WorldStatusRegistryImpl.class);
        lenient().when(registry.getStatuses()).thenReturn(List.copyOf(STATUSES));
        lenient().when(registry.getDefaultStatus()).thenReturn(NOT_STARTED);
        lenient().when(registry.getStatus(anyString())).thenAnswer(invocation -> byId(invocation.getArgument(0)));
        return registry;
    }

    /**
     * Wires a mocked plugin's {@link NavigatorCategoryRegistryImpl} to resolve the built-in categories by id, defaulting
     * to {@link #PUBLIC}.
     *
     * @param plugin The mocked plugin to wire
     * @return The mocked registry, for further stubbing if needed
     */
    public static NavigatorCategoryRegistryImpl categoryRegistry() {
        NavigatorCategoryRegistryImpl registry = mock(NavigatorCategoryRegistryImpl.class);
        lenient().when(registry.getCategories()).thenReturn(List.copyOf(CATEGORIES));
        lenient().when(registry.getDefaultCategory()).thenReturn(PUBLIC);
        lenient()
                .when(registry.getCategory(anyString()))
                .thenAnswer(invocation -> categoryById(invocation.getArgument(0)));
        return registry;
    }

    /**
     * A {@link WorldContext} wired with mocked collaborators, for tests that build {@link BuildWorldImpl}/
     * {@code FolderImpl} or a codec/storage. The config returns a valid unload time so world construction does not
     * throw, and the status registry resolves the built-in statuses.
     *
     * @return A ready-to-use mocked context
     */
    public static WorldContext worldContext() {
        ConfigService configService = mock(ConfigService.class, RETURNS_DEEP_STUBS);
        lenient()
                .when(configService.current().world().unload().timeUntilUnload())
                .thenReturn("06:00:00");
        return new WorldContext(
                mock(Messages.class, RETURNS_DEEP_STUBS),
                mock(MenuItems.class),
                configService,
                mock(PlayerServiceImpl.class),
                mock(SpawnService.class),
                statusRegistry(),
                mock(CustomizableIcons.class),
                new TaskScheduler(mock(Plugin.class)),
                Logger.getLogger("BuildSystemTest"));
    }

    /**
     * A mocked {@link Services} registry wired with the {@link #worldContext()} and the collaborators the storages and
     * world service resolve from it, so a test can construct those without standing up the real service graph.
     *
     * @return A ready-to-use mocked registry
     */
    public static Services mockServices() {
        // Build every collaborator (some of which stub themselves) into locals first: creating or stubbing a mock
        // inside a thenReturn(...) argument, while the outer when(...) is still open, dangles that nested stubbing and
        // trips Mockito's UnfinishedStubbingException.
        WorldContext context = worldContext();
        NavigatorCategoryRegistryImpl categoryRegistry = categoryRegistry();
        PlayerLookupService playerLookup = mock(PlayerLookupService.class);
        Prompts prompts = mock(Prompts.class);
        Services services = mock(Services.class);
        lenient().when(services.worldContext()).thenReturn(context);
        lenient().when(services.scheduler()).thenReturn(context.scheduler());
        lenient().when(services.config()).thenReturn(context.configService());
        lenient().when(services.messages()).thenReturn(context.messages());
        lenient().when(services.player()).thenReturn(context.playerService());
        lenient().when(services.spawn()).thenReturn(context.spawnService());
        lenient().when(services.worldStatusRegistry()).thenReturn((WorldStatusRegistryImpl) context.statusRegistry());
        lenient().when(services.navigatorCategoryRegistry()).thenReturn(categoryRegistry);
        lenient().when(services.playerLookup()).thenReturn(playerLookup);
        lenient().when(services.prompts()).thenReturn(prompts);
        return services;
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
