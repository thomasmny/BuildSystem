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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.storage.codec.Codec;
import de.eintosti.buildsystem.storage.codec.FolderCodec;
import de.eintosti.buildsystem.storage.codec.PlayerCodec;
import de.eintosti.buildsystem.storage.codec.WorldCodec;
import de.eintosti.buildsystem.storage.yaml.YamlEntityStore;
import de.eintosti.buildsystem.storage.yaml.YamlStore;
import de.eintosti.buildsystem.test.TestData;
import de.eintosti.buildsystem.world.WorldContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * What every backend must do with the entities the codecs produce. Each test saves through the real storages and reads
 * back through fresh ones, as a restart would, and the entities must come back as they do from the YAML files.
 */
@NullMarked
public abstract class EntityStoreContractTest {

    protected static final Logger LOGGER = Logger.getLogger("EntityStoreContractTest");

    private static final String FULL_WORLD = "0a0a0a0a-0000-4000-8000-000000000001";
    private static final String UNRESOLVED_WORLD = "0a0a0a0a-0000-4000-8000-000000000003";
    private static final String FULL_PLAYER = "0b0b0b0b-0000-4000-8000-000000000001";
    private static final String MINIMAL_PLAYER = "0b0b0b0b-0000-4000-8000-000000000002";

    @TempDir
    File yamlFolder;

    private final WorldContext context = TestData.worldContext();
    private final WorldCodec worldCodec = new WorldCodec(context, mock(PlayerLookupService.class));
    private final FolderCodec folderCodec = new FolderCodec(context, TestData.categoryRegistry());
    private final PlayerCodec playerCodec = new PlayerCodec(LOGGER);

    /**
     * The plugin folder the backend is opened in, which also holds the YAML files it imports.
     */
    @TempDir
    protected File dataFolder;

    /**
     * Unique to each test, so tests sharing one database server never see each other's tables.
     */
    protected final String tablePrefix = "t" + UUID.randomUUID().toString().substring(0, 8) + "_";

    private StorageBackend backend;

    /**
     * {@return the {@code storage} settings that select the backend under test}
     */
    protected abstract PluginConfig.Storage.Database settings();

    /**
     * {@return the order the backend loads entries in that were saved in the given order}
     */
    protected abstract List<String> loadOrder(List<String> saved);

    @BeforeEach
    void open() {
        MockBukkit.mock();
        // The stored chunk generator only resolves when its plugin is installed.
        MockBukkit.createMockPlugin("Terra");
        backend = StorageBackend.connect(settings().type(), settings(), dataFolder, LOGGER);
    }

    @AfterEach
    void close() {
        backend.close();
        MockBukkit.unmock();
    }

    /**
     * {@return the backend's store for the named collection} Every store of one name sees what the others wrote.
     */
    protected EntityStore store(String name) {
        return backend.store(EntityKind.valueOf(name.toUpperCase(Locale.ROOT)));
    }

    protected static PluginConfig.Storage.Database settings(
            StorageType type, String host, int port, String database, String username, String password, String prefix) {
        Map<StorageType, PluginConfig.Storage.Database.Server> servers = type.hasServer()
                ? Map.of(type, new PluginConfig.Storage.Database.Server(type, host, port, database, username, password))
                : Map.of();
        return new PluginConfig.Storage.Database(type, prefix, servers);
    }

    @Test
    void worlds_loadAsTheyDoFromYaml() {
        Collection<BuildWorld> fixtures = worlds(
                        fixture("worlds", "/codec/worlds-4.0.yml", "/storage/worlds-unresolved.yml"))
                .load()
                .join();
        worlds(store("worlds")).save(fixtures).join();
        worlds(yaml("worlds")).save(fixtures).join();

        Map<String, Map<String, Object>> loaded =
                serialized(worldCodec, worlds(store("worlds")).load().join());

        assertEquals(serialized(worldCodec, worlds(yaml("worlds")).load().join()), loaded);
        assertEquals(3, loaded.size());
        assertEquals(1_700_000_000_000L, loaded.get(FULL_WORLD).get("date"));
        Map<?, ?> fullData = (Map<?, ?>) loaded.get(FULL_WORLD).get("data");
        assertEquals(42, fullData.get(WorldDataKey.TIME_SINCE_BACKUP.id()));
        assertEquals(1_700_000_000_300L, fullData.get(WorldDataKey.LAST_UNLOADED.id()));
        assertEquals(true, ((Map<?, ?>) fullData.get("physics-exceptions")).get("falling-blocks"));
        Map<?, ?> unresolvedData = (Map<?, ?>) loaded.get(UNRESOLVED_WORLD).get("data");
        assertEquals(
                List.of("retired", "NOT_A_BLOCK", "BRUTAL"),
                List.of(
                        unresolvedData.get("status"),
                        unresolvedData.get("material"),
                        unresolvedData.get("difficulty")));
    }

    @Test
    void folders_loadAsTheyDoFromYaml() {
        Collection<Folder> fixtures = folders(
                        fixture("folders", "/codec/folders-4.0.yml", "/storage/folders-unresolved.yml"))
                .load()
                .join();
        folders(store("folders")).save(fixtures).join();
        folders(yaml("folders")).save(fixtures).join();

        Map<String, Map<String, Object>> loaded =
                serialized(folderCodec, folders(store("folders")).load().join());

        assertEquals(serialized(folderCodec, folders(yaml("folders")).load().join()), loaded);
        Map<String, Object> full = loaded.get("0f0f0f0f-0000-4000-8000-000000000001");
        assertEquals(1_700_000_000_000L, full.get("creation"));
        assertEquals(
                List.of("0d0d0d0d-0000-4000-8000-000000000001", "0d0d0d0d-0000-4000-8000-000000000002"),
                full.get("worlds"));
        assertEquals(
                List.of(), loaded.get("0f0f0f0f-0000-4000-8000-000000000003").get("worlds"));
        Map<String, Object> unresolved = loaded.get("0f0f0f0f-0000-4000-8000-000000000004");
        assertEquals(List.of("secret", "NOT_A_BLOCK"), List.of(unresolved.get("category"), unresolved.get("material")));
    }

    @Test
    void players_loadAsTheyDoFromYaml() {
        Collection<BuildPlayer> fixtures =
                players(fixture("players", "/codec/players-4.0.yml")).load().join();
        players(store("players")).save(fixtures).join();
        players(yaml("players")).save(fixtures).join();

        Map<String, Map<String, Object>> loaded =
                serialized(playerCodec, players(store("players")).load().join());

        assertEquals(serialized(playerCodec, players(yaml("players")).load().join()), loaded);
        Map<?, ?> settings = (Map<?, ?>) loaded.get(FULL_PLAYER).get("settings");
        assertEquals(
                List.of(true, false, "NEW"),
                List.of(settings.get("no-clip"), settings.get("scoreboard"), settings.get("type")));
        assertEquals("lobby:1.5:64.0:-3.25:90.0:-10.0", loaded.get(FULL_PLAYER).get("logout-location"));
    }

    @Test
    void savingAgain_replacesTheEntry() {
        BuildWorld world = minimalWorld();
        world.getData().set(WorldDataKey.PROJECT, "first");
        worlds(store("worlds")).save(world).join();
        world.getData().set(WorldDataKey.PROJECT, "second");
        worlds(store("worlds")).save(world).join();

        Collection<BuildWorld> loaded = worlds(store("worlds")).load().join();

        assertEquals(
                List.of("second"),
                loaded.stream().map(w -> w.getData().get(WorldDataKey.PROJECT)).toList());
    }

    @Test
    void delete_removesOnlyThatEntry() {
        Collection<BuildPlayer> fixtures =
                players(fixture("players", "/codec/players-4.0.yml")).load().join();
        players(store("players")).save(fixtures).join();

        players(store("players")).delete(FULL_PLAYER).join();

        assertEquals(
                List.of(MINIMAL_PLAYER), List.copyOf(store("players").loadAll().keySet()));
    }

    @Test
    void entries_loadInTheSameOrderEveryTime() {
        List<String> saved = List.of(
                "0b0b0b0b-0000-4000-8000-000000000003",
                "0b0b0b0b-0000-4000-8000-000000000001",
                "0b0b0b0b-0000-4000-8000-000000000002");
        Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
        saved.forEach(key -> entries.put(key, Map.of("settings", Map.of("no-clip", true))));
        store("players").upsert(entries);

        List<String> first = List.copyOf(store("players").loadAll().keySet());

        assertEquals(loadOrder(saved), first);
        assertEquals(first, List.copyOf(store("players").loadAll().keySet()));
        assertEquals(saved.size(), players(store("players")).load().join().size());
    }

    @Test
    void unparseableEntry_isSkippedAndKeptThroughLaterSaves() {
        store("worlds").upsert(Map.of("not-a-uuid", Map.of("name", "broken")));
        worlds(store("worlds")).save(minimalWorld()).join();

        Collection<BuildWorld> loaded = worlds(store("worlds")).load().join();
        worlds(store("worlds")).save(loaded).join();

        assertEquals(List.of("plain"), loaded.stream().map(BuildWorld::getName).toList());
        assertEquals(Map.of("name", "broken"), store("worlds").loadAll().get("not-a-uuid"));
    }

    @Test
    void legacyYamlFiles_areImportedOnceAndKept() throws IOException {
        for (String file : List.of("worlds.yml", "folders.yml")) {
            try (InputStream in = getClass().getResourceAsStream("/upgrade/3.0.2/" + file)) {
                Files.copy(in, new File(dataFolder, file).toPath());
            }
        }

        EntityStore worlds = backend.store(EntityKind.WORLDS);
        Collection<Folder> folders =
                folders(backend.store(EntityKind.FOLDERS)).load().join();

        Map<String, Map<String, Object>> imported = worlds.loadAll();
        assertEquals(7, imported.size());
        assertEquals(
                "lobby", imported.get("0e7a1aca-d915-4fb9-84b1-30f8b95cc373").get("name"));
        assertEquals(4, folders.size());
        assertEquals(
                2,
                folders.stream().filter(folder -> folder.getParent() != null).count(),
                "parents were re-keyed to UUIDs before the import");
        assertTrue(new File(dataFolder, "worlds.yml").exists());
        assertTrue(new File(dataFolder, "worlds.yml.v3.bak").exists());

        worlds.delete("0e7a1aca-d915-4fb9-84b1-30f8b95cc373");

        assertEquals(6, backend.store(EntityKind.WORLDS).loadAll().size());
    }

    @Test
    void replaceAll_leavesOnlyTheGivenEntries() {
        store("players").upsert(Map.of("a", Map.of("n", "1"), "b", Map.of("n", "2")));

        store("players").replaceAll(Map.of("c", Map.of("n", "3")));

        assertEquals(Map.of("c", Map.of("n", "3")), store("players").loadAll());
    }

    @Test
    void hostileKeysAndValues_roundTripExactly() {
        // No dots: a key is always a UUID, and a dot would nest the entry in YAML.
        List<String> hostile = List.of(
                "x'); DROP TABLE " + tablePrefix + "players; --",
                "'; DROP TABLE " + tablePrefix + "worlds; --",
                "\" OR \"1\"=\"1",
                "\" OR 1=1 --",
                "$where",
                "{\"$ne\": null}",
                "{\"$gt\": \"\"}",
                "back\\slash\\'",
                "%_*?",
                "Case",
                "case",
                "ünïcødé 名前 🧱",
                "x".repeat(64));
        Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
        for (String value : hostile) {
            entries.put(value, Map.of("name", value, "nested", Map.of("text", value, "list", List.of(value, "plain"))));
        }
        // Only as a value: PostgreSQL text cannot hold NUL, and no key ever does, being a UUID. Not for YAML, which
        // writes
        // such a string as !!binary and reads it back as bytes; chat input and world names cannot carry NUL anyway.
        if (settings().type() != StorageType.YAML) {
            entries.put("nul", Map.of("name", "before\0after"));
        }

        store("players").upsert(entries);
        store("worlds").upsert(Map.of("kept", Map.of("name", "kept")));
        store("players").delete(hostile.getFirst());
        entries.remove(hostile.getFirst());

        assertEquals(entries, store("players").loadAll());
        assertEquals(Map.of("kept", Map.of("name", "kept")), store("worlds").loadAll());
    }

    @Test
    void bukkitObjects_comeBackAsObjects() {
        // Stands in for the items of an archive snapshot, which need a running server to build.
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("items", Arrays.asList(new Vector(1, 2.5, -3), null, new BlockVector(4, 5, 6)));
        values.put("nested", Map.of("color", Color.fromRGB(10, 20, 30)));

        store("players").upsert(Map.of("holder", values));

        assertEquals(values, store("players").loadAll().get("holder"));
    }

    @Test
    void missingYamlFile_isOnlyCreatedForYaml() {
        store("players").loadAll();

        assertEquals(settings().type() == StorageType.YAML, new File(dataFolder, "players.yml").exists());
    }

    private BuildWorld minimalWorld() {
        return worlds(fixture("worlds", "/codec/worlds-4.0.yml")).load().join().stream()
                .filter(world -> world.getName().equals("plain"))
                .findFirst()
                .orElseThrow();
    }

    private WorldStorageImpl worlds(EntityStore store) {
        return new WorldStorageImpl(LOGGER, () -> "minecraft", collection(store, worldCodec));
    }

    private FolderStorageImpl folders(EntityStore store) {
        return new FolderStorageImpl(LOGGER, mock(WorldStorage.class), () -> context, collection(store, folderCodec));
    }

    private PlayerStorageImpl players(EntityStore store) {
        return new PlayerStorageImpl(LOGGER, collection(store, playerCodec));
    }

    private static <T> EntityCollection<T> collection(EntityStore store, Codec<T> codec) {
        return new EntityCollection<>(store, "entry", () -> codec, Runnable::run, LOGGER);
    }

    /**
     * {@return a YAML store, in a folder of its own, which the backend's results are compared against}
     */
    private EntityStore yaml(String name) {
        return yamlStore(new File(yamlFolder, "reference"), name);
    }

    /**
     * {@return a YAML store holding the entries of the given codec fixtures}
     */
    private EntityStore fixture(String name, String... resources) {
        EntityStore store = yamlStore(new File(yamlFolder, "fixture-" + UUID.randomUUID()), name);
        for (String resource : resources) {
            store.upsert(entries(resource));
        }
        return store;
    }

    protected static YamlEntityStore yamlStore(File folder, String name) {
        folder.mkdirs();
        return new YamlEntityStore(new YamlStore(folder, name + ".yml", LOGGER), name, LOGGER, null);
    }

    private static Map<String, Map<String, Object>> entries(String resource) {
        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStream in = EntityStoreContractTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource " + resource);
            }
            yaml.loadFromString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
        for (String key : yaml.getKeys(false)) {
            entries.put(key, asMap(yaml.getConfigurationSection(key)));
        }
        return entries;
    }

    private static Map<String, Object> asMap(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        section.getValues(false)
                .forEach((key, value) ->
                        values.put(key, value instanceof ConfigurationSection nested ? asMap(nested) : value));
        return values;
    }

    private static <T> Map<String, Map<String, Object>> serialized(Codec<T> codec, Collection<T> entities) {
        Map<String, Map<String, Object>> serialized = new TreeMap<>();
        for (T entity : new ArrayList<>(entities)) {
            serialized.put(codec.key(entity), codec.serialize(entity));
        }
        return serialized;
    }
}
