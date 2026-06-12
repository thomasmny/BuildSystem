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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Migrates config from v2 to v3: restructures the layout for logical grouping.
 * <ul>
 *   <li>{@code messages.*} → {@code settings.*} (display prefs are settings, not messages)</li>
 *   <li>{@code settings.disabled-physics.*} → {@code world.disabled-physics.*} (physics is world behavior)</li>
 *   <li>{@code world.default.*} → {@code world.defaults.*} (plural)</li>
 *   <li>{@code world.default.settings.*} → {@code world.defaults.*} (flatten sub-nesting)</li>
 *   <li>{@code world.default.settings.builders-enabled.*} → {@code world.defaults.builders-enabled.*}</li>
 *   <li>{@code world.default.settings.public-worlds/private-worlds} → {@code world.limits.public/private}</li>
 *   <li>{@code world.default.worldborder.size} → {@code world.defaults.worldborder-size}</li>
 *   <li>{@code world.import-all.delay} → {@code world.import-all-delay}</li>
 *   <li>Dead: {@code world.disabled-physics.*} (old, never read) deleted</li>
 *   <li>Dead: {@code world.max-amount.*} (wrong path) deleted</li>
 * </ul>
 */
@NullMarked
public class MigrationV2ToV3 implements Migration {

    @Override
    public void migrate(FileConfiguration config) {
        // 1. messages.* → settings.*
        moveIfPresent(config, "messages.spawn-teleport-message", "settings.spawn-teleport-message");
        moveIfPresent(config, "messages.join-quit-messages", "settings.join-quit-messages");
        moveIfPresent(config, "messages.date-format", "settings.date-format");
        config.set("messages", null);

        // 2. settings.disabled-physics → world.disabled-physics
        moveIfPresent(config, "settings.disabled-physics.prevent-connections", "world.disabled-physics.prevent-connections");
        moveIfPresent(config, "settings.disabled-physics.prevent-fluid-flow", "world.disabled-physics.prevent-fluid-flow");
        moveIfPresent(config, "settings.disabled-physics.prevent-falling-blocks", "world.disabled-physics.prevent-falling-blocks");
        config.set("settings.disabled-physics", null);

        // 3. world.default.settings.public-worlds/private-worlds → world.limits.*
        moveIfPresent(config, "world.default.settings.public-worlds", "world.limits.public");
        moveIfPresent(config, "world.default.settings.private-worlds", "world.limits.private");
        // Also migrate from the wrong path that operators may have used
        if (config.contains("world.max-amount.public")) {
            config.set("world.limits.public", config.getInt("world.max-amount.public", -1));
        }
        if (config.contains("world.max-amount.private")) {
            config.set("world.limits.private", config.getInt("world.max-amount.private", -1));
        }
        config.set("world.max-amount", null);

        // 4. world.default → world.defaults (flatten settings sub-nesting)
        moveIfPresent(config, "world.default.permission.public", "world.defaults.permission.public");
        moveIfPresent(config, "world.default.permission.private", "world.defaults.permission.private");
        moveIfPresent(config, "world.default.time.sunrise", "world.defaults.time.sunrise");
        moveIfPresent(config, "world.default.time.noon", "world.defaults.time.noon");
        moveIfPresent(config, "world.default.time.night", "world.defaults.time.night");
        moveIfPresent(config, "world.default.worldborder.size", "world.defaults.worldborder-size");
        moveIfPresent(config, "world.default.difficulty", "world.defaults.difficulty");

        // Copy gamerules section
        ConfigurationSection gameRules = config.getConfigurationSection("world.default.gamerules");
        if (gameRules != null) {
            for (String key : gameRules.getKeys(false)) {
                config.set("world.defaults.gamerules." + key, gameRules.get(key));
            }
        }

        // Flatten world.default.settings → world.defaults
        moveIfPresent(config, "world.default.settings.physics", "world.defaults.physics");
        moveIfPresent(config, "world.default.settings.explosions", "world.defaults.explosions");
        moveIfPresent(config, "world.default.settings.mob-ai", "world.defaults.mob-ai");
        moveIfPresent(config, "world.default.settings.block-breaking", "world.defaults.block-breaking");
        moveIfPresent(config, "world.default.settings.block-placement", "world.defaults.block-placement");
        moveIfPresent(config, "world.default.settings.block-interactions", "world.defaults.block-interactions");
        moveIfPresent(config, "world.default.settings.builders-enabled.public", "world.defaults.builders-enabled.public");
        moveIfPresent(config, "world.default.settings.builders-enabled.private", "world.defaults.builders-enabled.private");

        // Delete old world.default entirely
        config.set("world.default", null);

        // 5. world.import-all.delay → world.import-all-delay
        moveIfPresent(config, "world.import-all.delay", "world.import-all-delay");
        config.set("world.import-all", null);

        // 6. Delete dead sections
        config.set("world.disabled-physics", config.contains("world.disabled-physics.prevent-connections") ? null : config.get("world.disabled-physics"));
        // The dead "world.disabled-physics" from V2 (lines 72-75 in old config) - ensure removal if leftover
        // (already handled above by moving settings.disabled-physics → world.disabled-physics - the old dead one at world.disabled-physics was overwritten)
    }

    private static void moveIfPresent(FileConfiguration config, String from, String to) {
        if (config.contains(from)) {
            config.set(to, config.get(from));
        }
    }
}
