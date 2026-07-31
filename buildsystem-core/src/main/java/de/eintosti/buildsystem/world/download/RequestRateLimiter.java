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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.jspecify.annotations.NullMarked;

/**
 * A fixed-window request limit per client address, so the download endpoint cannot be hammered by one host.
 */
@NullMarked
final class RequestRateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    RequestRateLimiter(int maxRequests, long windowMillis) {
        this(maxRequests, windowMillis, System::currentTimeMillis);
    }

    RequestRateLimiter(int maxRequests, long windowMillis, LongSupplier clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /**
     * Counts a request from {@code clientAddress}.
     *
     * @return {@code true} if it is within the limit, {@code false} if the client is over budget
     */
    boolean allow(String clientAddress) {
        long now = clock.getAsLong();
        Window window = windows.compute(clientAddress, (ignored, current) -> {
            if (current == null || now - current.startedAt() >= windowMillis) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });
        return window.count() <= maxRequests;
    }

    /**
     * Forgets windows that have run out, so the map does not grow with every address ever seen.
     */
    void purgeStale() {
        long now = clock.getAsLong();
        windows.values().removeIf(window -> now - window.startedAt() >= windowMillis);
    }

    void clear() {
        windows.clear();
    }

    private record Window(long startedAt, int count) {}
}
