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

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Migrates config from v3 to v4: introduces additional permission-granularity controls.
 *
 * <ul>
 *   <li>Adds {@code settings.per-option-permissions} (default {@code false}) so existing servers keep today's behavior
 *       until an admin opts in.
 *   <li>Adds {@code settings.world-permission-whitelist} (default empty list) so by default any permission lock may be
 *       set via {@code /worlds setPermission}.
 *   <li>Adds {@code settings.restrict-template-access} (default {@code false}) so template and generator use is
 *       unrestricted until an admin opts in.
 * </ul>
 */
@NullMarked
public class MigrationV3ToV4 implements Migration {

    @Override
    public void migrate(FileConfiguration config) {
        addIfMissing(config, "settings.per-option-permissions", false);
        addIfMissing(config, "settings.world-permission-whitelist", List.of());
        addIfMissing(config, "settings.restrict-template-access", false);
    }

    private static void addIfMissing(FileConfiguration config, String path, Object value) {
        if (!config.contains(path)) {
            config.set(path, value);
        }
    }
}
