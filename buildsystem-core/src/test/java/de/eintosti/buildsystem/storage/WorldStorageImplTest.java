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
package de.eintosti.buildsystem.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.test.TestData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

@NullMarked
class WorldStorageImplTest {

    @TempDir
    Path tempDir;

    private String defaultNamespace = NamespacedKey.MINECRAFT;
    private WorldStorageImpl storage;

    @BeforeEach
    void setUp() {
        storage =
                new WorldStorageImpl(Logger.getLogger("test"), () -> defaultNamespace, TestData.noopEntityCollection());
    }

    private BuildWorld world(String name) {
        BuildWorld w = mock(BuildWorld.class);
        when(w.getUniqueId()).thenReturn(UUID.randomUUID());
        when(w.getName()).thenReturn(name);
        when(w.getFolder()).thenReturn(null);
        return w;
    }

    @Test
    void addAndLookupByUuid() {
        BuildWorld w = world("Alpha");
        storage.addBuildWorld(w);
        assertEquals(w, storage.getBuildWorld(w.getUniqueId()));
    }

    @Test
    void addAndLookupByExactName() {
        BuildWorld w = world("Bravo");
        storage.addBuildWorld(w);
        assertEquals(w, storage.getBuildWorld("Bravo"));
    }

    @Test
    void lookupByNameIsCaseInsensitive() {
        BuildWorld w = world("Charlie");
        storage.addBuildWorld(w);
        assertEquals(w, storage.getBuildWorld("charlie"));
        assertEquals(w, storage.getBuildWorld("CHARLIE"));
        assertEquals(w, storage.getBuildWorld("ChArLiE"));
    }

    @Test
    void removeClearsAllIndexes() {
        BuildWorld w = world("Delta");
        storage.addBuildWorld(w);
        storage.removeBuildWorld(w);
        assertNull(storage.getBuildWorld(w.getUniqueId()));
        assertNull(storage.getBuildWorld("Delta"));
    }

    @Test
    void renameOldNameGoneNewNameResolves() {
        BuildWorld w = world("Echo");
        storage.addBuildWorld(w);

        when(w.getName()).thenReturn("Foxtrot");
        storage.rename(w, "Echo", "Foxtrot");

        assertNull(storage.getBuildWorld("Echo"));
        assertEquals(w, storage.getBuildWorld("Foxtrot"));
        assertEquals(w, storage.getBuildWorld("foxtrot"));
    }

    @Test
    void renamePreservesUuidLookup() {
        BuildWorld w = world("Golf");
        storage.addBuildWorld(w);
        UUID id = w.getUniqueId();

        when(w.getName()).thenReturn("Hotel");
        storage.rename(w, "Golf", "Hotel");

        assertEquals(w, storage.getBuildWorld(id));
    }

    @Test
    void missingWorldReturnsNull() {
        assertNull(storage.getBuildWorld("NoSuchWorld"));
        assertNull(storage.getBuildWorld(UUID.randomUUID()));
    }

    @Test
    void concurrentAddRemoveIsConsistent() throws Exception {
        int threads = 4;
        int opsPerThread = 250;

        try (ExecutorService exec = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch done = new CountDownLatch(threads);
            Future<?>[] futures = new Future<?>[threads];

            for (int t = 0; t < threads; t++) {
                futures[t] = exec.submit(() -> {
                    try {
                        ready.countDown();
                        ready.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            BuildWorld w =
                                    world("World-" + Thread.currentThread().threadId() + "-" + i);
                            storage.addBuildWorld(w);
                            assertNotNull(storage.getBuildWorld(w.getUniqueId()));
                            storage.removeBuildWorld(w);
                            assertNull(storage.getBuildWorld(w.getUniqueId()));
                        }
                    } finally {
                        done.countDown();
                    }
                    return null;
                });
            }

            assertTrue(done.await(10, TimeUnit.SECONDS));
            // Rethrows an assertion that failed on a worker thread.
            for (Future<?> future : futures) {
                future.get();
            }
        }
    }

    @Test
    void rename_caseOnlyChange_keepsNameResolvable() {
        // Old and new names lower-case to the same key; the world must stay resolvable after the rename.
        BuildWorld w = world("India");
        storage.addBuildWorld(w);

        when(w.getName()).thenReturn("INDIA");
        storage.rename(w, "India", "INDIA");

        assertSame(w, storage.getBuildWorld("india"));
        assertSame(w, storage.getBuildWorld("INDIA"));
    }

    @Test
    void sameNameInDifferentNamespaces_areDistinctWorlds() {
        BuildWorld plain = world("Lobby");
        BuildWorld namespaced = world("maps:Lobby");
        storage.addBuildWorld(plain);
        storage.addBuildWorld(namespaced);

        assertSame(plain, storage.getBuildWorld("lobby"));
        assertSame(plain, storage.getBuildWorld("minecraft:LOBBY"));
        assertSame(namespaced, storage.getBuildWorld("MAPS:lobby"));
        assertNull(storage.getBuildWorld("other:lobby"));
    }

    @Test
    void lookupByBukkitWorld_usesTheKeyOutsideMinecraft() {
        BuildWorld plain = world("maps_lobby");
        BuildWorld namespaced = world("maps:lobby");
        storage.addBuildWorld(plain);
        storage.addBuildWorld(namespaced);

        // Paper names the world maps:lobby "maps_lobby", which must not resolve to the plain world of that name.
        World keyed = mock(World.class);
        when(keyed.getName()).thenReturn("maps_lobby");
        when(keyed.getKey()).thenReturn(new NamespacedKey("maps", "lobby"));
        assertSame(namespaced, storage.getBuildWorld(keyed));

        World main = mock(World.class);
        when(main.getName()).thenReturn("maps_lobby");
        when(main.getKey()).thenReturn(NamespacedKey.minecraft("maps_lobby"));
        assertSame(plain, storage.getBuildWorld(main));
    }

    @Test
    void worldImportedUnderItsBukkitName_isFoundByItsKey() {
        // Imported before namespaces existed: the world maps:lobby was stored under Paper's Bukkit name for it, and
        // its data lives in dimensions/maps/lobby, not in a folder of its own.
        BuildWorld legacy = world("maps_lobby");
        storage.addBuildWorld(legacy);

        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertSame(legacy, storage.getBuildWorld(keyedWorld("maps_lobby", new NamespacedKey("maps", "lobby"))));
            assertSame(legacy, storage.getBuildWorld("maps:lobby"));
            assertSame(legacy, storage.getBuildWorld("maps_lobby"));
            assertNull(storage.getBuildWorld("other:lobby"));
        }
    }

    @Test
    void worldReallyNamedLikeABukkitName_isNotFoundByTheNamespacedName() throws IOException {
        BuildWorld plain = world("maps_lobby");
        storage.addBuildWorld(plain);
        Files.createDirectories(tempDir.resolve("world/dimensions/minecraft/maps_lobby"));

        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertNull(storage.getBuildWorld("maps:lobby"));
            assertNull(storage.getBuildWorld(keyedWorld("maps_lobby", new NamespacedKey("maps", "lobby"))));
            assertSame(plain, storage.getBuildWorld("maps_lobby"));
        }
    }

    @Test
    void bukkitNameClash_worksBothWaysAgainstStoredWorlds() {
        storage.addBuildWorld(world("maps_lobby"));
        storage.addBuildWorld(world("events:Arena"));

        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertEquals("maps_lobby", storage.bukkitNameClash("maps:Lobby"));
            assertEquals("events:Arena", storage.bukkitNameClash("Events_arena"));
            assertNull(storage.bukkitNameClash("maps_lobby"));
            assertNull(storage.bukkitNameClash("events:arena"));
            assertNull(storage.bukkitNameClash("maps:arena"));
        }
    }

    @Test
    void bukkitNameClash_countsALoadedWorldUnlessItIsTheSameWorld() {
        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            World plain = keyedWorld("maps_lobby", NamespacedKey.minecraft("maps_lobby"));
            bukkit.when(() -> Bukkit.getWorld("maps_lobby")).thenReturn(plain);
            assertEquals("maps_lobby", storage.bukkitNameClash("maps:lobby"));

            // Importing a world another plugin already loaded at that key is not a clash.
            World keyed = keyedWorld("maps_lobby", new NamespacedKey("maps", "lobby"));
            bukkit.when(() -> Bukkit.getWorld("maps_lobby")).thenReturn(keyed);
            assertNull(storage.bukkitNameClash("maps:lobby"));
            assertEquals("maps:lobby", storage.bukkitNameClash("maps_lobby"));
        }
    }

    private static World keyedWorld(String name, NamespacedKey key) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        when(world.getKey()).thenReturn(key);
        return world;
    }

    /** A running server with {@code level-name=world} whose container is the temp dir. */
    private MockedStatic<Bukkit> mockServer() {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(tempDir.toFile());
        World main = keyedWorld("world", NamespacedKey.minecraft("overworld"));
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(main));
        bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);
        return bukkit;
    }

    @Test
    void matchWorlds_bareName_findsAUniqueWorldInAnyNamespace() {
        BuildWorld arena = world("events:Arena");
        storage.addBuildWorld(arena);

        assertEquals(List.of(arena), storage.matchWorlds("arena"));
        assertEquals(List.of(arena), storage.matchWorlds("EVENTS:arena"));
        assertEquals(List.of(), storage.matchWorlds("minecraft:arena"));
        assertEquals(List.of(), storage.matchWorlds("lobby"));
    }

    @Test
    void matchWorlds_bareName_prefersTheDefaultNamespaceThenMinecraft() {
        BuildWorld plain = world("Lobby");
        BuildWorld maps = world("maps:Lobby");
        storage.addBuildWorld(plain);
        storage.addBuildWorld(maps);
        storage.addBuildWorld(world("events:Lobby"));

        defaultNamespace = "maps";
        assertEquals(List.of(maps), storage.matchWorlds("lobby"));
        defaultNamespace = "other";
        assertEquals(List.of(plain), storage.matchWorlds("lobby"));
    }

    @Test
    void matchWorlds_bareName_isAmbiguousWithoutAPreferredWorld() {
        BuildWorld events = world("events:Lobby");
        BuildWorld games = world("games:Lobby");
        storage.addBuildWorld(events);
        storage.addBuildWorld(games);

        assertEquals(2, storage.matchWorlds("lobby").size());
        assertEquals(List.of(games), storage.matchWorlds("games:lobby"));
    }

    @Test
    void typedNames_isTheBarePathOnlyWhenNoOtherWorldSharesIt() {
        storage.addBuildWorld(world("events:Arena"));
        storage.addBuildWorld(world("Lobby"));
        storage.addBuildWorld(world("maps:Lobby"));

        assertEquals("Arena", storage.typedNames().apply("events:Arena"));
        assertEquals("minecraft:Lobby", storage.typedNames().apply("Lobby"));
        assertEquals("maps:Lobby", storage.typedNames().apply("maps:Lobby"));
    }

    @Test
    void newWorldName_placesBareNamesInTheDefaultNamespace() {
        defaultNamespace = "maps";

        assertEquals("maps:lobby", storage.newWorldName("lobby"));
        assertEquals("Lobby", storage.newWorldName("minecraft:Lobby"));
        assertEquals("events:lobby", storage.newWorldName("Events:lobby"));
        for (String worldName : List.of("Lobby", "maps:Lobby", "events:Arena")) {
            String typed = storage.typedNewName(worldName);
            assertEquals(worldName, storage.newWorldName(typed), typed);
        }
        assertEquals("Lobby", storage.typedNewName("maps:Lobby"));
        assertEquals("minecraft:Lobby", storage.typedNewName("Lobby"));
    }

    @Test
    void renamedWorldName_keepsTheWorldsNamespaceForBareNames() {
        defaultNamespace = "events";

        assertEquals("maps:arena", storage.renamedWorldName("maps:lobby", "arena"));
        assertEquals("arena", storage.renamedWorldName("Lobby", "arena"));
        assertEquals("events:arena", storage.renamedWorldName("maps:lobby", "events:arena"));
        assertEquals("arena", storage.renamedWorldName("maps:lobby", "minecraft:arena"));
    }

    @Test
    void rename_remapsNameLookup() {
        BuildWorld buildWorld = world("oldName");
        storage.addBuildWorld(buildWorld);

        storage.rename(buildWorld, "oldName", "newName");

        assertNull(storage.getBuildWorld("oldName"));
        assertSame(buildWorld, storage.getBuildWorld("newName"));
        assertSame(buildWorld, storage.getBuildWorld(buildWorld.getUniqueId()));
    }
}
