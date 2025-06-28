/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class MigrationV1ToV2 implements Migration {

    @Override
    public void migrate(FileConfiguration config) {
        if (config.contains("settings.archive-vanish")) {
            config.set("settings.archive.vanish", config.getBoolean("settings.archive-vanish"));
            config.set("settings.archive-vanish", null);
        }

        if (config.contains("settings.archive-should-change-gamemode")) {
            config.set("settings.archive.change-gamemode", config.getBoolean("settings.archive-should-change-gamemode"));
            config.set("settings.archive-should-change-gamemode", null);
        }

        if (config.contains("settings.archive-world-game-mode")) {
            config.set("settings.archive.world-gamemode", config.getString("settings.archive-world-game-mode"));
            config.setComments("settings.archive.world-gamemode", List.of("Options: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR"));
            config.set("settings.archive-world-game-mode", null);
        }

        if (config.contains("settings.teleport-after-creation")) {
            config.set("settings.teleport-after-creation", null);
        }

        if (config.contains("world.void-block")) {
            config.set("world.void-block", null);
        }
    }
}
