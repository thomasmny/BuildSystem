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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.util.ServerModeChecker;
import de.eintosti.buildsystem.util.ServerModeChecker.ServerMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;

/**
 * Pins the offline-safe behavior of {@link PlayerLookupService}: cached lookups resolve without scheduling, names are
 * case-insensitive, the undashed/dashed UUID conversion round-trips, and {@code resolve} prefers an online player. The
 * network paths are intentionally untested (no live Mojang calls in CI) and covered by the compile gate.
 */
@NullMarked
class PlayerLookupServiceTest {

    @Test
    void cachedUuidLookupCompletesImmediately() throws ExecutionException, InterruptedException {
        PlayerLookupService service = new PlayerLookupService(null, Runnable::run, Runnable::run);
        UUID uuid = UUID.randomUUID();
        service.cacheUser(uuid, "Notch");

        var future = service.lookupUniqueId("Notch");
        assertTrue(future.isDone(), "cached lookup should not schedule an async task");
        assertEquals(uuid, future.get());
    }

    @Test
    void cachedNameLookupIsCaseInsensitive() throws ExecutionException, InterruptedException {
        PlayerLookupService service = new PlayerLookupService(null, Runnable::run, Runnable::run);
        UUID uuid = UUID.randomUUID();
        service.cacheUser(uuid, "Steve");

        assertEquals(uuid, service.lookupUniqueId("steve").get());
        assertEquals(uuid, service.lookupUniqueId("STEVE").get());
    }

    @Test
    void undashedUuidIsParsed() {
        assertEquals(
                UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
                PlayerLookupService.fromUndashed("069a79f444e94726a5befca90e38aaf5"));
    }

    @Test
    void namesMojangCannotHaveAreRejectedBeforeAnyRequest() {
        // Neither name forms a valid URI, so without the check URI.create throws and the null plugin fails the log
        // call. Neither can reach Mojang if the check is removed.
        PlayerLookupService service = new PlayerLookupService(null, Runnable::run, Runnable::run);
        try (MockedStatic<ServerModeChecker> mode = mockStatic(ServerModeChecker.class)) {
            mode.when(ServerModeChecker::getServerMode).thenReturn(ServerMode.ONLINE);

            assertNull(service.lookupUniqueIdBlocking("two words"));
            assertNull(service.lookupUniqueIdBlocking("a^b"));
        }
    }

    @Test
    void resolve_onlinePlayer_usesThemWithoutALookup() {
        ServerMock server = MockBukkit.mock();
        try {
            PlayerMock steve = server.addPlayer("Steve");
            Executor unused = task -> fail("an online player needs no lookup");
            List<Builder> found = new ArrayList<>();

            new PlayerLookupService(null, unused, unused).resolve("Steve", found::add, () -> fail("Steve is online"));

            assertEquals(steve.getUniqueId(), found.getFirst().getUniqueId());
            assertEquals("Steve", found.getFirst().getName());
        } finally {
            MockBukkit.unmock();
        }
    }

    @Test
    void resolve_offlinePlayer_isLookedUp() {
        MockBukkit.mock();
        try {
            PlayerLookupService service = new PlayerLookupService(null, Runnable::run, Runnable::run);
            UUID notch = UUID.randomUUID();
            service.cacheUser(notch, "Notch");
            List<Builder> found = new ArrayList<>();

            service.resolve("Notch", found::add, () -> fail("Notch is cached"));

            assertEquals(notch, found.getFirst().getUniqueId());
            assertEquals("Notch", found.getFirst().getName());
        } finally {
            MockBukkit.unmock();
        }
    }

    @Test
    void resolve_unknownName_runsTheNotFoundCallback() {
        MockBukkit.mock();
        try (MockedStatic<ServerModeChecker> mode = mockStatic(ServerModeChecker.class)) {
            mode.when(ServerModeChecker::getServerMode).thenReturn(ServerMode.ONLINE);
            AtomicBoolean notFound = new AtomicBoolean();

            new PlayerLookupService(null, Runnable::run, Runnable::run)
                    .resolve("two words", builder -> fail("no account has that name"), () -> notFound.set(true));

            assertTrue(notFound.get());
        } finally {
            MockBukkit.unmock();
        }
    }
}
