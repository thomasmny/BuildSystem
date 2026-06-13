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
 * Migrates config from v3 to v4: introduces per-option {@code /settings} permission checks.
 *
 * <ul>
 *   <li>Adds {@code settings.per-option-permissions} (default {@code false}) so existing servers keep today's behavior
 *       until an admin opts in.
 * </ul>
 */
@NullMarked
public class MigrationV3ToV4 implements Migration {

    @Override
    public void migrate(FileConfiguration config) {
        addIfMissing(config, "settings.per-option-permissions", false);
    }

    private static void addIfMissing(FileConfiguration config, String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }
}
