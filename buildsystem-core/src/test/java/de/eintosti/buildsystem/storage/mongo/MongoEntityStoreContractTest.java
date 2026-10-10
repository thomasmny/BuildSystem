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
package de.eintosti.buildsystem.storage.mongo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.EntityStoreContractTest;
import de.eintosti.buildsystem.storage.StorageType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs the contract against MongoDB 8.2 with a root user, so the login goes through the admin database as it does on a
 * typical server. Needs Docker: run with {@code ./gradlew dockerTest}.
 */
@Tag("docker")
class MongoEntityStoreContractTest extends EntityStoreContractTest {

    private static final String USER = "buildsystem";
    private static final String PASSWORD = "buildsystem-secret";

    // 8.2, because the current 8.0 and latest images refuse to start on Linux 6.19 or newer (SERVER-121912).
    // The entrypoint starts mongod once to create the user, then again for real.
    private static final GenericContainer<?> DATABASE = new GenericContainer<>(DockerImageName.parse(
                    "mongo:8.2@sha256:e0ce8c35124d4a9f9785532d1f268f39e9728ffa1cb38f46fa482436424c4bd3"))
            .withEnv("MONGO_INITDB_ROOT_USERNAME", USER)
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", PASSWORD)
            .withExposedPorts(27017)
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 2));

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    static void start() {
        DATABASE.start();
    }

    @AfterAll
    static void stop() {
        DATABASE.stop();
    }

    @Override
    protected PluginConfig.Storage.Database settings() {
        return settings(
                StorageType.MONGODB,
                DATABASE.getHost(),
                DATABASE.getMappedPort(27017),
                "buildsystem",
                USER,
                PASSWORD,
                tablePrefix);
    }

    @Override
    protected List<String> loadOrder(List<String> saved) {
        return saved.stream().sorted().toList();
    }

    @Test
    void fieldNameMongoWouldReadAsOperatorOrPath_isRejected() {
        for (String field : List.of("$set", "a.b")) {
            Map<String, Map<String, Object>> entry = Map.of("p", Map.of("settings", Map.of(field, true)));

            assertThrows(IllegalArgumentException.class, () -> store("players").upsert(entry), field);
        }
        assertEquals(Map.of(), store("players").loadAll());
    }
}
