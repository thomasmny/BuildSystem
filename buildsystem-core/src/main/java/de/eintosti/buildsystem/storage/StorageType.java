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

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;

/**
 * Where worlds, folders and players can be kept, named in {@code storage.type}.
 */
@NullMarked
public enum StorageType {
    YAML("YAML", 0),
    SQLITE("SQLite", 0),
    MYSQL("MySQL", 3306),
    MARIADB("MariaDB", 3306),
    POSTGRESQL("PostgreSQL", 5432),
    MONGODB("MongoDB", 27017);

    private final String displayName;
    private final int defaultPort;

    StorageType(String displayName, int defaultPort) {
        this.displayName = displayName;
        this.defaultPort = defaultPort;
    }

    /**
     * {@return the type named in the config} Case is ignored.
     *
     * @throws IllegalArgumentException If no type has that name, listing the ones that do
     */
    public static StorageType parse(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String known = Arrays.stream(values()).map(StorageType::key).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Unknown storage.type \"" + name + "\". Use one of: " + known, e);
        }
    }

    /**
     * {@return the name used in the config, which is also the name of the type's connection block}
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    /**
     * {@return whether the type connects to a server, configured in {@code storage.<key>}}
     */
    public boolean hasServer() {
        return defaultPort != 0;
    }

    public int defaultPort() {
        return defaultPort;
    }
}
