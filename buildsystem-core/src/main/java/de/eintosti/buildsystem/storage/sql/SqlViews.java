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

import de.eintosti.buildsystem.storage.EntityKind;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;

/**
 * The read-only views that give tools outside the plugin real columns over the JSON in each table. They are the
 * supported way to query the data: the JSON itself may change between versions, and the plugin rebuilds these on every
 * start to match it.
 */
@NullMarked
final class SqlViews {

    private static final String SQLITE_DATA = "CASE WHEN json_valid(data) THEN data END";
    private static final String POSTGRES_DATA = "CAST(data AS JSON)";

    /**
     * @param kind The table the view reads, which it must not outlive on PostgreSQL
     */
    record View(String name, EntityKind kind, String select) {}

    private SqlViews() {}

    static List<View> of(SqlDialect dialect, String prefix) {
        String worlds = prefix + EntityKind.WORLDS.id();
        String folders = prefix + EntityKind.FOLDERS.id();
        return List.of(
                new View(prefix + "worlds_view", EntityKind.WORLDS, worldsView(dialect, worlds)),
                new View(prefix + "world_builders", EntityKind.WORLDS, worldBuilders(dialect, worlds)),
                new View(prefix + "folders_view", EntityKind.FOLDERS, foldersView(dialect, folders)),
                new View(prefix + "folder_worlds", EntityKind.FOLDERS, folderWorlds(dialect, folders)));
    }

    private static String worldsView(SqlDialect dialect, String table) {
        return "SELECT id, "
                + text(dialect, "name") + " AS name, "
                + text(dialect, "type") + " AS type, "
                + text(dialect, "data", "status") + " AS status, "
                + text(dialect, "data", "visibility") + " AS visibility, "
                + text(dialect, "data", "project") + " AS project, "
                + text(dialect, "data", "permission") + " AS permission, "
                + text(dialect, "creator", "uuid") + " AS creator_id, "
                + text(dialect, "creator", "name") + " AS creator_name, "
                + number(dialect, "date") + " AS created_at, "
                + number(dialect, "data", "last-edited") + " AS last_edited"
                + " FROM " + table + readableRows(dialect);
    }

    private static String foldersView(SqlDialect dialect, String table) {
        return "SELECT id, "
                + text(dialect, "name") + " AS name, "
                + text(dialect, "category") + " AS category, "
                + text(dialect, "parent") + " AS parent_id, "
                + text(dialect, "project") + " AS project, "
                + text(dialect, "permission") + " AS permission, "
                + text(dialect, "creator", "uuid") + " AS creator_id, "
                + text(dialect, "creator", "name") + " AS creator_name, "
                + number(dialect, "creation") + " AS created_at"
                + " FROM " + table + readableRows(dialect);
    }

    private static String worldBuilders(SqlDialect dialect, String table) {
        return switch (dialect) {
            case SQLITE ->
                "SELECT t.id AS world_id, json_extract(e.value, '$.uuid') AS builder_id,"
                        + " json_extract(e.value, '$.name') AS builder_name FROM " + table + " t, "
                        + sqliteElements("builders") + " e WHERE e.type = 'object'";
            case MYSQL ->
                "SELECT t.id AS world_id, e.builder_id COLLATE utf8mb4_bin AS builder_id,"
                        + " e.builder_name COLLATE utf8mb4_bin AS builder_name FROM " + table + " t,"
                        + " JSON_TABLE(t.data, '$.builders[*]' COLUMNS ("
                        + "builder_id VARCHAR(36) CHARACTER SET utf8mb4 PATH '$.uuid',"
                        + " builder_name VARCHAR(255) CHARACTER SET utf8mb4 PATH '$.name')) e"
                        + " WHERE e.builder_id IS NOT NULL";
            case POSTGRESQL ->
                "SELECT t.id AS world_id, e.value ->> 'uuid' AS builder_id, e.value ->> 'name' AS builder_name FROM "
                        + table + " t CROSS JOIN LATERAL json_array_elements(" + postgresArray("builders")
                        + ") AS e(value) WHERE json_typeof(e.value) = 'object'";
        };
    }

    private static String folderWorlds(SqlDialect dialect, String table) {
        return switch (dialect) {
            case SQLITE ->
                "SELECT t.id AS folder_id, e.value AS world_id FROM " + table + " t, " + sqliteElements("worlds")
                        + " e WHERE e.type = 'text'";
            case MYSQL ->
                "SELECT t.id AS folder_id, e.world_id COLLATE utf8mb4_bin AS world_id FROM " + table + " t,"
                        + " JSON_TABLE(t.data, '$.worlds[*]' COLUMNS ("
                        + "world_id VARCHAR(36) CHARACTER SET utf8mb4 PATH '$')) e"
                        + " WHERE e.world_id IS NOT NULL";
            case POSTGRESQL ->
                "SELECT t.id AS folder_id, e.value AS world_id FROM " + table
                        + " t CROSS JOIN LATERAL json_array_elements_text(" + postgresArray("worlds") + ") AS e(value)";
        };
    }

    private static String text(SqlDialect dialect, String... path) {
        return switch (dialect) {
            case SQLITE -> "json_extract(" + SQLITE_DATA + ", '" + jsonPath(path) + "')";
            case MYSQL -> "JSON_UNQUOTE(JSON_EXTRACT(data, '" + jsonPath(path) + "'))";
            case POSTGRESQL -> POSTGRES_DATA + " #>> '" + postgresPath(path) + "'";
        };
    }

    /**
     * {@return the number at {@code path}, or {@code NULL} if it holds something else} Casting anything else would make
     * a number up on SQLite and MySQL, and fail the whole query on PostgreSQL.
     */
    private static String number(SqlDialect dialect, String... path) {
        return switch (dialect) {
            case SQLITE ->
                "CASE WHEN json_type(" + SQLITE_DATA + ", '" + jsonPath(path) + "') IN ('integer', 'real') THEN CAST("
                        + text(dialect, path) + " AS INTEGER) END";
            case MYSQL ->
                "CASE WHEN JSON_TYPE(JSON_EXTRACT(data, '" + jsonPath(path) + "')) IN ('INTEGER', 'UNSIGNED INTEGER',"
                        + " 'DOUBLE', 'DECIMAL') THEN CAST(JSON_EXTRACT(data, '" + jsonPath(path) + "') AS SIGNED) END";
            case POSTGRESQL ->
                "CASE WHEN json_typeof(" + POSTGRES_DATA + " #> '" + postgresPath(path)
                        + "') = 'number' THEN CAST(CAST(" + text(dialect, path) + " AS NUMERIC) AS BIGINT) END";
        };
    }

    /**
     * SQLite has no JSON type, so a table can hold a row that is not JSON, which the plugin skips and keeps. Reading one
     * would fail the whole query, and a condition on the view may run before this one, so {@link #SQLITE_DATA} guards
     * each read as well.
     */
    private static String readableRows(SqlDialect dialect) {
        return dialect == SqlDialect.SQLITE ? " WHERE json_valid(data)" : "";
    }

    private static String sqliteElements(String key) {
        return "json_each(CASE WHEN json_valid(t.data) THEN t.data ELSE '{}' END, '$." + key + "')";
    }

    /**
     * An entry not saved since 4.1 holds its builders as a string, on which {@code json_array_elements} would fail the
     * whole query.
     */
    private static String postgresArray(String key) {
        String value = "CAST(t.data AS JSON) -> '" + key + "'";
        return "CASE WHEN json_typeof(" + value + ") = 'array' THEN " + value + " ELSE CAST('[]' AS JSON) END";
    }

    private static String jsonPath(String... path) {
        return "$" + Arrays.stream(path).map(key -> ".\"" + key + "\"").collect(Collectors.joining());
    }

    private static String postgresPath(String... path) {
        return "{" + String.join(",", path) + "}";
    }
}
