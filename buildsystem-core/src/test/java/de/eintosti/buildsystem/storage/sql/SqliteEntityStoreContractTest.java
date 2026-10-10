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

import java.io.File;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class SqliteEntityStoreContractTest extends SqlEntityStoreContractTest {

    @TempDir
    File dataFolder;

    private SqlDatabase database;

    @BeforeEach
    void connect() {
        database = SqlDatabase.connect(
                SqlDialect.SQLITE,
                "jdbc:sqlite:" + new File(dataFolder, "buildsystem.db").getAbsolutePath(),
                new Properties(),
                PREFIX,
                LOGGER);
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Override
    protected SqlDatabase database() {
        return database;
    }
}
