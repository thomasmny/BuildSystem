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
package de.eintosti.buildsystem.storage.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.storage.codec.Codec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class YamlEntityFileTest {

    @TempDir
    File dataFolder;

    private record Entry(String name) {}

    private static final Codec<Entry> CODEC = new Codec<>() {
        @Override
        public String key(Entry value) {
            return value.name();
        }

        @Override
        public Map<String, Object> serialize(Entry value) {
            return Map.of("name", value.name());
        }

        @Override
        public Entry deserialize(String key, ConfigurationSection section) {
            return new Entry(key);
        }
    };

    @Test
    void aDeleteAfterASave_isWrittenAfterIt() {
        // Runs the most recently submitted task first, the worst order a thread pool could pick.
        Deque<Runnable> queued = new ArrayDeque<>();
        Logger logger = Logger.getLogger("YamlEntityFileTest");
        YamlEntityFile<Entry> file = new YamlEntityFile<>(
                new YamlStore(dataFolder, "entries.yml", logger),
                "entries",
                "entry",
                () -> CODEC,
                queued::push,
                logger,
                null);

        CompletableFuture<Void> save = file.save(new Entry("arena"));
        CompletableFuture<Void> delete = file.delete(new Entry("arena"));
        while (!queued.isEmpty()) {
            queued.pop().run();
        }

        assertTrue(save.isDone() && delete.isDone());
        YamlConfiguration written = YamlConfiguration.loadConfiguration(new File(dataFolder, "entries.yml"));
        assertFalse(written.contains("entries.arena"), "the save must not bring the deleted entry back");
    }

    @Test
    void aFileThatFailsToLoad_isNotMigratedOrSavedOver_andTheRefusalIsLoggedOnce() throws Exception {
        List<String> logged = new ArrayList<>();
        Logger logger = Logger.getLogger("YamlEntityFileTest.broken");
        logger.setUseParentHandlers(false);
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
        YamlEntityFile<Entry> file = new YamlEntityFile<>(
                new YamlStore(dataFolder, "entries.yml", logger),
                "entries",
                "entry",
                () -> CODEC,
                Runnable::run,
                logger,
                (section, log) -> section.set("entries", null));
        Path entries = dataFolder.toPath().resolve("entries.yml");
        String broken = "entries:\n  arena:\n    name: [unclosed\n";
        Files.writeString(entries, broken);

        assertTrue(file.load().join().isEmpty());
        assertEquals(broken, Files.readString(entries), "the legacy migration must not rewrite a file it cannot read");

        file.save(new Entry("lobby")).join();
        file.save(new Entry("hub")).join();
        assertEquals(broken, Files.readString(entries));
        assertEquals(
                1,
                logged.stream()
                        .filter(message -> message.startsWith("Not saving"))
                        .count(),
                "a refused save is logged once, not on every autosave");
    }
}
