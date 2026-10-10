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

import de.eintosti.buildsystem.config.PluginConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs the contract against MySQL 8.4. Needs Docker: run with {@code ./gradlew dockerTest}.
 */
@Tag("docker")
class MySqlEntityStoreContractTest extends SqlEntityStoreContractTest {

    private static final MySQLContainer DATABASE = new MySQLContainer(
            DockerImageName.parse("mysql:8.4@sha256:6ea90827b1100f8f2ae306a539f86d2c264a26ed435a2a9f75551dd5c3aeb242")
                    .asCompatibleSubstituteFor("mysql"));

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
        return settings("mysql", DATABASE, 3306);
    }

    @Override
    protected SqlDatabase rawDatabase() {
        return connect(SqlDialect.MYSQL, "mysql", DATABASE, 3306, tablePrefix);
    }

    @Override
    protected void dropOtherConnections(Connection connection) throws SQLException {
        List<Long> others = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT id FROM information_schema.processlist"
                        + " WHERE id <> CONNECTION_ID() AND db = DATABASE()")) {
            while (rows.next()) {
                others.add(rows.getLong(1));
            }
        }
        try (Statement statement = connection.createStatement()) {
            for (long id : others) {
                statement.execute("KILL " + id);
            }
        }
    }
}
