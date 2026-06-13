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
package de.eintosti.buildsystem.integration.placeholderapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.data.Type;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
class PlaceholderApiExpansionTest {

    private static final String WORLD_NAME = "test_world";

    private PlaceholderApiExpansion expansion;
    private Player player;
    private Settings settings;
    private BuildWorld buildWorld;
    private WorldData worldData;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        WorldStorageImpl worldStorage = mock(WorldStorageImpl.class);
        Messages messages = mock(Messages.class);

        settings = mock(Settings.class);
        buildWorld = mock(BuildWorld.class);
        worldData = mock(WorldData.class);
        Builders builders = mock(Builders.class);

        World world = mock(World.class);
        player = mock(Player.class);

        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn(WORLD_NAME);
        when(settingsService.getSettings(player)).thenReturn(settings);
        when(worldStorage.getBuildWorld(WORLD_NAME)).thenReturn(buildWorld);
        when(buildWorld.getData()).thenReturn(worldData);
        when(buildWorld.getBuilders()).thenReturn(builders);

        expansion = new PlaceholderApiExpansion("TestAuthor", "1.0", settingsService, worldStorage, messages);
    }

    // --- Settings placeholders ---

    @Test
    void settings_navigatortype_returnsValue() {
        when(settings.getNavigatorType()).thenReturn(NavigatorType.NEW);
        assertEquals("NEW", expansion.onPlaceholderRequest(player, "settings_navigatortype"));
    }

    @Test
    void settings_scoreboard_returnsValue() {
        when(settings.isScoreboard()).thenReturn(true);
        assertEquals("true", expansion.onPlaceholderRequest(player, "settings_scoreboard"));
    }

    @Test
    void settings_noclip_returnsValue() {
        when(settings.isNoClip()).thenReturn(false);
        assertEquals("false", expansion.onPlaceholderRequest(player, "settings_noclip"));
    }

    @Test
    void settings_nightvision_returnsValue() {
        when(settings.isNightVision()).thenReturn(true);
        assertEquals("true", expansion.onPlaceholderRequest(player, "settings_nightvision"));
    }

    @Test
    void settings_hideplayers_returnsValue() {
        when(settings.isHidePlayers()).thenReturn(false);
        assertEquals("false", expansion.onPlaceholderRequest(player, "settings_hideplayers"));
    }

    // --- World placeholders ---

    @Test
    void world_loaded_returnsValue() {
        when(buildWorld.isLoaded()).thenReturn(true);
        assertEquals("true", expansion.onPlaceholderRequest(player, "loaded"));
    }

    @Test
    void world_world_returnsName() {
        when(buildWorld.getName()).thenReturn(WORLD_NAME);
        assertEquals(WORLD_NAME, expansion.onPlaceholderRequest(player, "world"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void world_permission_returnsValue() {
        Type<String> attr = mock(Type.class);
        when(worldData.permission()).thenReturn(attr);
        when(attr.get()).thenReturn("buildsystem.world.test");
        assertEquals("buildsystem.world.test", expansion.onPlaceholderRequest(player, "permission"));
    }

    @Test
    void world_time_returnsValue() {
        when(buildWorld.getWorldTime()).thenReturn("Day");
        assertEquals("Day", expansion.onPlaceholderRequest(player, "time"));
    }

    @Test
    void world_notPresent_returnsDash() {
        WorldStorageImpl emptyStorage = mock(WorldStorageImpl.class);
        when(emptyStorage.getBuildWorld(WORLD_NAME)).thenReturn(null);
        PlaceholderApiExpansion noWorld =
                new PlaceholderApiExpansion("a", "1", mock(SettingsService.class), emptyStorage, mock(Messages.class));
        assertEquals("-", noWorld.onPlaceholderRequest(player, "loaded"));
    }

    @Test
    void nullPlayer_returnsEmptyString() {
        assertEquals("", expansion.onPlaceholderRequest(null, "settings_scoreboard"));
    }

    @Test
    void unknownSettingsIdentifier_returnsNull() {
        assertNull(expansion.onPlaceholderRequest(player, "settings_unknownkey"));
    }

    @Test
    void unknownWorldIdentifier_returnsNull() {
        assertNull(expansion.onPlaceholderRequest(player, "unknownworldkey"));
    }
}
