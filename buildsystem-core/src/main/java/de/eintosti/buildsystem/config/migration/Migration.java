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
 * Interface for a single configuration migration step. Each implementation should know how to migrate a configuration from a specific source version to the next version.
 */
@NullMarked
public interface Migration {

    /**
     * Applies the migration logic to the given configuration.
     * <p>
     * The implementation is responsible for modifying the config's internal data and potentially its version, though the manager will handle version incrementing.
     *
     * @param config The configuration to migrate
     */
    void migrate(FileConfiguration config);
}
