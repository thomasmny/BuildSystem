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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs the migration tests against PostgreSQL 17 as the target. Needs Docker: run with {@code ./gradlew dockerTest}.
 */
@Tag("docker")
class PostgresStorageMigratorTest extends StorageMigratorTest {

    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17@sha256:2d2b8998d31037bf721cfdf764d76ba74171b4fab3431b7f72c27c56ddbdf9e3")
                    .asCompatibleSubstituteFor("postgres"));

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
    protected StorageType target() {
        return StorageType.POSTGRESQL;
    }

    @Override
    protected Map<StorageType, PluginConfig.Storage.Database.Server> servers() {
        return Map.of(
                StorageType.POSTGRESQL,
                new PluginConfig.Storage.Database.Server(
                        StorageType.POSTGRESQL,
                        DATABASE.getHost(),
                        DATABASE.getMappedPort(5432),
                        DATABASE.getDatabaseName(),
                        DATABASE.getUsername(),
                        DATABASE.getPassword()),
                StorageType.MYSQL,
                super.servers().get(StorageType.MYSQL));
    }
}
