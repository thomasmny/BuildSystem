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
package de.eintosti.buildsystem.api.data;

import org.jspecify.annotations.NullMarked;

/**
 * A generic interface representing a configurable data type.
 *
 * @param <T> The type of the value held by this data point
 */
@NullMarked
public interface Type<T> {

    /**
     * Gets the current value of this data point.
     *
     * @return The current value
     */
    T get();

    /**
     * Sets the value of this data point.
     *
     * @param value The new value to set
     */
    void set(T value);

    /**
     * Gets the value of this data point formatted for storage in a configuration file. This might involve converting complex objects into simpler types (e.g., enums to strings).
     *
     * @return The value formatted for a config file
     */
    Object getConfigFormat();
}