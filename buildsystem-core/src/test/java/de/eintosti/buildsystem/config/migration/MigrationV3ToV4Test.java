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
package de.eintosti.buildsystem.config.migration;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MigrationV3ToV4Test {

    @Test
    void migrate_perOptionPermissions_addedWithDefaultFalse() {
        YamlConfiguration config = new YamlConfiguration();

        new MigrationV3ToV4().migrate(config);

        assertTrue(config.contains("settings.per-option-permissions"));
        assertFalse(config.getBoolean("settings.per-option-permissions"));
    }

    @Test
    void migrate_existingKeys_preserved() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.scoreboard", false);
        config.set("settings.date-format", "yyyy-MM-dd");

        new MigrationV3ToV4().migrate(config);

        assertFalse(config.getBoolean("settings.scoreboard"));
        assertEquals("yyyy-MM-dd", config.getString("settings.date-format"));
        assertFalse(config.getBoolean("settings.per-option-permissions"));
    }

    @Test
    void migrate_existingPerOptionPermissions_notOverwritten() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.per-option-permissions", true);

        new MigrationV3ToV4().migrate(config);

        assertTrue(config.getBoolean("settings.per-option-permissions"));
    }

    @Test
    void migrate_missingKeys_noException() {
        YamlConfiguration config = new YamlConfiguration();
        assertDoesNotThrow(() -> new MigrationV3ToV4().migrate(config));
    }

    @Test
    void migrate_idempotent_secondRunNoOp() {
        YamlConfiguration config = new YamlConfiguration();

        MigrationV3ToV4 migration = new MigrationV3ToV4();
        migration.migrate(config);
        migration.migrate(config);

        assertFalse(config.getBoolean("settings.per-option-permissions"));
    }
}
