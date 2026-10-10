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
 * Whether the connection to a database server is encrypted, named in {@code storage.<type>.tls}. Only these four, so
 * the config never passes driver options of its own.
 */
@NullMarked
public enum StorageTls {
    PREFER("PREFERRED", "prefer"),
    OFF("DISABLED", "disable"),
    REQUIRE("REQUIRED", "require"),
    VERIFY("VERIFY_IDENTITY", "verify-full");

    private final String mysqlSslMode;
    private final String postgresSslMode;

    StorageTls(String mysqlSslMode, String postgresSslMode) {
        this.mysqlSslMode = mysqlSslMode;
        this.postgresSslMode = postgresSslMode;
    }

    /**
     * {@return the mode named at {@code path}} Case is ignored.
     *
     * @throws IllegalArgumentException If no mode has that name, listing the ones that do
     */
    public static StorageTls parse(String path, String name) {
        // YAML reads an unquoted off as false.
        if (name.equals("false")) {
            return OFF;
        }
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String known = Arrays.stream(values())
                    .map(tls -> tls.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(path + " must be one of: " + known, e);
        }
    }

    /**
     * {@return the value of the MySQL Connector/J {@code sslMode} property}
     */
    public String mysqlSslMode() {
        return mysqlSslMode;
    }

    /**
     * {@return the value of the PostgreSQL driver's {@code sslmode} property}
     */
    public String postgresSslMode() {
        return postgresSslMode;
    }
}
