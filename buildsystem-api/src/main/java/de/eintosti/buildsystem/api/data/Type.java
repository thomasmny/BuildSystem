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
package de.eintosti.buildsystem.api.data;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

/**
 * A generic interface representing a configurable data type.
 *
 * @param <T> The type of the value held by this data point
 */
@NullMarked
public interface Type<T> {

    /**
     * An immutable implementation of the {@link Type} interface using a Java Record. This class holds a final, read-only value.
     *
     * @param <T>   The type of the value held
     * @param value The immutable value
     */
    record ImmutableType<T>(T value) implements Type<T> {

        /**
         * Gets the immutable value.
         *
         * @return The value
         */
        @Override
        public T get() {
            return value;
        }

        /**
         * Throws {@link UnsupportedOperationException} as this type is immutable.
         *
         * @param value The value to set (which is ignored)
         * @throws UnsupportedOperationException Always, as this type cannot be modified
         */
        @Contract("_ -> fail")
        @Override
        public void set(T value) {
            throw new UnsupportedOperationException("This Type is immutable and cannot be modified.");
        }

        /**
         * Gets the immutable value formatted for storage.
         *
         * @return The immutable value
         */
        @Override
        public Object getConfigFormat() {
            return value;
        }
    }

    /**
     * An immutable {@link Type} representing the boolean value {@code true}.
     */
    Type<Boolean> TRUE = new ImmutableType<>(true);

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
     * Gets the value of this data point formatted for storage in a configuration file.
     * This might involve converting complex objects into simpler types (e.g., enums to strings).
     *
     * @return The value formatted for a config file
     */
    Object getConfigFormat();
}