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
package de.eintosti.buildsystem.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.EntityStoreContractTest;
import de.eintosti.buildsystem.storage.StorageBackend;
import de.eintosti.buildsystem.storage.StorageException;
import de.eintosti.buildsystem.storage.StorageType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * The contract, plus what only a table can get wrong: a batch that fails part way, a row that is not an object, the column type and the views.
 */
public abstract class SqlEntityStoreContractTest extends EntityStoreContractTest {

    /**
     * {@return a second connection to the database under test, for writing rows the store never would}
     */
    protected abstract SqlDatabase rawDatabase();

    /**
     * {@return a value that loads as an unreadable row} Where the column has a JSON type this is JSON, just not an
     * object, since the database refuses anything else.
     */
    protected String garbledRow() {
        return "[\"not an object\"]";
    }

    /**
     * {@return whether the data column has a JSON type, which SQLite lacks}
     */
    protected boolean jsonColumn() {
        return true;
    }

    /**
     * {@return how the value of the data column is written in an INSERT}
     */
    protected String dataParameter() {
        return "?";
    }

    private void insert(SqlDatabase raw, String table, String id, String data) {
        raw.run("test", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO " + table + " (id, data) VALUES (?, " + dataParameter() + ")")) {
                statement.setString(1, id);
                statement.setString(2, data);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private void execute(SqlDatabase raw, String sql) {
        raw.run("test", connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            return null;
        });
    }

    /**
     * {@return every row of the query, each as its columns in order}
     */
    private List<List<@Nullable String>> query(SqlDatabase raw, String sql) {
        return raw.run("test", connection -> {
            List<List<@Nullable String>> rows = new ArrayList<>();
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(sql)) {
                int columns = result.getMetaData().getColumnCount();
                while (result.next()) {
                    List<@Nullable String> row = new ArrayList<>();
                    for (int column = 1; column <= columns; column++) {
                        row.add(result.getString(column));
                    }
                    rows.add(row);
                }
            }
            return rows;
        });
    }

    /**
     * {@return a connection to a database in a container, as {@code raw} or as the plugin would open it}
     */
    protected static SqlDatabase connect(
            SqlDialect dialect, String scheme, JdbcDatabaseContainer<?> container, int port, String tablePrefix) {
        Properties credentials = new Properties();
        credentials.setProperty("user", container.getUsername());
        credentials.setProperty("password", container.getPassword());
        String url = "jdbc:" + scheme + "://" + container.getHost() + ":" + container.getMappedPort(port) + "/"
                + container.getDatabaseName();
        return SqlDatabase.connect(dialect, url, credentials, tablePrefix, LOGGER);
    }

    protected PluginConfig.Storage.Database settings(StorageType type, JdbcDatabaseContainer<?> container, int port) {
        return settings(
                type,
                container.getHost(),
                container.getMappedPort(port),
                container.getDatabaseName(),
                container.getUsername(),
                container.getPassword(),
                tablePrefix);
    }

    /**
     * Has the database server close every other connection to it, as MySQL does to one left idle too long.
     */
    protected void dropOtherConnections(Connection connection) throws SQLException {
        Assumptions.abort("no server to drop the connection");
    }

    @Override
    protected List<String> loadOrder(List<String> saved) {
        return saved.stream().sorted().toList();
    }

    @Test
    void batchFailingPartWay_writesNone() {
        store("players").upsert(Map.of("kept", Map.of("n", "1")));
        Map<String, Map<String, Object>> batch = new LinkedHashMap<>();
        batch.put("kept", Map.of("n", "2"));
        batch.put("added", Map.of("n", "3"));
        // The id column is NOT NULL, so the database rejects this row after it took the two before it.
        batch.put(null, Map.of("n", "4"));

        assertThrows(StorageException.class, () -> store("players").upsert(batch));

        assertEquals(Map.of("kept", Map.of("n", "1")), store("players").loadAll());
    }

    @Test
    void replaceAllFailingPartWay_keepsWhatWasThere() {
        store("players").upsert(Map.of("kept", Map.of("n", "1")));
        Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
        entries.put("added", Map.of("n", "2"));
        entries.put(null, Map.of("n", "3"));

        assertThrows(StorageException.class, () -> store("players").replaceAll(entries));

        assertEquals(Map.of("kept", Map.of("n", "1")), store("players").loadAll());
    }

    @Test
    void connectionDroppedByTheServer_isOpenedAgain() {
        store("players").upsert(Map.of("before", Map.of("n", "1")));
        SqlDatabase raw = rawDatabase();
        try {
            raw.run("test", connection -> {
                dropOtherConnections(connection);
                return null;
            });
        } finally {
            raw.close();
        }

        store("players").upsert(Map.of("after", Map.of("n", "2")));

        assertEquals(
                Map.of("after", Map.of("n", "2"), "before", Map.of("n", "1")),
                store("players").loadAll());
    }

    @Test
    void rowThatIsNotAnObject_isSkippedAndKeptThroughLaterSaves() {
        EntityStore players = store("players");
        String table = tablePrefix + "players";
        SqlDatabase raw = rawDatabase();
        try {
            insert(raw, table, "garbled", garbledRow());
            insert(raw, table, "unrevivable", "{\"item\":{\"==\":\"com.example.UnknownItem\"}}");

            players.upsert(Map.of("fine", Map.of("n", "1")));

            assertEquals(Map.of("fine", Map.of("n", "1")), players.loadAll());
            assertEquals(2, players.unreadable());
            assertEquals(
                    List.of(List.of(garbledRow())), query(raw, "SELECT data FROM " + table + " WHERE id = 'garbled'"));
        } finally {
            raw.close();
        }
    }

    @Test
    void aTableFromBeforeTheFormatVersion_isUpgraded_andKeepsItsRows() {
        String table = tablePrefix + "players";
        SqlDatabase raw = rawDatabase();
        try {
            execute(raw, "CREATE TABLE " + table + " (id VARCHAR(64) NOT NULL PRIMARY KEY, data TEXT NOT NULL)");
            insert(raw, table, "kept", "{\"n\":\"1\"}");

            assertEquals(Map.of("kept", Map.of("n", "1")), store("players").loadAll());

            assertEquals(StorageBackend.FORMAT_VERSION, storedFormat());
            if (jsonColumn()) {
                assertThrows(StorageException.class, () -> insert(raw, table, "refused", "{not json"));
            }
        } finally {
            raw.close();
        }
    }

    @Test
    void aTableThatCannotBeConverted_stillWorks_withoutViews_untilTheRowIsFixed() {
        String table = tablePrefix + "worlds";
        SqlDatabase raw = rawDatabase();
        try {
            execute(raw, "CREATE TABLE " + table + " (id VARCHAR(64) NOT NULL PRIMARY KEY, data TEXT NOT NULL)");
            execute(raw, "INSERT INTO " + table + " (id, data) VALUES ('garbled', '{not json')");

            store("worlds").upsert(Map.of("fine", Map.of("name", "fine")));
            backend.prepare();

            assertEquals(Map.of("fine", Map.of("name", "fine")), store("worlds").loadAll());
            assertEquals(
                    List.of(List.of("{not json")), query(raw, "SELECT data FROM " + table + " WHERE id = 'garbled'"));
            if (!jsonColumn()) {
                return;
            }
            assertEquals(0, storedFormat(), "the conversion is tried again on the next start");
            assertThrows(StorageException.class, () -> query(raw, "SELECT * FROM " + tablePrefix + "worlds_view"));

            execute(raw, "DELETE FROM " + table + " WHERE id = 'garbled'");
            restart();
            store("worlds");
            backend.prepare();

            assertEquals(StorageBackend.FORMAT_VERSION, storedFormat());
            assertEquals(
                    List.of(List.of("fine", "fine")),
                    query(raw, "SELECT id, name FROM " + tablePrefix + "worlds_view"));
            assertThrows(StorageException.class, () -> insert(raw, table, "refused", "{not json"));
        } finally {
            raw.close();
        }
    }

    @Test
    void anUpgradeThatStoppedAtOneTable_finishesOnceItsRowIsFixed_althoughTheOtherHasViews() {
        String worlds = tablePrefix + "worlds";
        String folders = tablePrefix + "folders";
        SqlDatabase raw = rawDatabase();
        try {
            execute(raw, "CREATE TABLE " + worlds + " (id VARCHAR(64) NOT NULL PRIMARY KEY, data TEXT NOT NULL)");
            insert(raw, worlds, "fine", "{\"name\":\"fine\"}");
            execute(raw, "CREATE TABLE " + folders + " (id VARCHAR(64) NOT NULL PRIMARY KEY, data TEXT NOT NULL)");
            execute(raw, "INSERT INTO " + folders + " (id, data) VALUES ('garbled', '{not json')");

            store("worlds");
            backend.prepare();
            if (!jsonColumn()) {
                return;
            }
            assertEquals(0, storedFormat());

            execute(raw, "DELETE FROM " + folders + " WHERE id = 'garbled'");
            restart();
            store("worlds");
            backend.prepare();

            assertEquals(StorageBackend.FORMAT_VERSION, storedFormat());
            assertEquals(
                    List.of(List.of("fine", "fine")),
                    query(raw, "SELECT id, name FROM " + tablePrefix + "worlds_view"));
        } finally {
            raw.close();
        }
    }

    private static final String ALEX = "0c0c0c0c-0000-4000-8000-000000000001";
    private static final String SAM = "0c0c0c0c-0000-4000-8000-000000000002";

    private void storeSampleData() {
        Map<String, Object> worldData = new LinkedHashMap<>();
        worldData.put("status", "finished");
        worldData.put("visibility", "PUBLIC");
        worldData.put("project", "Arena's");
        worldData.put("permission", "-");
        worldData.put("last-edited", 1_700_000_000_123L);
        Map<String, Object> world = new LinkedHashMap<>();
        world.put("name", "maps:arena");
        world.put("type", "NORMAL");
        world.put("creator", Map.of("uuid", ALEX, "name", "Alex"));
        world.put("date", 1_700_000_000_000L);
        world.put("builders", List.of(Map.of("uuid", ALEX, "name", "Alex"), Map.of("uuid", SAM, "name", "Sam")));
        world.put("data", worldData);
        // As a world imported from a YAML file looks until its first save.
        Map<String, Object> notSavedYet = new LinkedHashMap<>();
        notSavedYet.put("name", "old");
        notSavedYet.put("creator", ALEX + ",Alex");
        notSavedYet.put("date", "not a number");
        notSavedYet.put("builders", ALEX + ",Alex");
        store("worlds").upsert(Map.of("arena", world, "old", notSavedYet));

        Map<String, Object> folder = new LinkedHashMap<>();
        folder.put("name", "Maps");
        folder.put("category", "public");
        folder.put("parent", "top");
        folder.put("project", "-");
        folder.put("permission", "-");
        folder.put("creator", Map.of("uuid", SAM, "name", "Sam"));
        folder.put("creation", 1_600_000_000_000L);
        folder.put("worlds", List.of("arena", "old"));
        store("folders").upsert(Map.of("maps", folder));
    }

    @Test
    void views_showTheDataInColumns() {
        storeSampleData();
        store("players");
        backend.prepare();

        SqlDatabase raw = rawDatabase();
        try {
            assertEquals(
                    List.of(List.of(
                            "arena",
                            "maps:arena",
                            "NORMAL",
                            "finished",
                            "PUBLIC",
                            "Arena's",
                            "-",
                            ALEX,
                            "Alex",
                            "1700000000000",
                            "1700000000123")),
                    query(raw, "SELECT * FROM " + tablePrefix + "worlds_view WHERE name = 'maps:arena'"));
            assertEquals(
                    List.of(List.of("arena", ALEX, "Alex"), List.of("arena", SAM, "Sam")),
                    query(raw, "SELECT * FROM " + tablePrefix + "world_builders ORDER BY builder_name"));
            assertEquals(
                    List.of(List.of("maps", "Maps", "public", "top", "-", "-", SAM, "Sam", "1600000000000")),
                    query(raw, "SELECT * FROM " + tablePrefix + "folders_view"));
            assertEquals(
                    List.of(List.of("maps", "arena"), List.of("maps", "old")),
                    query(raw, "SELECT * FROM " + tablePrefix + "folder_worlds ORDER BY world_id"));
        } finally {
            raw.close();
        }
    }

    @Test
    void views_canBeJoined() {
        storeSampleData();
        store("players");
        backend.prepare();

        SqlDatabase raw = rawDatabase();
        try {
            assertEquals(
                    List.of(List.of("maps:arena")),
                    query(
                            raw,
                            "SELECT w.name FROM " + tablePrefix + "worlds_view w JOIN " + tablePrefix
                                    + "world_builders b ON b.world_id = w.id WHERE b.builder_id = '" + SAM + "'"));
            assertEquals(
                    List.of(List.of("Maps", "maps:arena"), List.of("Maps", "old")),
                    query(
                            raw,
                            "SELECT f.name, w.name FROM " + tablePrefix + "folders_view f JOIN " + tablePrefix
                                    + "folder_worlds m ON m.folder_id = f.id JOIN " + tablePrefix
                                    + "worlds_view w ON w.id = m.world_id ORDER BY w.name"));
        } finally {
            raw.close();
        }
    }

    @Test
    void views_leaveOutWhatIsNotInTheCurrentShape_withoutFailing() {
        storeSampleData();
        store("players");
        SqlDatabase raw = rawDatabase();
        try {
            insert(raw, tablePrefix + "worlds", "garbled", garbledRow());
            backend.prepare();

            assertEquals(
                    List.of(Arrays.asList("old", null, null)),
                    query(
                            raw,
                            "SELECT name, creator_id, created_at FROM " + tablePrefix
                                    + "worlds_view WHERE name = 'old'"));
            assertEquals(
                    List.of(),
                    query(
                            raw,
                            "SELECT builder_id FROM " + tablePrefix + "world_builders WHERE world_id IN ('old',"
                                    + " 'garbled')"));
        } finally {
            raw.close();
        }
    }

    @Test
    void droppingATable_dropsItsViews() {
        storeSampleData();
        store("players");
        backend.prepare();

        backend.drop(EntityKind.WORLDS);

        SqlDatabase raw = rawDatabase();
        try {
            assertThrows(StorageException.class, () -> query(raw, "SELECT * FROM " + tablePrefix + "worlds_view"));
            assertEquals(
                    1,
                    query(raw, "SELECT * FROM " + tablePrefix + "folders_view").size());
        } finally {
            raw.close();
        }
    }
}
