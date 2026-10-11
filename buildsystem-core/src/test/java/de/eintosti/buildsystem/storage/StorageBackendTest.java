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

import com.mysql.cj.conf.ConnectionUrl;
import de.eintosti.buildsystem.config.PluginConfig;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.postgresql.Driver;
import org.postgresql.ssl.DefaultJavaSSLFactory;

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

    @Test
    void hostWithAnUnderscore_reachesTheDriversAsThatHost() throws Exception {
        Properties postgres =
                Driver.parseURL(StorageBackend.url("postgresql", server(StorageType.POSTGRESQL, "postgres_db")), null);
        ConnectionUrl mysql = ConnectionUrl.getConnectionUrlInstance(
                StorageBackend.url("mysql", server(StorageType.MYSQL, "mysql_db")), new Properties());

        assertEquals("postgres_db", postgres.getProperty("PGHOST"));
        assertEquals("5432", postgres.getProperty("PGPORT"));
        assertEquals("buildsystem", postgres.getProperty("PGDBNAME"));
        assertEquals(1, mysql.getHostsList().size());
        assertEquals("mysql_db", mysql.getMainHost().getHost());
        assertEquals(3306, mysql.getMainHost().getPort());
        assertEquals("buildsystem", mysql.getDatabase());
    }

    @Test
    void postgresVerify_trustsTheJvmTrustStore_andTheOtherModesKeepTheDefault() {
        for (StorageTls tls : StorageTls.values()) {
            Properties postgres = StorageBackend.properties(new PluginConfig.Storage.Database.Server(
                    StorageType.POSTGRESQL, "localhost", 5432, "buildsystem", "builder", "secret", tls));

            assertEquals(
                    tls == StorageTls.VERIFY ? DefaultJavaSSLFactory.class.getName() : null,
                    postgres.getProperty("sslfactory"),
                    tls.name());
        }
    }

    private static PluginConfig.Storage.Database.Server server(StorageType type) {
        return server(type, "localhost");
    }

    private static PluginConfig.Storage.Database.Server server(StorageType type, String host) {
        return new PluginConfig.Storage.Database.Server(
                type, host, type.defaultPort(), "buildsystem", "builder", "secret", StorageTls.PREFER);
    }
}
