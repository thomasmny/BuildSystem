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

import de.eintosti.buildsystem.storage.Database;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.StorageException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One connection to an SQL database, shared by every table and used by one caller at a time. MySQL closes a connection
 * that sat idle too long, so the connection is checked before each use and opened again when it has gone.
 */
@NullMarked
public final class SqlDatabase implements Database {

    private static final int VALID_TIMEOUT_SECONDS = 5;
    private static final String METADATA = "meta";

    private final SqlDialect dialect;
    private final Driver driver;
    private final String url;
    private final Properties properties;
    private final String tablePrefix;
    private final Logger logger;

    private @Nullable Connection connection;
    private boolean closed;
    private final Set<EntityKind> textColumns = EnumSet.noneOf(EntityKind.class);

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
        database.run("connect to the database", connection -> null);
        return database;
    }

    @Override
    public EntityStore collection(EntityKind kind) {
        SqlEntityStore store = table(kind);
        store.createTable();
        return store;
    }

    private SqlEntityStore table(EntityKind kind) {
        // The only identifier in the SQL, and it cannot be a parameter. It is safe to splice in: the prefix was checked
        // against [a-z_][a-z0-9_]{0,31}| when the config was parsed, and the rest is the lowercase name of an enum
        // constant.
        return new SqlEntityStore(this, dialect, tablePrefix + kind.id(), logger);
    }

    @Override
    public boolean exists(EntityKind kind) {
        return tableExists(tablePrefix + kind.id());
    }

    private boolean tableExists(String table) {
        return run("look for " + table, connection -> {
            // The name is a LIKE pattern, where _ matches any character, so only an exact match counts.
            try (ResultSet tables = connection
                    .getMetaData()
                    .getTables(connection.getCatalog(), connection.getSchema(), table, new String[] {"TABLE"})) {
                while (tables.next()) {
                    if (tables.getString("TABLE_NAME").equalsIgnoreCase(table)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void drop(EntityKind kind) {
        // PostgreSQL refuses to drop a table that a view reads.
        views().stream().filter(view -> view.kind() == kind).forEach(this::dropView);
        table(kind).dropTable();
    }

    @Override
    public boolean hasMetadata() {
        return tableExists(tablePrefix + METADATA);
    }

    @Override
    public EntityStore metadata() {
        SqlEntityStore store = new SqlEntityStore(this, dialect, tablePrefix + METADATA, logger);
        store.createTable();
        return store;
    }

    /**
     * A row that is not JSON makes the conversion fail. It is then left as it is, since the plugin skips and keeps such
     * a row, and the data column still works as text. Views over it would fail on that row, so it gets none.
     */
    @Override
    public synchronized boolean upgradeFormat(int from) {
        if (from >= 1) {
            return true;
        }
        // A start whose upgrade did not finish still created views over the tables it converted. PostgreSQL refuses to
        // change a column that a view reads, and createViews makes them again.
        for (SqlViews.View view : views()) {
            try {
                dropView(view);
            } catch (StorageException e) {
                // A user may lack the right to drop views. If one is in the way, the conversion fails and says so.
            }
        }
        for (EntityKind kind : EntityKind.values()) {
            String table = tablePrefix + kind.id();
            String convert = dialect.convertDataToJson(table);
            if (convert != null && exists(kind)) {
                try {
                    execute("convert " + table, convert);
                } catch (StorageException e) {
                    textColumns.add(kind);
                    logger.warning("Keeping the data column of " + table + " as text, without views, until a row"
                            + " that is not JSON is fixed or deleted. Tried again on every start: " + e.getMessage());
                }
            }
        }
        return textColumns.isEmpty();
    }

    /**
     * Failing only warns: the plugin does not read the views, and a user without the right to create views, or an older
     * server without {@code JSON_TABLE}, is no reason to stop the server.
     */
    @Override
    public synchronized void createViews() {
        List<SqlViews.View> views = views();
        Set<EntityKind> readable = EnumSet.noneOf(EntityKind.class);
        try {
            for (EntityKind kind :
                    views.stream().map(SqlViews.View::kind).distinct().toList()) {
                if (!textColumns.contains(kind) && exists(kind)) {
                    readable.add(kind);
                }
            }
        } catch (StorageException e) {
            logger.warning("Could not create the views, which only outside tools read: " + e.getMessage());
            return;
        }
        for (SqlViews.View view : views) {
            try {
                if (readable.contains(view.kind())) {
                    dropView(view);
                    execute("create " + view.name(), "CREATE VIEW " + view.name() + " AS " + view.select());
                }
            } catch (StorageException e) {
                logger.warning("Could not create the view " + view.name() + ", which only outside tools read: "
                        + e.getMessage());
            }
        }
    }

    private List<SqlViews.View> views() {
        return SqlViews.of(dialect, tablePrefix);
    }

    private void dropView(SqlViews.View view) {
        execute("drop " + view.name(), "DROP VIEW IF EXISTS " + view.name());
    }

    private void execute(String action, String sql) {
        run(action, connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            return null;
        });
    }

    /**
     * @param action What the work does, for the error, such as {@code "load buildsystem_worlds"}
     */
    synchronized <R> R run(String action, SqlWork<R> work) {
        try {
            return work.apply(connection());
        } catch (SQLException e) {
            // A driver message may quote the SQL, which holds only placeholders, never values, the URL or credentials.
            throw new StorageException(
                    "Could not " + action + ": " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Runs {@code work} in one transaction, so it either commits whole or not at all.
     */
    synchronized void transaction(String action, SqlWork<Void> work) {
        run(action, connection -> {
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
        if (closed) {
            // Work that was queued before the plugin disabled must not open a connection nobody closes.
            throw new SQLException("The database connection is closed");
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
        closed = true;
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
