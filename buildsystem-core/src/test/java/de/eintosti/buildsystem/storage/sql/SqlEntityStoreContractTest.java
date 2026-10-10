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
import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.EntityStoreContractTest;
import de.eintosti.buildsystem.storage.StorageException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * The contract, plus what only a table can get wrong: a batch that fails part way and a row that is not JSON.
 */
public abstract class SqlEntityStoreContractTest extends EntityStoreContractTest {

    /**
     * {@return a second connection to the database under test, for writing rows the store never would}
     */
    protected abstract SqlDatabase rawDatabase();

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

    protected PluginConfig.Storage.Database settings(String type, JdbcDatabaseContainer<?> container, int port) {
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
    void connectionDroppedByTheServer_isOpenedAgain() {
        store("players").upsert(Map.of("before", Map.of("n", "1")));
        SqlDatabase raw = rawDatabase();
        try {
            raw.run(connection -> {
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
    void rowThatIsNotJson_isSkippedAndKeptThroughLaterSaves() {
        EntityStore players = store("players");
        String table = tablePrefix + "players";
        SqlDatabase raw = rawDatabase();
        try {
            raw.run(connection -> {
                try (PreparedStatement statement =
                        connection.prepareStatement("INSERT INTO " + table + " (id, data) VALUES (?, ?)")) {
                    statement.setString(1, "garbled");
                    statement.setString(2, "{not json");
                    statement.executeUpdate();
                }
                return null;
            });

            players.upsert(Map.of("fine", Map.of("n", "1")));

            assertEquals(Map.of("fine", Map.of("n", "1")), players.loadAll());
            assertEquals("{not json", raw.run(connection -> {
                try (PreparedStatement statement =
                                connection.prepareStatement("SELECT data FROM " + table + " WHERE id = 'garbled'");
                        ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    return rows.getString(1);
                }
            }));
        } finally {
            raw.close();
        }
    }
}
