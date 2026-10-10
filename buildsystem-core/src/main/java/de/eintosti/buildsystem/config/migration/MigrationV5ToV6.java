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

import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Migrates config from v5 to v6: default game rules stored under their pre-1.21.11 names are folded into the names the
 * config ships with.
 *
 * <p>A config from 3.x names them {@code doDaylightCycle} and so on. Saving it on 4.x copied the shipped
 * {@code advance_time: false} in next to the old name, and both were applied to new worlds, the old name last. So the
 * admin's 3.x value won even after they changed {@code advance_time}. Now a changed new name wins, an unchanged one
 * takes the old value, and the old name is removed.
 */
@NullMarked
public class MigrationV5ToV6 implements Migration {

    private static final String GAME_RULES = "world.defaults.gamerules.";

    /**
     *
     * The game rules the config ships with, by their old name, with the new name and the shipped value.
     *
     */
    private static final Map<String, Map.Entry<String, Boolean>> RENAMED = Map.of(
            "doDaylightCycle", Map.entry("advance_time", false),
            "doMobSpawning", Map.entry("spawn_mobs", false),
            "fireDamage", Map.entry("fire_damage", false));

    @Override
    public void migrate(FileConfiguration config) {
        RENAMED.forEach((oldName, shipped) -> {
            String oldPath = GAME_RULES + oldName;
            if (!config.contains(oldPath, true)) {
                return;
            }
            String newPath = GAME_RULES + shipped.getKey();
            boolean changedByAdmin =
                    config.contains(newPath, true) && !Objects.equals(config.get(newPath), shipped.getValue());
            if (!changedByAdmin) {
                config.set(newPath, config.get(oldPath));
            }
            config.set(oldPath, null);
        });
    }
}
