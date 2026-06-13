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
package de.eintosti.buildsystem.world.data.type;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link Property} that can be serialized to a configuration file.
 *
 * @param <T> The type of the value held
 */
@NullMarked
public interface PersistentProperty<T> extends Property<T> {

    /**
     * Gets the value of this data point formatted for storage in a configuration file. This might involve converting
     * complex objects into simpler types (e.g., enums to strings).
     *
     * @return The value formatted for a config file
     */
    Object getConfigFormat();
}
