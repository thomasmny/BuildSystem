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
package de.eintosti.buildsystem.world.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers what stands between a leaked link and the world behind it: the link belongs to the first client that uses
 * it, it dies on time, and the rate limiter cuts off a client that hammers the endpoint.
 */
@NullMarked
class DownloadRegistryTest {

    private static final String OWNER = "203.0.113.7";
    private static final String STRANGER = "198.51.100.4";

    @TempDir
    Path tempDir;

    private final AtomicLong now = new AtomicLong(1_000L);

    @Test
    void pinsALinkToTheClientThatUsesItFirst() throws IOException {
        DownloadRegistry registry = new DownloadRegistry(now::get);
        String token = registry.register(archive("world.zip"), "world.zip", now.get() + 1_000L);

        assertNotNull(registry.claim(token, OWNER), "the first client should get the download");
        assertNull(registry.claim(token, STRANGER), "a forwarded link should be useless to anyone else");
        assertNotNull(registry.claim(token, OWNER), "the owner should still be able to retry");
    }

    @Test
    void refusesExpiredAndUnknownTokens() throws IOException {
        DownloadRegistry registry = new DownloadRegistry(now::get);
        String token = registry.register(archive("world.zip"), "world.zip", now.get() + 1_000L);

        assertNull(registry.claim("not-a-token", OWNER));

        now.addAndGet(1_001L);
        assertNull(registry.claim(token, OWNER));
    }

    @Test
    void purgeDeletesExpiredArchivesOnly() throws IOException {
        DownloadRegistry registry = new DownloadRegistry(now::get);
        Path expiring = archive("expiring.zip");
        Path surviving = archive("surviving.zip");
        registry.register(expiring, "expiring.zip", now.get() + 1_000L);
        registry.register(surviving, "surviving.zip", now.get() + 10_000L);

        now.addAndGet(1_001L);
        List<Path> deleted = new ArrayList<>();
        registry.purgeExpired(deleted::add);

        assertEquals(List.of(expiring), deleted);
        assertEquals(Files.size(surviving), registry.totalBytes());
    }

    @Test
    void rateLimiterCutsOffAClientAndRecoversNextWindow() {
        RequestRateLimiter limiter = new RequestRateLimiter(2, 1_000L, now::get);

        assertTrue(limiter.allow(OWNER));
        assertTrue(limiter.allow(OWNER));
        assertTrue(!limiter.allow(OWNER), "the third request in the window should be refused");
        assertTrue(limiter.allow(STRANGER), "another client keeps its own budget");

        now.addAndGet(1_001L);
        assertTrue(limiter.allow(OWNER));
    }

    private Path archive(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, "archive-" + name);
        return file;
    }
}
