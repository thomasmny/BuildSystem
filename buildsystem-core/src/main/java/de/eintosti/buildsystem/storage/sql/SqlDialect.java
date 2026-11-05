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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * What differs between the SQL databases: the driver, the type of the data column and how an upsert is written.
 * MariaDB is reached through the MySQL driver and speaks the MySQL dialect.
 *
 * <p>SQLite has no JSON type, but its JSON functions read text. PostgreSQL uses JSON rather than JSONB, which refuses
 * the escape for NUL: an item's text can hold one, and it would fail every other entry in the same save.
 */
@NullMarked
public enum SqlDialect {
    SQLITE("org.sqlite.JDBC", "TEXT", "", "?", "ON CONFLICT(id) DO UPDATE SET data = excluded.data"),
    // VALUES(data) is deprecated in MySQL 8.0.20 in favour of a row alias, which MariaDB does not support.
    MYSQL(
            "com.mysql.cj.jdbc.Driver",
            "JSON",
            " DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin",
            "?",
            "ON DUPLICATE KEY UPDATE data = VALUES(data)"),
    POSTGRESQL(
            "org.postgresql.Driver",
            "JSON",
            "",
            "CAST(? AS JSON)",
            "ON CONFLICT(id) DO UPDATE SET data = excluded.data");

    private final String driverClass;
    private final String dataType;
    private final String tableOptions;
    private final String dataParameter;
    private final String onConflict;

    SqlDialect(String driverClass, String dataType, String tableOptions, String dataParameter, String onConflict) {
        this.driverClass = driverClass;
        this.dataType = dataType;
        this.tableOptions = tableOptions;
        this.dataParameter = dataParameter;
        this.onConflict = onConflict;
    }

    String driverClass() {
        return driverClass;
    }

    String createTable(String table) {
        return "CREATE TABLE IF NOT EXISTS " + table + " (id VARCHAR(64) NOT NULL PRIMARY KEY, data " + dataType
                + " NOT NULL)" + tableOptions;
    }

    String upsert(String table) {
        return "INSERT INTO " + table + " (id, data) VALUES (?, " + dataParameter + ") " + onConflict;
    }

    /**
     * {@return the statement that gives the data column of a table from before format 1 a JSON type, or {@code null} on
     * SQLite, which has none}
     */
    @Nullable String convertDataToJson(String table) {
        return switch (this) {
            case SQLITE -> null;
            case MYSQL -> "ALTER TABLE " + table + " MODIFY data JSON NOT NULL";
            case POSTGRESQL -> "ALTER TABLE " + table + " ALTER COLUMN data TYPE JSON USING CAST(data AS JSON)";
        };
    }
}
