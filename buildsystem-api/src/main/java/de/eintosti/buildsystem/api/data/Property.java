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
 * A generic interface representing a single configurable world property.
 *
 * @param <T> The type of the value held by this property
 */
@NullMarked
public interface Property<T> {

    /**
     * An immutable implementation of the {@link Property} interface using a Java Record. This class holds a final,
     * read-only value.
     *
     * @param <T> The type of the value held
     * @param value The immutable value
     */
    record Immutable<T>(T value) implements Property<T> {

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
         * Throws {@link UnsupportedOperationException} as this property is immutable.
         *
         * @param value The value to set (which is ignored)
         * @throws UnsupportedOperationException Always, as this property cannot be modified
         */
        @Contract("_ -> fail")
        @Override
        public void set(T value) {
            throw new UnsupportedOperationException("This property is immutable and cannot be modified.");
        }
    }

    /**
     * An immutable {@link Property} representing the boolean value {@code true}.
     */
    Property<Boolean> TRUE = new Immutable<>(true);

    /**
     * Gets the current value of this property.
     *
     * @return The current value
     */
    T get();

    /**
     * Sets the value of this property.
     *
     * @param value The new value to set
     */
    void set(T value);
}
