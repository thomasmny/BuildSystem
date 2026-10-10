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

import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@NullMarked
class PlaceholderApiExpansionTest {

    private static final String PLAYER_WORLD = "spawn";
    private static final String NAMED_WORLD = "test_world";

    private WorldStorageImpl worldStorage;
    private PlaceholderApiExpansion expansion;
    private Player player;
    private Settings settings;
    private BuildWorld playerWorld;
    private WorldData worldData;
    private BuildWorld namedWorld;

    @BeforeEach
    void setUp() {
        SettingsService settingsService = mock(SettingsService.class);
        worldStorage = mock(WorldStorageImpl.class);
        settings = mock(Settings.class);
        player = mock(Player.class);

        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn(PLAYER_WORLD);
        when(world.getKey()).thenReturn(NamespacedKey.minecraft(PLAYER_WORLD));
        when(settingsService.getSettings(player)).thenReturn(settings);

        playerWorld = buildWorld(PLAYER_WORLD);
        worldData = playerWorld.getData();
        namedWorld = buildWorld(NAMED_WORLD);
        when(worldStorage.getBuildWorld(PLAYER_WORLD)).thenReturn(playerWorld);
        when(worldStorage.matchWorlds(NAMED_WORLD)).thenReturn(List.of(namedWorld));

        expansion =
                new PlaceholderApiExpansion("TestAuthor", "1.0", settingsService, worldStorage, mock(Messages.class));
    }

    private static BuildWorld buildWorld(String name) {
        BuildWorld buildWorld = mock(BuildWorld.class);
        when(buildWorld.getName()).thenReturn(name);
        WorldData data = mock(WorldData.class);
        when(buildWorld.getData()).thenReturn(data);
        return buildWorld;
    }

    @FunctionalInterface
    private interface Stub {
        void apply(PlaceholderApiExpansionTest test);
    }

    /**
     * Each case stubs only the value its placeholder should read, so a placeholder reading another getter gets that
     * getter's default instead.
     */
    static Stream<Arguments> placeholders() {
        return Stream.of(
                Arguments.of(
                        "settings_navigatortype",
                        (Stub) t -> when(t.settings.getNavigatorType()).thenReturn(NavigatorType.NEW),
                        "NEW"),
                Arguments.of(
                        "settings_scoreboard",
                        (Stub) t -> when(t.settings.isScoreboard()).thenReturn(true),
                        "true"),
                Arguments.of(
                        "settings_noclip",
                        (Stub) t -> when(t.settings.isNoClip()).thenReturn(true),
                        "true"),
                Arguments.of(
                        "settings_nightvision",
                        (Stub) t -> when(t.settings.isNightVision()).thenReturn(true),
                        "true"),
                Arguments.of(
                        "settings_hideplayers",
                        (Stub) t -> when(t.settings.isHidePlayers()).thenReturn(true),
                        "true"),
                Arguments.of(
                        "loaded", (Stub) t -> when(t.playerWorld.isLoaded()).thenReturn(true), "true"),
                Arguments.of("world", (Stub) t -> {}, PLAYER_WORLD),
                Arguments.of(
                        "permission",
                        (Stub) t ->
                                when(t.worldData.get(WorldDataKey.PERMISSION)).thenReturn("buildsystem.world.test"),
                        "buildsystem.world.test"),
                Arguments.of(
                        "project",
                        (Stub) t -> when(t.worldData.get(WorldDataKey.PROJECT)).thenReturn("Lobby"),
                        "Lobby"),
                Arguments.of(
                        "time", (Stub) t -> when(t.playerWorld.getWorldTime()).thenReturn("Day"), "Day"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("placeholders")
    void placeholder_readsItsValue(String identifier, Stub stub, String expected) {
        stub.apply(this);
        assertEquals(expected, expansion.onPlaceholderRequest(player, identifier));
    }

    @Test
    void namedWorld_withUnderscoreInItsName_isLookedUpWhole() {
        when(namedWorld.getWorldTime()).thenReturn("Night");
        assertEquals("Night", expansion.onPlaceholderRequest(player, "time_" + NAMED_WORLD));
    }

    @Test
    void namedWorld_isReadInsteadOfThePlayersWorld() {
        when(playerWorld.isLoaded()).thenReturn(true);
        when(namedWorld.isLoaded()).thenReturn(false);

        assertEquals(PLAYER_WORLD, expansion.onPlaceholderRequest(player, "world"));
        assertEquals("true", expansion.onPlaceholderRequest(player, "loaded"));
        assertEquals(NAMED_WORLD, expansion.onPlaceholderRequest(player, "world_" + NAMED_WORLD));
        assertEquals("false", expansion.onPlaceholderRequest(player, "loaded_" + NAMED_WORLD));
    }

    @ParameterizedTest
    @ValueSource(strings = {"maps:lobby", "maps_lobby"})
    void namespacedWorld_isFoundByItsNameAndItsBukkitName(String typed) {
        BuildWorld lobby = buildWorld("maps:lobby");
        when(worldStorage.matchWorlds("maps:lobby")).thenReturn(List.of(lobby));
        when(worldStorage.getBuildWorlds()).thenReturn(List.of(playerWorld, namedWorld, lobby));

        assertEquals("maps:lobby", expansion.onPlaceholderRequest(player, "world_" + typed));
    }

    @Test
    void ambiguousWorldName_returnsDash() {
        BuildWorld mapsLobby = buildWorld("maps:lobby");
        BuildWorld eventsLobby = buildWorld("events:lobby");
        when(worldStorage.matchWorlds("lobby")).thenReturn(List.of(mapsLobby, eventsLobby));

        assertEquals("-", expansion.onPlaceholderRequest(player, "world_lobby"));
    }

    @Test
    void unknownWorld_returnsDash() {
        assertEquals("-", expansion.onPlaceholderRequest(player, "loaded_nowhere"));
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
