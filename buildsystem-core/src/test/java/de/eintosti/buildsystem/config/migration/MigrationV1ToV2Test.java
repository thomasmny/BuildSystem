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

class MigrationV1ToV2Test {

    @Test
    void migrate_archiveSettings_movedToNestedPath() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.archive-vanish", true);
        config.set("settings.archive-should-change-gamemode", false);
        config.set("settings.archive-world-game-mode", "ADVENTURE");

        new MigrationV1ToV2().migrate(config);

        assertTrue(config.getBoolean("settings.archive.vanish"));
        assertFalse(config.getBoolean("settings.archive.change-gamemode"));
        assertEquals("ADVENTURE", config.getString("settings.archive.world-gamemode"));
    }

    @Test
    void migrate_archiveSettings_oldKeysDeleted() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.archive-vanish", true);
        config.set("settings.archive-should-change-gamemode", false);
        config.set("settings.archive-world-game-mode", "CREATIVE");

        new MigrationV1ToV2().migrate(config);

        assertNull(config.get("settings.archive-vanish"));
        assertNull(config.get("settings.archive-should-change-gamemode"));
        assertNull(config.get("settings.archive-world-game-mode"));
    }

    @Test
    void migrate_deadKeys_deleted() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.teleport-after-creation", true);
        config.set("world.void-block", "STONE");

        new MigrationV1ToV2().migrate(config);

        assertNull(config.get("settings.teleport-after-creation"));
        assertNull(config.get("world.void-block"));
    }

    @Test
    void migrate_missingKeys_noException() {
        YamlConfiguration config = new YamlConfiguration();
        // Empty config — migration should not crash
        assertDoesNotThrow(() -> new MigrationV1ToV2().migrate(config));
    }

    @Test
    void migrate_idempotent_secondRunNoOp() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.archive-vanish", true);

        MigrationV1ToV2 migration = new MigrationV1ToV2();
        migration.migrate(config);
        migration.migrate(config);

        assertTrue(config.getBoolean("settings.archive.vanish"));
        assertNull(config.get("settings.archive-vanish"));
    }
}
