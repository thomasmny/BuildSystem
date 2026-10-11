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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.EntityKind;
import de.eintosti.buildsystem.storage.StorageBackend;
import de.eintosti.buildsystem.storage.StorageTls;
import de.eintosti.buildsystem.storage.StorageType;
import java.io.File;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Connects to PostgreSQL servers with TLS on. One has a certificate from the test CA, which dockerTest puts in the JVM
 * trust store; the other a self-signed one. Needs Docker: run with {@code ./gradlew dockerTest}.
 */
@NullMarked
@Tag("docker")
class PostgresTlsTest {

    private static final Logger LOGGER = Logger.getLogger(PostgresTlsTest.class.getName());
    private static final PostgreSQLContainer TRUSTED = withTls("server");
    private static final PostgreSQLContainer SELF_SIGNED = withTls("self-signed");

    @TempDir
    File dataFolder;

    @TempDir
    File emptyHome;

    private String userHome = "";

    /**
     * The key must belong to the postgres user, which a copied file does not, so the entrypoint copies it once more.
     */
    private static PostgreSQLContainer withTls(String certificate) {
        PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse(
                        "postgres:17@sha256:2d2b8998d31037bf721cfdf764d76ba74171b4fab3431b7f72c27c56ddbdf9e3")
                .asCompatibleSubstituteFor("postgres"));
        return container
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("tls/" + certificate + ".crt"), "/tls/server.crt")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("tls/" + certificate + ".key"), "/tls/server.key")
                .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                        "sh",
                        "-c",
                        "mkdir -p /var/lib/pgtls && cp /tls/* /var/lib/pgtls/ && chown postgres /var/lib/pgtls/*"
                                + " && chmod 600 /var/lib/pgtls/server.key && exec docker-entrypoint.sh postgres"
                                + " -c ssl=on -c ssl_cert_file=/var/lib/pgtls/server.crt"
                                + " -c ssl_key_file=/var/lib/pgtls/server.key"));
    }

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    static void start() {
        TRUSTED.start();
        SELF_SIGNED.start();
    }

    @AfterAll
    static void stop() {
        TRUSTED.stop();
        SELF_SIGNED.stop();
    }

    @BeforeEach
    void hideRootCertificate() {
        // pgjdbc's own factory reads ~/.postgresql/root.crt, which verify must no longer need.
        userHome = System.getProperty("user.home");
        System.setProperty("user.home", emptyHome.getAbsolutePath());
    }

    @AfterEach
    void restoreHome() {
        System.setProperty("user.home", userHome);
    }

    @Test
    void verify_withACertificateTheJvmTrusts_connectsOverTls() {
        assertEquals("true", ssl(TRUSTED, StorageTls.VERIFY));
    }

    @Test
    void require_withASelfSignedCertificate_stillConnectsOverTls() {
        assertEquals("true", ssl(SELF_SIGNED, StorageTls.REQUIRE));
    }

    @Test
    void prefer_withASelfSignedCertificate_stillConnectsOverTls() {
        assertEquals("true", ssl(SELF_SIGNED, StorageTls.PREFER));
    }

    /**
     * {@return whether the connection the plugin opens is encrypted, as the server sees it, or null without one}
     */
    private String ssl(PostgreSQLContainer container, StorageTls tls) {
        PluginConfig.Storage.Database settings = new PluginConfig.Storage.Database(
                StorageType.POSTGRESQL,
                "tls_",
                Map.of(
                        StorageType.POSTGRESQL,
                        new PluginConfig.Storage.Database.Server(
                                StorageType.POSTGRESQL,
                                container.getHost(),
                                container.getMappedPort(5432),
                                container.getDatabaseName(),
                                container.getUsername(),
                                container.getPassword(),
                                tls)));
        try (StorageBackend backend = StorageBackend.connect(StorageType.POSTGRESQL, settings, dataFolder, LOGGER)) {
            backend.collection(EntityKind.PLAYERS).upsert(Map.of("probe", Map.of("n", "1")));
            SqlDatabase raw =
                    SqlEntityStoreContractTest.connect(SqlDialect.POSTGRESQL, "postgresql", container, 5432, "tls_");
            try {
                return raw.run("check", connection -> {
                    try (Statement statement = connection.createStatement();
                            ResultSet rows = statement.executeQuery("SELECT bool_and(s.ssl)::text FROM pg_stat_ssl s"
                                    + " JOIN pg_stat_activity a USING (pid) WHERE a.pid <> pg_backend_pid()"
                                    + " AND a.datname = current_database() AND a.backend_type = 'client backend'")) {
                        rows.next();
                        return rows.getString(1);
                    }
                });
            } finally {
                raw.close();
            }
        }
    }
}
