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

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MigrationV2ToV3Test {

    @Test
    void migrate_messagesSection_movedToSettings() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("messages.spawn-teleport-message", true);
        config.set("messages.join-quit-messages", false);
        config.set("messages.date-format", "dd/MM/yyyy");

        new MigrationV2ToV3().migrate(config);

        assertTrue(config.getBoolean("settings.spawn-teleport-message"));
        assertFalse(config.getBoolean("settings.join-quit-messages"));
        assertEquals("dd/MM/yyyy", config.getString("settings.date-format"));
        assertNull(config.get("messages"));
    }

    @Test
    void migrate_disabledPhysics_movedToWorldSection() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.disabled-physics.prevent-connections", true);
        config.set("settings.disabled-physics.prevent-fluid-flow", false);
        config.set("settings.disabled-physics.prevent-falling-blocks", true);

        new MigrationV2ToV3().migrate(config);

        assertTrue(config.getBoolean("world.disabled-physics.prevent-connections"));
        assertFalse(config.getBoolean("world.disabled-physics.prevent-fluid-flow"));
        assertTrue(config.getBoolean("world.disabled-physics.prevent-falling-blocks"));
        assertNull(config.get("settings.disabled-physics"));
    }

    @Test
    void migrate_worldLimits_movedFromDefaultSettings() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.default.settings.public-worlds", 10);
        config.set("world.default.settings.private-worlds", 5);

        new MigrationV2ToV3().migrate(config);

        assertEquals(10, config.getInt("world.limits.public"));
        assertEquals(5, config.getInt("world.limits.private"));
    }

    @Test
    void migrate_missingKeys_noException() {
        YamlConfiguration config = new YamlConfiguration();
        assertDoesNotThrow(() -> new MigrationV2ToV3().migrate(config));
    }

    @Test
    void migrate_idempotent_secondRunNoOp() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("messages.date-format", "yyyy-MM-dd");

        MigrationV2ToV3 migration = new MigrationV2ToV3();
        migration.migrate(config);
        migration.migrate(config);

        assertEquals("yyyy-MM-dd", config.getString("settings.date-format"));
        assertNull(config.get("messages"));
    }
}
