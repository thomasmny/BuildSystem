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

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class WorldStorageImplTest {

    private WorldStorageImpl storage;

    @BeforeEach
    void setUp() {
        storage = new WorldStorageImpl(Logger.getLogger("test")) {
            @Override
            public CompletableFuture<Void> save(BuildWorld object) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> save(Collection<BuildWorld> objects) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Collection<BuildWorld>> load() {
                return CompletableFuture.completedFuture(java.util.List.of());
            }

            @Override
            public CompletableFuture<Void> delete(BuildWorld object) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> delete(String key) {
                return CompletableFuture.completedFuture(null);
            }
        };
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
    void concurrentAddRemoveIsConsistent() throws InterruptedException {
        int threads = 4;
        int opsPerThread = 250;

        try (ExecutorService exec = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch done = new CountDownLatch(threads);

            for (int t = 0; t < threads; t++) {
                exec.submit(() -> {
                    ready.countDown();
                    try {
                        ready.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < opsPerThread; i++) {
                        BuildWorld w = world("World-" + Thread.currentThread().threadId() + "-" + i);
                        storage.addBuildWorld(w);
                        assertNotNull(storage.getBuildWorld(w.getUniqueId()));
                        storage.removeBuildWorld(w);
                        assertNull(storage.getBuildWorld(w.getUniqueId()));
                    }
                    done.countDown();
                });
            }

            done.await();
        }
    }
}
