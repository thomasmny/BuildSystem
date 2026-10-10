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

import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.StorageException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One connection to an SQL database, shared by every table and used by one caller at a time. MySQL closes a connection
 * that sat idle too long, so the connection is checked before each use and opened again when it has gone.
 */
@NullMarked
public final class SqlDatabase implements AutoCloseable {

    private static final int VALID_TIMEOUT_SECONDS = 5;

    private final SqlDialect dialect;
    private final Driver driver;
    private final String url;
    private final Properties properties;
    private final String tablePrefix;
    private final Logger logger;

    private @Nullable Connection connection;

    private SqlDatabase(
            SqlDialect dialect, Driver driver, String url, Properties properties, String tablePrefix, Logger logger) {
        this.dialect = dialect;
        this.driver = driver;
        this.url = url;
        this.properties = properties;
        this.tablePrefix = tablePrefix;
        this.logger = logger;
    }

    /**
     * Connects right away, so a wrong address or login fails at startup rather than at the first save.
     *
     * @param url The JDBC url, without credentials, which go in {@code properties}
     * @throws StorageException If the driver is missing or the database cannot be reached
     */
    public static SqlDatabase connect(
            SqlDialect dialect, String url, Properties properties, String tablePrefix, Logger logger) {
        Driver driver;
        try {
            driver = Class.forName(dialect.driverClass())
                    .asSubclass(Driver.class)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new StorageException("The " + dialect.driverClass() + " driver is not available", e);
        }
        SqlDatabase database = new SqlDatabase(dialect, driver, url, properties, tablePrefix, logger);
        database.run(connection -> null);
        return database;
    }

    /**
     * {@return the table for the named collection, created if it does not exist yet}
     */
    public EntityStore collection(String name) {
        String table = tablePrefix + name;
        run(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(dialect.createTable(table));
            }
            return null;
        });
        return new SqlEntityStore(this, dialect, table, logger);
    }

    synchronized <R> R run(SqlWork<R> work) {
        try {
            return work.apply(connection());
        } catch (SQLException e) {
            throw new StorageException("Database request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Runs {@code work} in one transaction, so it either commits whole or not at all.
     */
    synchronized void transaction(SqlWork<Void> work) {
        run(connection -> {
            connection.setAutoCommit(false);
            try {
                work.apply(connection);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            return null;
        });
    }

    private Connection connection() throws SQLException {
        if (connection != null && connection.isValid(VALID_TIMEOUT_SECONDS)) {
            return connection;
        }
        closeQuietly();
        Connection opened = driver.connect(url, properties);
        if (opened == null) {
            throw new SQLException("The " + dialect.driverClass() + " driver does not accept the configured address");
        }
        connection = opened;
        return opened;
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // The connection is being replaced or the plugin is shutting down; either way it is no longer used.
        }
        connection = null;
    }

    @FunctionalInterface
    interface SqlWork<R> {
        R apply(Connection connection) throws SQLException;
    }
}
