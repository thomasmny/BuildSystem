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

import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Migrates config from v3 to v4: {@code world.disabled-physics.prevent-*} becomes
 * {@code world.defaults.physics-exceptions.*} with inverted sense — {@code prevent-x: true} meant the behavior was
 * blocked while physics were disabled, {@code physics-exceptions.x: true} means it still runs. The values are now
 * per-world defaults seeding the physics-exceptions world data, not live global switches.
 */
@NullMarked
public class MigrationV3ToV4 implements Migration {

    @Override
    public void migrate(FileConfiguration config) {
        invertIfPresent(
                config, "world.disabled-physics.prevent-connections", "world.defaults.physics-exceptions.connections");
        invertIfPresent(
                config, "world.disabled-physics.prevent-fluid-flow", "world.defaults.physics-exceptions.fluid-flow");
        invertIfPresent(
                config,
                "world.disabled-physics.prevent-falling-blocks",
                "world.defaults.physics-exceptions.falling-blocks");
        config.set("world.disabled-physics", null);
    }

    private static void invertIfPresent(FileConfiguration config, String from, String to) {
        if (config.contains(from)) {
            config.set(to, !config.getBoolean(from, true));
        }
    }
}
