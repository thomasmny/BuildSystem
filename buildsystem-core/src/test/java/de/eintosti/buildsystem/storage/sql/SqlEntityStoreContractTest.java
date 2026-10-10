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

import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.EntityStoreContractTest;
import de.eintosti.buildsystem.storage.StorageException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The contract, plus what only a table can get wrong: a batch that fails part way and a row that is not JSON.
 */
public abstract class SqlEntityStoreContractTest extends EntityStoreContractTest {

    protected static final String PREFIX = "test_";
    private static final String TABLE = PREFIX + "players";

    protected abstract SqlDatabase database();

    @Override
    protected EntityStore store(String name) {
        return database().collection(name);
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
    void rowThatIsNotJson_isSkippedAndKeptThroughLaterSaves() {
        EntityStore players = store("players");
        database().run(connection -> {
            try (PreparedStatement statement =
                    connection.prepareStatement("INSERT INTO " + TABLE + " (id, data) VALUES (?, ?)")) {
                statement.setString(1, "garbled");
                statement.setString(2, "{not json");
                statement.executeUpdate();
            }
            return null;
        });

        players.upsert(Map.of("fine", Map.of("n", "1")));

        assertEquals(Map.of("fine", Map.of("n", "1")), players.loadAll());
        String raw = database().run(connection -> {
            try (PreparedStatement statement =
                    connection.prepareStatement("SELECT data FROM " + TABLE + " WHERE id = 'garbled'")) {
                ResultSet rows = statement.executeQuery();
                rows.next();
                return rows.getString(1);
            }
        });
        assertEquals("{not json", raw);
    }
}
