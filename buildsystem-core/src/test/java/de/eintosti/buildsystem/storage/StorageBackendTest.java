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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.eintosti.buildsystem.config.PluginConfig;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class StorageBackendTest {

    @Test
    void sqlConnections_giveUpOnAReadAfterThirtySeconds() {
        Properties mysql = StorageBackend.properties(server(StorageType.MYSQL));
        Properties mariadb = StorageBackend.properties(server(StorageType.MARIADB));
        Properties postgres = StorageBackend.properties(server(StorageType.POSTGRESQL));

        assertEquals("30000", mysql.getProperty("socketTimeout"), "milliseconds");
        assertEquals("30000", mariadb.getProperty("socketTimeout"), "milliseconds");
        assertEquals("30", postgres.getProperty("socketTimeout"), "seconds");
    }

    private static PluginConfig.Storage.Database.Server server(StorageType type) {
        return new PluginConfig.Storage.Database.Server(
                type, "localhost", type.defaultPort(), "buildsystem", "builder", "secret", StorageTls.PREFER);
    }
}
