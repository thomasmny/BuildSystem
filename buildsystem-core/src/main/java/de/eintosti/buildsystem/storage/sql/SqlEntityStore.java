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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.SerializedObjects;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;

/**
 * One table of {@code (id, data)} rows, the data being the entry's map as JSON. Rows load in id order.
 */
@NullMarked
final class SqlEntityStore implements EntityStore {

    // Whole numbers must come back as longs: WorldCodec reads the creation date only when it is a Long.
    private static final Gson GSON = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .disableHtmlEscaping()
            .create();
    private static final Type MAP = new TypeToken<Map<String, Object>>() {}.getType();

    private final SqlDatabase database;
    private final String table;
    private final String createTable;
    private final String selectAll;
    private final String selectAny;
    private final String upsert;
    private final String delete;
    private final String deleteAll;
    private final String dropTable;
    private final Logger logger;

    /**
     * @param table Made only of the checked table prefix and an {@link EntityKind} id, the one part of the SQL that
     *     cannot be a parameter
     */
    SqlEntityStore(SqlDatabase database, SqlDialect dialect, String table, Logger logger) {
        this.database = database;
        this.table = table;
        this.createTable = dialect.createTable(table);
        this.selectAll = "SELECT id, data FROM " + table + " ORDER BY id";
        this.selectAny = "SELECT 1 FROM " + table + " LIMIT 1";
        this.upsert = dialect.upsert(table);
        this.delete = "DELETE FROM " + table + " WHERE id = ?";
        this.deleteAll = "DELETE FROM " + table;
        this.dropTable = "DROP TABLE " + table;
        this.logger = logger;
    }

    void createTable() {
        database.run("create " + table, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(createTable);
            }
            return null;
        });
    }

    void dropTable() {
        database.run("drop " + table, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(dropTable);
            }
            return null;
        });
    }

    @Override
    public SequencedMap<String, Map<String, Object>> loadAll() {
        return database.run("load " + table, connection -> {
            SequencedMap<String, Map<String, Object>> entries = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(selectAll);
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String id = rows.getString(1);
                    try {
                        Map<String, Object> values = GSON.fromJson(rows.getString(2), MAP);
                        if (values == null) {
                            throw new JsonParseException("No data");
                        }
                        entries.put(id, SerializedObjects.revive(values));
                    } catch (JsonParseException | IllegalArgumentException e) {
                        logger.log(Level.WARNING, "Skipping row \"" + id + "\" of " + table + ": could not be read", e);
                    }
                }
            }
            return entries;
        });
    }

    @Override
    public boolean isEmpty() {
        return database.run("read " + table, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(selectAny);
                    ResultSet rows = statement.executeQuery()) {
                return !rows.next();
            }
        });
    }

    @Override
    public void upsert(Map<String, Map<String, Object>> entries) {
        if (entries.isEmpty()) {
            return;
        }
        database.transaction("save to " + table, connection -> {
            write(connection, entries);
            return null;
        });
    }

    @Override
    public void replaceAll(Map<String, Map<String, Object>> entries) {
        database.transaction("replace " + table, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(deleteAll)) {
                statement.executeUpdate();
            }
            if (!entries.isEmpty()) {
                write(connection, entries);
            }
            return null;
        });
    }

    private void write(Connection connection, Map<String, Map<String, Object>> entries) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(upsert)) {
            for (Map.Entry<String, Map<String, Object>> entry : entries.entrySet()) {
                statement.setString(1, entry.getKey());
                statement.setString(2, GSON.toJson(SerializedObjects.flatten(entry.getValue())));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Override
    public void delete(String key) {
        database.run("delete from " + table, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(delete)) {
                statement.setString(1, key);
                statement.executeUpdate();
            }
            return null;
        });
    }
}
