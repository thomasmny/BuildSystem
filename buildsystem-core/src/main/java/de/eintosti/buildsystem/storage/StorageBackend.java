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
    private final @Nullable Database database;
    private final File dataFolder;
    private final Logger logger;

    private StorageBackend(StorageType type, @Nullable Database database, File dataFolder, Logger logger) {
        this.type = type;
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
                    case YAML -> {
                        if (sqliteFile.exists()) {
                            logger.warning(SQLITE_FILE + " exists, but storage.type is yaml, so worlds, folders and"
                                    + " players load from the YAML files instead.");
                        }
                        yield null;
                    }
                    case SQLITE -> sqlite(sqliteFile, prefix, logger);
                    case MYSQL, MARIADB ->
                        SqlDatabase.connect(
                                SqlDialect.MYSQL,
                                url("mysql", settings.server(type)),
                                credentials(settings.server(type)),
                                prefix,
                                logger);
                    case POSTGRESQL -> {
                        Properties properties = credentials(settings.server(type));
                        // Error details can quote a failing row, which has no place in the log.
                        properties.setProperty("logServerErrorDetail", "false");
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
                                prefix,
                                logger);
                    }
                };
        if (database != null) {
            logger.info("Storing worlds, folders and players in " + type.displayName() + ", database "
                    + (type == StorageType.SQLITE
                            ? SQLITE_FILE
                            : settings.server(type).database()));
        }
        return new StorageBackend(type, database, dataFolder, logger);
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
                                    + " does not allow running native code. Start the server with"
                                    + " -Dorg.sqlite.tmpdir=<a folder that does>, or set storage.type to another"
                                    + " backend.",
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
     * {@return where entries of {@code kind} are kept} For a database, an empty collection is first filled from the
     * YAML file, read through the same path as a YAML load so a legacy file is migrated first. The file is left as it
     * is.
     */
    public EntityStore store(EntityKind kind) {
        YamlEntityStore yaml = kind.yaml(dataFolder, logger);
        if (database == null) {
            return yaml;
        }
        EntityStore store = database.collection(kind);
        if (store.isEmpty()) {
            importEntries(yaml, store);
        }
        return store;
    }

    private void importEntries(YamlEntityStore yaml, EntityStore store) {
        if (!yaml.exists()) {
            // Loading would create an empty file.
            return;
        }
        Map<String, Map<String, Object>> entries = yaml.loadAll();
        if (entries.isEmpty()) {
            return;
        }
        try {
            store.upsert(entries);
        } catch (RuntimeException e) {
            // MongoDB keeps what it wrote before the failure, and a non-empty collection is never imported again.
            try {
                entries.keySet().forEach(store::delete);
            } catch (RuntimeException cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
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
