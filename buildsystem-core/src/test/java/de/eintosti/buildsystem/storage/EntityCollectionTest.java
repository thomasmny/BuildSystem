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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.codec.Codec;
import de.eintosti.buildsystem.storage.yaml.YamlEntityStore;
import de.eintosti.buildsystem.storage.yaml.YamlStore;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@NullMarked
class EntityCollectionTest {

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
        Logger logger = Logger.getLogger("EntityCollectionTest");
        EntityCollection<Entry> file = new EntityCollection<>(
                new YamlEntityStore(new YamlStore(dataFolder, "entries.yml", logger), "entries", logger, null),
                "entry",
                () -> CODEC,
                queued::push,
                logger);

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
    void aSingleSaveThatFails_isLogged() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        List<String> logged = new ArrayList<>();
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getLevel() + " " + record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        EntityStore failing = mock(EntityStore.class);
        doThrow(new StorageException("Could not save to buildsystem_players", null))
                .when(failing)
                .upsert(any());
        EntityCollection<Entry> file = new EntityCollection<>(failing, "player", () -> CODEC, Runnable::run, logger);

        assertThrows(CompletionException.class, file.save(new Entry("steve"))::join);

        assertEquals(List.of("SEVERE Could not save player \"steve\""), logged);
    }

    @Test
    void replaceAll_isRefusedUntilTheEntriesLoad() {
        Logger logger = Logger.getLogger("EntityCollectionTest");
        EntityStore failing = mock(EntityStore.class);
        when(failing.loadAll()).thenThrow(new StorageException("Could not load entries", null));
        EntityCollection<Entry> file = new EntityCollection<>(failing, "entry", () -> CODEC, Runnable::run, logger);

        assertThrows(CompletionException.class, file.replaceAll(List.of())::join);
        assertThrows(CompletionException.class, file.load()::join);
        assertThrows(CompletionException.class, file.replaceAll(List.of())::join);
        verify(failing, never()).replaceAll(any());
    }

    @Test
    void aFileThatFailsToLoad_isNotMigratedOrSavedOver_andTheRefusalIsLoggedOnce() throws Exception {
        List<String> logged = new ArrayList<>();
        Logger logger = Logger.getLogger("EntityCollectionTest.broken");
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
        Path entries = dataFolder.toPath().resolve("entries.yml");
        String broken = "entries:\n  arena:\n    name: [unclosed\n";
        Files.writeString(entries, broken);
        EntityCollection<Entry> file = new EntityCollection<>(
                new YamlEntityStore(
                        new YamlStore(dataFolder, "entries.yml", logger),
                        "entries",
                        logger,
                        (section, log) -> section.set("entries", null)),
                "entry",
                () -> CODEC,
                Runnable::run,
                logger);

        assertThrows(CompletionException.class, file.load()::join);
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

    @Test
    void anEntryThatWasSkippedOnLoad_isNotSavedOver_untilItLoads() throws Exception {
        List<String> logged = new ArrayList<>();
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getLevel() + " " + record.getMessage());
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        Codec<Entry> strict = new Codec<>() {
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
                return new Entry(Objects.requireNonNull(section.getString("name"), "name"));
            }
        };
        Path entries = dataFolder.toPath().resolve("entries.yml");
        Files.writeString(entries, "entries:\n  hub:\n    other: kept\n  lobby:\n    name: lobby\n");
        EntityCollection<Entry> file = new EntityCollection<>(
                new YamlEntityStore(new YamlStore(dataFolder, "entries.yml", logger), "entries", logger, null),
                "entry",
                () -> strict,
                Runnable::run,
                logger);

        assertEquals(List.of("lobby"), List.copyOf(file.load().join().keySet()));
        logged.clear();
        file.save(new Entry("hub")).join();
        file.save(List.of(new Entry("hub"), new Entry("lobby"))).join();
        file.replaceAll(List.of(new Entry("lobby"))).join();

        YamlConfiguration written = YamlConfiguration.loadConfiguration(entries.toFile());
        assertEquals("kept", written.getString("entries.hub.other"));
        assertFalse(written.contains("entries.hub.name"), "the skipped entry must not be written over");
        assertTrue(written.contains("entries.lobby"));
        assertEquals(
                List.of("SEVERE Not saving entry \"hub\": it is stored but could not be loaded. Fix or delete"
                        + " the stored entry, then restart the server."),
                logged);

        Files.writeString(entries, "entries:\n  hub:\n    name: hub\n");
        assertEquals(List.of("hub"), List.copyOf(file.load().join().keySet()));
        file.save(new Entry("hub")).join();
        file.delete("hub").join();
        assertFalse(YamlConfiguration.loadConfiguration(entries.toFile()).contains("entries.hub"));
    }

    @Test
    void anEntryThatWasSkippedOnLoad_canStillBeDeleted() {
        Logger logger = Logger.getLogger("EntityCollectionTest");
        EntityStore store =
                new YamlEntityStore(new YamlStore(dataFolder, "entries.yml", logger), "entries", logger, null);
        Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
        rows.put("hub", Map.of("name", 7));
        store.upsert(rows);
        EntityCollection<Entry> file = new EntityCollection<>(
                store,
                "entry",
                () -> new Codec<>() {
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
                        throw new IllegalArgumentException("unreadable");
                    }
                },
                Runnable::run,
                logger);

        assertTrue(file.load().join().isEmpty());
        file.delete("hub").join();

        assertTrue(store.loadAll().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(
            value = StorageType.class,
            names = {"YAML", "SQLITE"})
    void afterAFailedLoad_writesAreRefused_untilALoadSucceeds(StorageType type) {
        Logger logger = Logger.getLogger("EntityCollectionTest");
        StorageBackend backend =
                StorageBackend.connect(type, new PluginConfig.Storage.Database(type, "", Map.of()), dataFolder, logger);
        try {
            EntityStore real = backend.store(EntityKind.PLAYERS);
            real.upsert(Map.of("hub", Map.of("name", "stored")));
            EntityStore failingOnce = spy(real);
            doThrow(new StorageException("Could not load the entries", null))
                    .doCallRealMethod()
                    .when(failingOnce)
                    .loadAll();
            EntityCollection<Entry> file =
                    new EntityCollection<>(failingOnce, "entry", () -> CODEC, Runnable::run, logger);

            assertThrows(CompletionException.class, file.load()::join);
            file.save(new Entry("hub")).join();
            file.save(new Entry("lobby")).join();
            file.delete("hub").join();
            assertEquals(Map.of("hub", Map.of("name", "stored")), real.loadAll());

            file.load().join();
            file.save(new Entry("lobby")).join();
            assertEquals(List.of("hub", "lobby"), List.copyOf(real.loadAll().keySet()));
        } finally {
            backend.close();
        }
    }
}
