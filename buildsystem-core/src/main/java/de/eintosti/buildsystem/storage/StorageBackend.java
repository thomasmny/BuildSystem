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

import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.mongo.MongoConnection;
import de.eintosti.buildsystem.storage.sql.SqlDatabase;
import de.eintosti.buildsystem.storage.sql.SqlDialect;
import de.eintosti.buildsystem.storage.yaml.YamlEntityStore;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The backend chosen in {@code storage.type}, opened once when the plugin enables. Worlds, folders and players go to
 * it; everything else stays in YAML.
 */
@NullMarked
public final class StorageBackend implements AutoCloseable {

    /**
     * The SQLite file in the plugin folder. Fixed, so the config cannot point the plugin at any other file.
     */
    public static final String SQLITE_FILE = "storage.db";

    private final StorageType type;
    private final String location;
    private final @Nullable Database database;
    private final File dataFolder;
    private final Logger logger;

    private StorageBackend(
            StorageType type, String location, @Nullable Database database, File dataFolder, Logger logger) {
        this.type = type;
        this.location = location;
        this.database = database;
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    /**
     * Opens {@code type}. There is no fallback to YAML: if the backend cannot be reached, the plugin must not enable on
     * data that is not the configured one.
     *
     * @param settings The checked settings, of which only the table prefix and the block of {@code type} are used
     * @throws StorageException If the database cannot be reached
     */
    public static StorageBackend connect(
            StorageType type, PluginConfig.Storage.Database settings, File dataFolder, Logger logger) {
        String prefix = settings.tablePrefix();
        File sqliteFile = new File(dataFolder, SQLITE_FILE);
        Database database =
                switch (type) {
                    case YAML -> null;
                    case SQLITE -> sqlite(sqliteFile, prefix, logger);
                    case MYSQL, MARIADB -> {
                        Properties properties = credentials(settings.server(type));
                        // The driver waits forever by default, PostgreSQL and MongoDB give up after 10 seconds.
                        properties.setProperty("connectTimeout", "10000");
                        properties.setProperty(
                                "sslMode", settings.server(type).tls().mysqlSslMode());
                        yield SqlDatabase.connect(
                                SqlDialect.MYSQL, url("mysql", settings.server(type)), properties, prefix, logger);
                    }
                    case POSTGRESQL -> {
                        Properties properties = credentials(settings.server(type));
                        // Error details can quote a failing row, which has no place in the log.
                        properties.setProperty("logServerErrorDetail", "false");
                        properties.setProperty(
                                "sslmode", settings.server(type).tls().postgresSslMode());
                        yield SqlDatabase.connect(
                                SqlDialect.POSTGRESQL,
                                url("postgresql", settings.server(type)),
                                properties,
                                prefix,
                                logger);
                    }
                    case MONGODB -> {
                        PluginConfig.Storage.Database.Server server = settings.server(type);
                        yield MongoConnection.connect(
                                server.host(),
                                server.port(),
                                server.database(),
                                server.username(),
                                server.resolvedPassword(),
                                server.tls(),
                                prefix,
                                logger);
                    }
                };
        if (database != null) {
            logger.info("Connected to " + type.displayName() + ", database "
                    + (type == StorageType.SQLITE
                            ? SQLITE_FILE
                            : settings.server(type).database()));
        }
        return new StorageBackend(type, location(type, settings), database, dataFolder, logger);
    }

    /**
     * {@return where {@code type} keeps its entries with these settings} Two types with the same location write to the
     * same tables, as MySQL and MariaDB do with their default blocks.
     *
     * @throws IllegalArgumentException If the block of {@code type} is invalid
     */
    public static String location(StorageType type, PluginConfig.Storage.Database settings) {
        return switch (type) {
            case YAML -> "yaml";
            case SQLITE -> SQLITE_FILE + "/" + settings.tablePrefix();
            case MYSQL, MARIADB, POSTGRESQL, MONGODB -> {
                PluginConfig.Storage.Database.Server server = settings.server(type);
                yield server.host().toLowerCase(Locale.ROOT) + ":" + server.port() + "/" + server.database() + "/"
                        + settings.tablePrefix();
            }
        };
    }

    private static SqlDatabase sqlite(File file, String prefix, Logger logger) {
        try {
            return SqlDatabase.connect(
                    SqlDialect.SQLITE, "jdbc:sqlite:" + file.getAbsolutePath(), new Properties(), prefix, logger);
        } catch (StorageException | LinkageError e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof LinkageError
                        || cause.getClass().getSimpleName().equals("NativeLibraryNotFoundException")) {
                    throw new StorageException(
                            "SQLite could not load its native library: " + cause.getMessage()
                                    + ". It is unpacked into the temp folder first, so this usually means that folder"
                                    + " does not allow running native code. Either set storage.type to yaml in"
                                    + " config.yml, or start the server with -Djava.io.tmpdir=<folder> or"
                                    + " -Dorg.sqlite.tmpdir=<folder>, pointing at a folder that allows it.",
                            e);
                }
            }
            throw e;
        }
    }

    private static String url(String scheme, PluginConfig.Storage.Database.Server server) {
        return "jdbc:" + scheme + "://" + server.host() + ":" + server.port() + "/" + server.database();
    }

    private static Properties credentials(PluginConfig.Storage.Database.Server server) {
        if (server.username().isBlank()) {
            throw new StorageException("storage." + server.type().key() + ".username is not set", null);
        }
        Properties properties = new Properties();
        properties.setProperty("user", server.username());
        String password = server.resolvedPassword();
        if (password != null) {
            properties.setProperty("password", password);
        }
        return properties;
    }

    public StorageType type() {
        return type;
    }

    /**
     * {@return where the entries are kept, as {@link #location(StorageType, PluginConfig.Storage.Database)} put it when
     * this connected}
     */
    public String location() {
        return location;
    }

    /**
     * {@return the store for {@code kind}, as it is} Unlike {@link #store}, nothing is imported into it.
     */
    public EntityStore collection(EntityKind kind) {
        return database == null ? kind.yaml(dataFolder, logger) : database.collection(kind);
    }

    /**
     * {@return whether the table or collection for {@code kind} exists} A YAML file always counts as existing, so it is
     * never {@link #drop dropped}.
     */
    public boolean exists(EntityKind kind) {
        return database == null || database.exists(kind);
    }

    /**
     * Removes the table or collection for {@code kind} with everything in it. Does nothing for YAML, whose files are
     * never deleted.
     */
    public void drop(EntityKind kind) {
        if (database != null) {
            database.drop(kind);
        }
    }

    /**
     * {@return whether entries of {@code kind} are stored here} Creates nothing, so that a later start still imports
     * the YAML file into a table or collection that does not exist yet.
     */
    public boolean holdsEntries(EntityKind kind) {
        if (database == null) {
            YamlEntityStore yaml = kind.yaml(dataFolder, logger);
            // Reading a YAML file that is not there would create it.
            return yaml.exists() && !yaml.isEmpty();
        }
        // Once it exists, opening it creates nothing.
        return database.exists(kind) && !database.collection(kind).isEmpty();
    }

    /**
     * {@return where entries of {@code kind} are kept} For a database, a table or collection created by this call is
     * first filled from the YAML file, read through the same path as a YAML load so a legacy file is migrated first. The file is left as it
     * is.
     */
    public EntityStore store(EntityKind kind) {
        YamlEntityStore yaml = kind.yaml(dataFolder, logger);
        Database database = this.database;
        if (database == null) {
            return yaml;
        }
        // Only a new table: an empty one may be empty because everything in it was deleted, and the old file would
        // bring
        // it all back.
        boolean existed = database.exists(kind);
        EntityStore store = collection(kind);
        if (!existed) {
            importEntries(database, kind, yaml, store);
        }
        return store;
    }

    private void importEntries(Database database, EntityKind kind, YamlEntityStore yaml, EntityStore store) {
        if (!yaml.exists()) {
            // Loading would create an empty file.
            return;
        }
        Map<String, Map<String, Object>> entries;
        try {
            entries = yaml.loadAll();
            store.upsert(entries);
        } catch (RuntimeException e) {
            // Only a new table is imported into, so it goes again for the next start to retry. MongoDB would also keep
            // what it wrote before the failure.
            try {
                database.drop(kind);
            } catch (RuntimeException cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
        }
        if (entries.isEmpty()) {
            return;
        }
        logger.info("Imported " + entries.size() + " " + yaml.name() + " from " + yaml.fileName() + " into "
                + type.displayName() + ". " + yaml.fileName() + " is no longer updated.");
    }

    @Override
    public void close() {
        if (database != null) {
            database.close();
        }
    }
}
