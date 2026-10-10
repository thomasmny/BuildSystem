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
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private final String selectAll;
    private final String selectAny;
    private final String upsert;
    private final String delete;
    private final Logger logger;

    /**
     * @param table Made only of the checked table prefix and an {@link EntityKind} id, the one part of the SQL that
     *     cannot be a parameter
     */
    SqlEntityStore(SqlDatabase database, SqlDialect dialect, String table, Logger logger) {
        this.database = database;
        this.table = table;
        this.selectAll = "SELECT id, data FROM " + table + " ORDER BY id";
        this.selectAny = "SELECT 1 FROM " + table + " LIMIT 1";
        this.upsert = dialect.upsert(table);
        this.delete = "DELETE FROM " + table + " WHERE id = ?";
        this.logger = logger;
    }

    @Override
    public SequencedMap<String, Map<String, Object>> loadAll() {
        return database.run(connection -> {
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
                        entries.put(id, values);
                    } catch (JsonParseException e) {
                        logger.log(Level.WARNING, "Skipping row \"" + id + "\" of " + table + ": not a JSON object", e);
                    }
                }
            }
            return entries;
        });
    }

    @Override
    public boolean isEmpty() {
        return database.run(connection -> {
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
        database.transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(upsert)) {
                for (Map.Entry<String, Map<String, Object>> entry : entries.entrySet()) {
                    statement.setString(1, entry.getKey());
                    statement.setString(2, GSON.toJson(entry.getValue()));
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return null;
        });
    }

    @Override
    public void delete(String key) {
        database.run(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(delete)) {
                statement.setString(1, key);
                statement.executeUpdate();
            }
            return null;
        });
    }
}
