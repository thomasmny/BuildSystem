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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MigrationV5ToV6Test {

    private static final String RULES = "world.defaults.gamerules.";

    @Test
    void migrate_oldNameOnly_movesToNewName() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(RULES + "doMobSpawning", true);

        new MigrationV5ToV6().migrate(config);

        assertEquals(true, config.get(RULES + "spawn_mobs"));
        assertNull(config.get(RULES + "doMobSpawning"));
    }

    @Test
    void migrate_newNameStillShipped_takesOldValue() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(RULES + "doDaylightCycle", true);
        config.set(RULES + "advance_time", false);

        new MigrationV5ToV6().migrate(config);

        assertEquals(true, config.get(RULES + "advance_time"));
        assertNull(config.get(RULES + "doDaylightCycle"));
    }

    @Test
    void migrate_newNameChangedByAdmin_wins() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(RULES + "doDaylightCycle", false);
        config.set(RULES + "advance_time", true);

        new MigrationV5ToV6().migrate(config);

        assertEquals(true, config.get(RULES + "advance_time"));
        assertNull(config.get(RULES + "doDaylightCycle"));
    }

    @Test
    void migrate_otherOldRules_untouched() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(RULES + "doFireTick", false);
        config.set(RULES + "keepInventory", true);

        new MigrationV5ToV6().migrate(config);

        assertEquals(false, config.get(RULES + "doFireTick"));
        assertEquals(true, config.get(RULES + "keepInventory"));
    }
}
