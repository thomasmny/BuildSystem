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
package de.eintosti.buildsystem.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.UnsafeValues;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

@NullMarked
class WorldNamesTest {

    @TempDir
    Path tempDir;

    @Test
    void bareName_isInTheMinecraftNamespace() {
        assertEquals(NamespacedKey.MINECRAFT, WorldNames.namespace("Lobby"));
        assertEquals("Lobby", WorldNames.path("Lobby"));
        assertFalse(WorldNames.isNamespaced("Lobby"));
        assertFalse(WorldNames.isNamespaced("minecraft:Lobby"));
        assertEquals("minecraft:lobby", WorldNames.id("Lobby"));
    }

    @Test
    void namespacedName_splitsAtTheFirstSeparator() {
        assertEquals("maps", WorldNames.namespace("Maps:Lobby"));
        assertEquals("Lobby", WorldNames.path("Maps:Lobby"));
        assertTrue(WorldNames.isNamespaced("maps:Lobby"));
        assertEquals("maps:lobby", WorldNames.id("Maps:Lobby"));
    }

    @Test
    void normalize_dropsTheMinecraftPrefix() {
        assertEquals("Lobby", WorldNames.normalize("minecraft:Lobby"));
        assertEquals("maps:Lobby", WorldNames.normalize("MAPS:Lobby"));
        assertEquals("Lobby", WorldNames.normalize("Lobby"));
    }

    @Test
    void fromInput_placesBareNamesInTheDefaultNamespace() {
        assertEquals("maps:lobby", WorldNames.fromInput("lobby", "maps"));
        assertEquals("lobby", WorldNames.fromInput("lobby", NamespacedKey.MINECRAFT));
        assertEquals("lobby", WorldNames.fromInput("minecraft:lobby", "maps"));
        assertEquals("events:lobby", WorldNames.fromInput("events:lobby", "maps"));
    }

    @Test
    void qualified_spellsOutEveryNamespace() {
        assertEquals("minecraft:Lobby", WorldNames.qualified("Lobby"));
        assertEquals("maps:Lobby", WorldNames.qualified("maps:Lobby"));
        assertTrue(WorldNames.isQualified("minecraft:Lobby"));
        assertFalse(WorldNames.isQualified("Lobby"));
    }

    @Test
    void isValidNamespace_rejectsWhatCannotBeAFolder() {
        assertTrue(WorldNames.isValidNamespace("maps"));
        assertTrue(WorldNames.isValidNamespace("my_maps-2.0"));
        assertFalse(WorldNames.isValidNamespace(""));
        assertFalse(WorldNames.isValidNamespace("."));
        assertFalse(WorldNames.isValidNamespace(".."));
        assertFalse(WorldNames.isValidNamespace("Maps"));
        assertFalse(WorldNames.isValidNamespace("a/b"));
    }

    @Test
    void isValidName_rejectsNamesThatDoNotReadBackOrCannotBeKeyed() {
        assertTrue(WorldNames.isValidName("Lobby"));
        assertTrue(WorldNames.isValidName("my world"));
        assertTrue(WorldNames.isValidName("maps:Lobby"));
        assertFalse(WorldNames.isValidName("minecraft:a:b"));
        assertFalse(WorldNames.isValidName("maps:a:b"));
        assertFalse(WorldNames.isValidName("maps:my world"));
    }

    @Test
    void bukkitName_followsPapersNaming() {
        assertEquals("Lobby", WorldNames.bukkitName("Lobby"));
        assertEquals("maps_lobby", WorldNames.bukkitName("maps:Lobby"));
    }

    @Test
    void ofWorld_usesTheKeyOnlyOutsideMinecraft() {
        World namespaced = mock(World.class);
        when(namespaced.getName()).thenReturn("maps_lobby");
        when(namespaced.getKey()).thenReturn(new NamespacedKey("maps", "lobby"));
        assertEquals("maps:lobby", WorldNames.of(namespaced));

        World main = mock(World.class);
        when(main.getName()).thenReturn("world");
        when(main.getKey()).thenReturn(NamespacedKey.minecraft("overworld"));
        assertEquals("world", WorldNames.of(main));
    }

    @Test
    void creator_namespacedWorld_isKeyed() {
        // The test classpath carries paper-api, so the keyed constructor is available.
        assertTrue(WorldNames.namespacesSupported());

        WorldCreator creator = WorldNames.creator("maps:Lobby");

        assertEquals(new NamespacedKey("maps", "lobby"), creator.key());
    }

    @Test
    void creator_worldStoredUnderItsBukkitName_usesTheLoadedWorldsKey() {
        World loaded = mock(World.class);
        when(loaded.getName()).thenReturn("maps_lobby");
        when(loaded.getKey()).thenReturn(new NamespacedKey("maps", "lobby"));
        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            bukkit.when(() -> Bukkit.getWorld("maps_lobby")).thenReturn(loaded);

            WorldCreator creator = WorldNames.creator("maps_lobby");

            assertEquals(new NamespacedKey("maps", "lobby"), creator.key());
            assertEquals("maps_lobby", creator.name());
        }
    }

    @Test
    void creator_worldReallyNamedLikeABukkitName_staysInMinecraft() throws IOException {
        // maps:lobby is loaded by another plugin, but this world has a folder of its own: it is not that world.
        Files.createDirectories(tempDir.resolve("world/dimensions/minecraft/maps_lobby"));
        World loaded = mock(World.class);
        when(loaded.getName()).thenReturn("maps_lobby");
        when(loaded.getKey()).thenReturn(new NamespacedKey("maps", "lobby"));
        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            bukkit.when(() -> Bukkit.getWorld("maps_lobby")).thenReturn(loaded);

            assertEquals(
                    NamespacedKey.minecraft("maps_lobby"),
                    WorldNames.creator("maps_lobby").key());
        }
    }

    /**
     * A running server with {@code level-name=world} whose container is the temp dir.
     */
    private MockedStatic<Bukkit> mockServer() {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(tempDir.toFile());
        World main = mock(World.class);
        when(main.getName()).thenReturn("world");
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(main));
        UnsafeValues unsafe = mock(UnsafeValues.class);
        when(unsafe.getMainLevelName()).thenReturn("world");
        bukkit.when(Bukkit::getUnsafe).thenReturn(unsafe);
        return bukkit;
    }

    @Test
    void creator_invalidKey_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> WorldNames.creator("maps:not valid"));
    }
}
