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
    void migrate_disabledPhysics_becomesInvertedPhysicsExceptions() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("world.disabled-physics.prevent-connections", true);
        config.set("world.disabled-physics.prevent-fluid-flow", false);
        config.set("world.disabled-physics.prevent-falling-blocks", true);

        new MigrationV3ToV4().migrate(config);

        assertFalse(config.getBoolean("world.defaults.physics-exceptions.connections"));
        assertTrue(config.getBoolean("world.defaults.physics-exceptions.fluid-flow"));
        assertFalse(config.getBoolean("world.defaults.physics-exceptions.falling-blocks"));
        assertNull(config.get("world.disabled-physics"));
    }

    @Test
    void migrate_missingSection_writesNothing() {
        YamlConfiguration config = new YamlConfiguration();

        new MigrationV3ToV4().migrate(config);

        assertNull(config.get("world.defaults.physics-exceptions"));
    }
}
