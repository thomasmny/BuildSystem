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
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The backend chosen in {@code storage.database}, opened once when the plugin enables. Worlds, folders and players go
 * to it; everything else stays in YAML.
 */
@NullMarked
public final class StorageBackend implements AutoCloseable {

    private static final Pattern TABLE_PREFIX = Pattern.compile("[A-Za-z0-9_]*");

    private final Type type;
    private final @Nullable Database database;
    private final Logger logger;

    private StorageBackend(Type type, @Nullable Database database, Logger logger) {
        this.type = type;
        this.database = database;
        this.logger = logger;
    }

    /**
     * Opens the configured backend. There is no fallback to YAML: if the backend is unknown or cannot be reached, the
     * plugin must not enable on data that is not the configured one.
     *
     * @throws StorageException If the type or table prefix is invalid or the database cannot be reached
     */
    public static StorageBackend connect(PluginConfig.Storage.Database settings, File dataFolder, Logger logger) {
        Type type = Type.parse(settings.type());
        String prefix = settings.tablePrefix();
        if (!TABLE_PREFIX.matcher(prefix).matches()) {
            throw new StorageException(
                    "storage.database.table-prefix may only hold letters, digits and underscores: " + prefix, null);
        }
        File sqliteFile = new File(dataFolder, settings.database() + ".db");
        int port = settings.port() == 0 ? type.defaultPort : settings.port();
        String address = settings.host() + ":" + port;

        Database database =
                switch (type) {
                    case YAML -> {
                        if (sqliteFile.exists()) {
                            logger.warning(sqliteFile.getName() + " exists, but storage.database.type is yaml, so"
                                    + " worlds, folders and players load from the YAML files instead.");
                        }
                        yield null;
                    }
                    case SQLITE ->
                        SqlDatabase.connect(
                                SqlDialect.SQLITE,
                                "jdbc:sqlite:" + sqliteFile.getAbsolutePath(),
                                new Properties(),
                                prefix,
                                logger);
                    case MYSQL, MARIADB ->
                        SqlDatabase.connect(
                                SqlDialect.MYSQL,
                                "jdbc:mysql://" + address + "/" + settings.database(),
                                credentials(settings),
                                prefix,
                                logger);
                    case POSTGRESQL ->
                        SqlDatabase.connect(
                                SqlDialect.POSTGRESQL,
                                "jdbc:postgresql://" + address + "/" + settings.database(),
                                credentials(settings),
                                prefix,
                                logger);
                    case MONGODB ->
                        MongoConnection.connect(
                                settings.host(),
                                port,
                                settings.database(),
                                settings.username(),
                                settings.resolvedPassword(),
                                prefix,
                                logger);
                };
        if (database != null) {
            logger.info("Storing worlds, folders and players in " + type.displayName + ", database "
                    + (type == Type.SQLITE ? sqliteFile.getName() : settings.database()));
        }
        return new StorageBackend(type, database, logger);
    }

    private static Properties credentials(PluginConfig.Storage.Database settings) {
        Properties properties = new Properties();
        properties.setProperty("user", settings.username());
        String password = settings.resolvedPassword();
        if (password != null) {
            properties.setProperty("password", password);
        }
        return properties;
    }

    /**
     * {@return where the entries YAML keeps in {@code yaml} are kept} For a database, an empty collection is first
     * filled from the YAML file, read through the same path as a YAML load so a legacy file is migrated first. The file
     * is left as it is.
     */
    public EntityStore store(YamlEntityStore yaml) {
        if (database == null) {
            return yaml;
        }
        EntityStore store = database.collection(yaml.name());
        if (store.isEmpty()) {
            importEntries(yaml, store);
        }
        return store;
    }

    private void importEntries(YamlEntityStore yaml, EntityStore store) {
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
                + type.displayName + ". " + yaml.fileName() + " is no longer updated.");
    }

    @Override
    public void close() {
        if (database != null) {
            database.close();
        }
    }

    enum Type {
        YAML("YAML", 0),
        SQLITE("SQLite", 0),
        MYSQL("MySQL", 3306),
        MARIADB("MariaDB", 3306),
        POSTGRESQL("PostgreSQL", 5432),
        MONGODB("MongoDB", 27017);

        private final String displayName;
        private final int defaultPort;

        Type(String displayName, int defaultPort) {
            this.displayName = displayName;
            this.defaultPort = defaultPort;
        }

        static Type parse(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                String known = Arrays.stream(values())
                        .map(type -> type.name().toLowerCase(Locale.ROOT))
                        .collect(Collectors.joining(", "));
                throw new StorageException("Unknown storage.database.type \"" + name + "\". Use one of: " + known, e);
            }
        }
    }
}
