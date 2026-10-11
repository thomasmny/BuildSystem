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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.codec.Codec;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageMigratorTest {

    private static final Logger LOGGER = Logger.getLogger("StorageMigratorTest");

    private static final String HOSTILE = "'; DROP TABLE buildsystem_players; -- \" OR 1=1 {\"$gt\": \"\"}";

    private record Note(String key, long time) {}

    private static final Codec<Note> CODEC = new Codec<>() {
        @Override
        public String key(Note note) {
            return note.key();
        }

        @Override
        public Map<String, Object> serialize(Note note) {
            return Map.of(
                    "time", note.time(), "name", HOSTILE, "tags", List.of("a", "b"), "nested", Map.of("on", true));
        }

        @Override
        public Note deserialize(String key, ConfigurationSection section) {
            if (section.contains("broken")) {
                throw new IllegalArgumentException("Cannot parse " + key);
            }
            return new Note(key, section.getLong("time"));
        }
    };

    @TempDir
    File dataFolder;

    // Runs the most recently submitted task first, the worst order a thread pool could pick.
    private final Deque<Runnable> background = new ArrayDeque<>();
    private final Deque<Runnable> mainThread = new ArrayDeque<>();
    private final List<Note> live = new ArrayList<>();
    private final YamlConfiguration liveConfig = new YamlConfiguration();
    private final AtomicReference<StorageBackend> current = new AtomicReference<>();
    // Unique, so that a test against a shared database server starts without tables.
    private final String prefix = "t" + UUID.randomUUID().toString().substring(0, 8) + "_";
    private EntityCollection<Note> notes;
    private StorageMigrator migrator;

    @BeforeEach
    void open() throws Exception {
        Files.writeString(
                new File(dataFolder, "config.yml").toPath(),
                "storage:\n  # Kept as it is.\n  type: yaml\n  table-prefix: buildsystem_\n");
        current.set(StorageBackend.connect(StorageType.YAML, settings(), dataFolder, LOGGER));
        notes = new EntityCollection<>(
                current.get().store(EntityKind.PLAYERS), "note", () -> CODEC, background::push, LOGGER);
        migrator = new StorageMigrator(
                List.of(new StorageMigrator.Part<>(EntityKind.PLAYERS, notes, () -> live)),
                current::get,
                current::set,
                dataFolder,
                () -> liveConfig,
                background::push,
                mainThread::push,
                LOGGER);
        notes.load();
        runAll();
    }

    @AfterEach
    void close() {
        current.get().close();
    }

    @Test
    void migrate_copiesWhatIsInMemorySwitchesAndWritesTheConfig() throws Exception {
        live.addAll(List.of(new Note("a", 1_700_000_000_000L), new Note("b", 2)));
        notes.save(live);

        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(target(), settings());
        drain(background);
        drain(mainThread);
        live.add(new Note("c", 3));
        notes.save(live.getLast());
        runAll();

        assertEquals(Map.of(EntityKind.PLAYERS, 2), migration.join());
        assertEquals(target(), current.get().type());
        assertEquals(
                List.of("a", "b", "c"),
                List.copyOf(
                        current.get().collection(EntityKind.PLAYERS).loadAll().keySet()));
        assertEquals(
                Map.of(
                        "time",
                        1_700_000_000_000L,
                        "name",
                        HOSTILE,
                        "tags",
                        List.of("a", "b"),
                        "nested",
                        Map.of("on", true)),
                current.get().collection(EntityKind.PLAYERS).loadAll().get("a"));
        assertEquals(
                List.of("a", "b"),
                List.copyOf(
                        EntityKind.PLAYERS.yaml(dataFolder, LOGGER).loadAll().keySet()),
                "the write queued during the migration goes to the new backend only");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
        assertEquals(target().key(), config.getString("storage.type"));
        assertEquals(List.of("Kept as it is."), config.getComments("storage.type"));
        assertEquals(target().key(), liveConfig.getString("storage.type"));
        assertFalse(migrator.running());
    }

    @Test
    void migrate_alsoCopiesTheStoredEntriesThatAreNotInMemory_asTheyAreStored() {
        live.add(new Note("a", 1));
        notes.save(live);
        runAll();
        // Like a world that failed to load, or an entry that could not be parsed.
        current.get().collection(EntityKind.PLAYERS).upsert(Map.of("unloaded", Map.of("time", 2, "kept", "as is")));

        CompletableFuture<Integer> notLoaded = migrator.notLoaded();
        runAll();
        assertEquals(1, notLoaded.join());

        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(target(), settings());
        runAll();

        assertEquals(Map.of(EntityKind.PLAYERS, 2), migration.join());
        Map<String, Map<String, Object>> copied =
                current.get().collection(EntityKind.PLAYERS).loadAll();
        assertEquals(List.of("a", "unloaded"), List.copyOf(copied.keySet()));
        assertEquals("as is", copied.get("unloaded").get("kept"));
    }

    @Test
    void migrate_copiesTheStoredEntryThatCouldNotBeParsed_overTheBlankOneInMemory() throws Exception {
        File players = new File(dataFolder, "players.yml");
        YamlConfiguration file = YamlConfiguration.loadConfiguration(players);
        file.set("players.p.time", 1);
        file.set("players.p.broken", true);
        file.save(players);
        notes.load();
        runAll();
        // Like a player whose entry was skipped joining: a blank one is made in memory, and its save is refused.
        live.add(new Note("p", 0));
        notes.save(live);
        runAll();

        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(target(), settings());
        runAll();
        migration.join();

        Map<String, Object> copied =
                current.get().collection(EntityKind.PLAYERS).loadAll().get("p");
        assertEquals(1L, ((Number) copied.get("time")).longValue(), String.valueOf(copied));
        assertEquals(true, copied.get("broken"), String.valueOf(copied));
    }

    @Test
    void migrate_isRefused_whileTheSourceHoldsEntriesItCannotRead() throws Exception {
        live.add(new Note("a", 1));
        notes.save(live);
        runAll();
        File players = new File(dataFolder, "players.yml");
        YamlConfiguration file = YamlConfiguration.loadConfiguration(players);
        file.set("players.unreadable", "not a section");
        file.save(players);

        CompletableFuture<Integer> unreadable = migrator.unreadable();
        runAll();
        assertEquals(1, unreadable.join());

        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(target(), settings());
        runAll();

        CompletionException failure = assertThrows(CompletionException.class, migration::join);
        StorageMigrator.UnreadableEntriesException refused =
                assertInstanceOf(StorageMigrator.UnreadableEntriesException.class, failure.getCause());
        assertEquals(1, refused.count());
        assertEquals(StorageType.YAML, current.get().type());
        assertEquals(
                "yaml",
                YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"))
                        .getString("storage.type"));
        CompletableFuture<Boolean> targetHoldsData = migrator.holdsData(target(), settings());
        runAll();
        assertFalse(targetHoldsData.join());
    }

    @Test
    void migrate_rightAfterADelete_doesNotBringTheEntryBack() {
        Note deleted = new Note("deleted", 2);
        live.addAll(List.of(new Note("a", 1), deleted));
        notes.save(live);
        runAll();

        live.remove(deleted);
        notes.delete(deleted);
        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(target(), settings());
        runAll();

        assertEquals(Map.of(EntityKind.PLAYERS, 1), migration.join());
        assertEquals(
                List.of("a"),
                List.copyOf(
                        current.get().collection(EntityKind.PLAYERS).loadAll().keySet()));
    }

    @Test
    void migrate_replacesWhatTheTargetHeldBefore() {
        Note old = new Note("old", 1);
        live.add(old);
        migrator.migrate(target(), settings());
        runAll();
        live.clear();
        notes.delete(old);
        live.add(new Note("new", 2));

        migrator.migrate(StorageType.YAML, settings());
        runAll();
        migrator.migrate(target(), settings());
        runAll();

        assertEquals(
                List.of("new"),
                List.copyOf(
                        current.get().collection(EntityKind.PLAYERS).loadAll().keySet()));
    }

    @Test
    void migrate_toAServerThatCannotBeReached_keepsTheOldBackendAndConfig() throws Exception {
        live.add(new Note("a", 1));
        StorageBackend before = current.get();

        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(StorageType.MYSQL, settings());
        notes.save(new Note("held", 2));
        runAll();

        assertThrows(CompletionException.class, migration::join);
        assertSame(before, current.get());
        assertTrue(EntityKind.PLAYERS.yaml(dataFolder, LOGGER).loadAll().containsKey("held"));
        assertEquals(
                "yaml",
                YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"))
                        .getString("storage.type"));
        assertFalse(migrator.running());
    }

    @Test
    void migrate_toAServerWithoutAUsername_namesTheMissingKey() {
        PluginConfig.Storage.Database settings = new PluginConfig.Storage.Database(
                StorageType.SQLITE,
                "buildsystem_",
                Map.of(
                        StorageType.POSTGRESQL,
                        new PluginConfig.Storage.Database.Server(
                                StorageType.POSTGRESQL,
                                "localhost",
                                1,
                                "buildsystem",
                                " ",
                                "secret",
                                StorageTls.PREFER)));

        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(StorageType.POSTGRESQL, settings);
        runAll();

        CompletionException failure = assertThrows(CompletionException.class, migration::join);
        assertEquals(
                "storage.postgresql.username is not set", failure.getCause().getMessage());
    }

    @Test
    void migrate_beforeTheEntriesLoaded_isRefused() {
        EntityCollection<Note> unloaded = new EntityCollection<>(
                current.get().store(EntityKind.PLAYERS), "note", () -> CODEC, background::push, LOGGER);
        StorageMigrator early = new StorageMigrator(
                List.of(new StorageMigrator.Part<>(EntityKind.PLAYERS, unloaded, () -> live)),
                current::get,
                current::set,
                dataFolder,
                () -> liveConfig,
                background::push,
                mainThread::push,
                LOGGER);

        CompletableFuture<Map<EntityKind, Integer>> migration = early.migrate(target(), settings());

        assertTrue(migration.isCompletedExceptionally());
        assertFalse(new File(dataFolder, StorageBackend.SQLITE_FILE).exists());
        assertFalse(early.running());
    }

    @Test
    void migrate_fromAYamlFileThatCannotBeRead_isRefused() throws Exception {
        EntityCollection<Note> unreadable = new EntityCollection<>(
                current.get().store(EntityKind.PLAYERS), "note", () -> CODEC, background::push, LOGGER);
        // Written after the store opened the file, which outside a server has no Bukkit logger to report it to.
        Files.writeString(new File(dataFolder, "players.yml").toPath(), "players: [unclosed\n");
        StorageMigrator migrator = new StorageMigrator(
                List.of(new StorageMigrator.Part<>(EntityKind.PLAYERS, unreadable, () -> live)),
                current::get,
                current::set,
                dataFolder,
                () -> liveConfig,
                background::push,
                mainThread::push,
                LOGGER);
        CompletableFuture<Map<String, Note>> load = unreadable.load();
        runAll();
        assertThrows(CompletionException.class, load::join);

        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(target(), settings());
        runAll();

        assertTrue(migration.isCompletedExceptionally());
        assertEquals(StorageType.YAML, current.get().type());
    }

    @Test
    void holdsData_saysWhetherTheTargetHasEntries() {
        CompletableFuture<Boolean> empty = migrator.holdsData(target(), settings());
        runAll();
        assertFalse(empty.join());

        live.add(new Note("a", 1));
        migrator.migrate(target(), settings());
        runAll();
        migrator.migrate(StorageType.YAML, settings());
        runAll();
        CompletableFuture<Boolean> filled = migrator.holdsData(target(), settings());
        runAll();
        assertTrue(filled.join());

        CompletableFuture<Boolean> unreachable = migrator.holdsData(StorageType.MYSQL, settings());
        runAll();
        assertThrows(CompletionException.class, unreachable::join);
    }

    @Test
    void holdsData_createsNothing_soTheNextStartStillImportsTheYamlFile() {
        live.add(new Note("a", 1));
        notes.save(live);
        runAll();

        CompletableFuture<Boolean> preview = migrator.holdsData(target(), settings());
        runAll();

        assertFalse(preview.join());
        try (StorageBackend started = StorageBackend.connect(target(), settings(), dataFolder, LOGGER)) {
            assertEquals(
                    List.of("a"),
                    List.copyOf(started.store(EntityKind.PLAYERS).loadAll().keySet()));
        }
    }

    @Test
    void migrate_thatFails_dropsTheTablesItCreated_soTheNextStartStillImportsTheYamlFile() {
        live.add(new Note("a", 1));
        notes.save(live);
        runAll();

        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(target(), settings());
        drain(background);
        migrator.abort();
        runAll();

        assertThrows(CompletionException.class, migration::join);
        try (StorageBackend started = StorageBackend.connect(target(), settings(), dataFolder, LOGGER)) {
            assertEquals(
                    List.of("a"),
                    List.copyOf(started.store(EntityKind.PLAYERS).loadAll().keySet()));
        }
    }

    @Test
    void migrate_toTheStorageInUse_isRefused() {
        live.add(new Note("a", 1));
        migrator.migrate(target(), settings());
        runAll();
        StorageBackend before = current.get();

        CompletableFuture<Boolean> preview = migrator.holdsData(target(), settings());
        CompletableFuture<Map<EntityKind, Integer>> again = migrator.migrate(target(), settings());
        runAll();

        assertThrows(CompletionException.class, preview::join);
        assertThrows(CompletionException.class, again::join);
        assertSame(before, current.get());
        assertEquals(
                List.of("a"),
                List.copyOf(before.collection(EntityKind.PLAYERS).loadAll().keySet()));
    }

    @Test
    void migrate_toATypeWithAnInvalidBlock_failsWithThatBlocksError() {
        String error = "storage.mysql.host must be a host name, an IP address or an IPv6 address in brackets";
        PluginConfig.Storage.Database settings = new PluginConfig.Storage.Database(
                StorageType.SQLITE, prefix, Map.of(), Map.of(StorageType.MYSQL, error));

        CompletableFuture<Boolean> preview = migrator.holdsData(StorageType.MYSQL, settings);
        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(StorageType.MYSQL, settings);
        runAll();

        assertEquals(
                error,
                assertThrows(CompletionException.class, preview::join)
                        .getCause()
                        .getMessage());
        assertEquals(
                error,
                assertThrows(CompletionException.class, migration::join)
                        .getCause()
                        .getMessage());
        assertFalse(migrator.running());
    }

    @Test
    void location_ofMySqlAndMariaDbWithTheSameBlock_isTheSame() {
        PluginConfig.Storage.Database settings = new PluginConfig.Storage.Database(
                StorageType.MYSQL,
                prefix,
                Map.of(
                        StorageType.MYSQL,
                        new PluginConfig.Storage.Database.Server(
                                StorageType.MYSQL, "DB.local", 3306, "buildsystem", "a", "", StorageTls.PREFER),
                        StorageType.MARIADB,
                        new PluginConfig.Storage.Database.Server(
                                StorageType.MARIADB, "db.local", 3306, "buildsystem", "b", "", StorageTls.PREFER)));

        assertEquals(
                StorageBackend.location(StorageType.MYSQL, settings),
                StorageBackend.location(StorageType.MARIADB, settings));
    }

    @Test
    void location_ofTheLocalMachine_isTheSameWhateverItIsCalled() {
        List<String> hosts = List.of("localhost", "LOCALHOST", "127.0.0.1", "[::1]");
        for (String host : hosts) {
            PluginConfig.Storage.Database settings = new PluginConfig.Storage.Database(
                    StorageType.MYSQL,
                    prefix,
                    Map.of(
                            StorageType.MYSQL,
                            new PluginConfig.Storage.Database.Server(
                                    StorageType.MYSQL, "localhost", 3306, "buildsystem", "a", "", StorageTls.PREFER),
                            StorageType.MARIADB,
                            new PluginConfig.Storage.Database.Server(
                                    StorageType.MARIADB, host, 3306, "buildsystem", "b", "", StorageTls.PREFER)));

            assertEquals(
                    StorageBackend.location(StorageType.MYSQL, settings),
                    StorageBackend.location(StorageType.MARIADB, settings),
                    host);
        }
    }

    @Test
    void migrate_whileOneRuns_isRefused() {
        CompletableFuture<Map<EntityKind, Integer>> first = migrator.migrate(target(), settings());

        CompletableFuture<Map<EntityKind, Integer>> second = migrator.migrate(target(), settings());

        assertTrue(second.isCompletedExceptionally());
        runAll();
        assertEquals(Map.of(EntityKind.PLAYERS, 0), first.join());
    }

    @Test
    void abort_letsHeldWritesGoToTheOldBackendAndStopsTheSwitch() {
        StorageBackend before = current.get();
        CompletableFuture<Map<EntityKind, Integer>> migration = migrator.migrate(target(), settings());
        notes.save(new Note("held", 1));

        migrator.abort();
        runAll();

        assertThrows(CompletionException.class, migration::join);
        assertSame(before, current.get());
        assertTrue(EntityKind.PLAYERS.yaml(dataFolder, LOGGER).loadAll().containsKey("held"));
    }

    private void runAll() {
        while (!background.isEmpty() || !mainThread.isEmpty()) {
            drain(background);
            drain(mainThread);
        }
    }

    private static void drain(Deque<Runnable> tasks) {
        while (!tasks.isEmpty()) {
            tasks.pop().run();
        }
    }

    /**
     * {@return the backend the migrations go to}
     */
    protected StorageType target() {
        return StorageType.SQLITE;
    }

    /**
     * {@return the servers to migrate to, holding at least an unreachable MySQL}
     */
    protected Map<StorageType, PluginConfig.Storage.Database.Server> servers() {
        // Nothing listens on port 1.
        return Map.of(
                StorageType.MYSQL,
                new PluginConfig.Storage.Database.Server(
                        StorageType.MYSQL, "localhost", 1, "buildsystem", "builder", "", StorageTls.PREFER));
    }

    private PluginConfig.Storage.Database settings() {
        return new PluginConfig.Storage.Database(StorageType.SQLITE, prefix, servers());
    }
}
