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
package de.eintosti.buildsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@NullMarked
class BuildSystemPluginShutdownTest {

    @Test
    @Timeout(10)
    void awaitSaves_givesUpOnASaveThatNeverFinishes_andNamesIt() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        List<String> logged = new ArrayList<>();
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        Map<String, CompletableFuture<Void>> saves = new LinkedHashMap<>();
        saves.put("worlds", CompletableFuture.completedFuture(null));
        saves.put("players", new CompletableFuture<>());

        BuildSystemPlugin.awaitSaves(saves, Duration.ofMillis(50), logger);

        assertEquals(List.of("Stopped waiting for saves after 0 seconds. Not saved: players"), logged);
    }
}
