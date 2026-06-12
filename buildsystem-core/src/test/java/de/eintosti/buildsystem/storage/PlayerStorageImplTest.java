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

import de.eintosti.buildsystem.api.player.BuildPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
class PlayerStorageImplTest {

    private PlayerStorageImpl storage;

    @BeforeEach
    void setUp() {
        storage = new PlayerStorageImpl(Logger.getLogger("test")) {
            @Override
            public CompletableFuture<Collection<BuildPlayer>> load() {
                return CompletableFuture.completedFuture(List.of());
            }

            @Override
            public CompletableFuture<Void> save(BuildPlayer buildPlayer) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> save(Collection<BuildPlayer> buildPlayers) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> delete(BuildPlayer buildPlayer) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Void> delete(String playerKey) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    @Test
    void createBuildPlayer_idempotent_returnsSameInstance() {
        UUID uuid = UUID.randomUUID();
        BuildPlayer first = storage.createBuildPlayer(uuid);
        BuildPlayer second = storage.createBuildPlayer(uuid);
        assertSame(first, second, "createBuildPlayer must return the same instance for the same UUID");
    }

    @Test
    void createBuildPlayer_differentUuids_differentInstances() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        BuildPlayer p1 = storage.createBuildPlayer(uuid1);
        BuildPlayer p2 = storage.createBuildPlayer(uuid2);
        assertEquals(uuid1, p1.getUniqueId());
        assertEquals(uuid2, p2.getUniqueId());
    }

    @Test
    void getBuildPlayer_returnsNullWhenNotPresent() {
        assertNull(storage.getBuildPlayer(UUID.randomUUID()));
    }

    @Test
    void getBuildPlayer_returnsInstanceAfterCreate() {
        UUID uuid = UUID.randomUUID();
        BuildPlayer created = storage.createBuildPlayer(uuid);
        assertSame(created, storage.getBuildPlayer(uuid));
    }

    @Test
    void createBuildPlayer_concurrent_sameUuid_exactlyOneInstance() throws Exception {
        UUID uuid = UUID.randomUUID();
        int threadCount = 4;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<BuildPlayer>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                barrier.await();
                return storage.createBuildPlayer(uuid);
            }));
        }

        executor.shutdown();
        BuildPlayer first = futures.get(0).get();
        for (Future<BuildPlayer> future : futures) {
            assertSame(first, future.get(), "All concurrent createBuildPlayer calls must return the same instance");
        }
        assertNotNull(first);
    }
}
