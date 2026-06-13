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
package de.eintosti.buildsystem.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * Pins the offline-safe behavior of {@link PlayerLookupService}: cached lookups resolve without scheduling, names are
 * case-insensitive, and the undashed/dashed UUID conversion round-trips. The network paths are intentionally untested
 * (no live Mojang calls in CI) and covered by the compile gate.
 */
@NullMarked
class PlayerLookupServiceTest {

    @Test
    void cachedUuidLookupCompletesImmediately() throws ExecutionException, InterruptedException {
        PlayerLookupService service = new PlayerLookupService(null);
        UUID uuid = UUID.randomUUID();
        service.cacheUser(uuid, "Notch");

        var future = service.lookupUniqueId("Notch");
        assertTrue(future.isDone(), "cached lookup should not schedule an async task");
        assertEquals(uuid, future.get());
    }

    @Test
    void cachedNameLookupIsCaseInsensitive() throws ExecutionException, InterruptedException {
        PlayerLookupService service = new PlayerLookupService(null);
        UUID uuid = UUID.randomUUID();
        service.cacheUser(uuid, "Steve");

        assertEquals(uuid, service.lookupUniqueId("steve").get());
        assertEquals(uuid, service.lookupUniqueId("STEVE").get());
        assertEquals("Steve", service.lookupName(uuid).get());
    }

    @Test
    void undashedUuidRoundTrips() {
        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        String undashed = PlayerLookupService.toUndashed(uuid);
        assertEquals("069a79f444e94726a5befca90e38aaf5", undashed);
        assertEquals(uuid, PlayerLookupService.fromUndashed(undashed));
    }
}
