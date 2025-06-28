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
package de.eintosti.buildsystem.api.navigator.settings;

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;

/**
 * Interface for a world filter that restricts which {@link BuildWorld}s are shown to a user in the navigator.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldFilter {

    /**
     * Gets the current mode.
     *
     * @return The mode
     */
    Mode getMode();

    /**
     * Sets the current mode.
     *
     * @param mode The mode
     */
    void setMode(Mode mode);

    /**
     * Gets the text which the filter is applied to.
     *
     * @return The text the filter is applied to
     */
    String getText();

    /**
     * Sets the text which the filter is applied to.
     *
     * @param text The text
     */
    void setText(String text);

    /**
     * Applies the current filter to a {@link BuildWorld} to determine if it should be shown.
     *
     * @return A {@link Predicate} that tests if a {@link BuildWorld} matches the filter criteria
     */
    Predicate<BuildWorld> apply();

    /**
     * Represents the different modes of filtering worlds in the navigator.
     */
    @NullMarked
    enum Mode {

        /**
         * No filtering is applied.
         */
        NONE,

        /**
         * Worlds that name starts with the filter text.
         *
         * @see #getText()
         */
        STARTS_WITH,

        /**
         * Worlds that name contains the filter text.
         *
         * @see #getText()
         */
        CONTAINS,

        /**
         * Worlds that name matches the filter text.
         *
         * @see #getText()
         */
        MATCHES;

        /**
         * Gets the next filtering mode in the sequence.
         *
         * @return The next {@link Mode} in the enumeration
         */
        public Mode getNext() {
            return switch (this) {
                case NONE -> STARTS_WITH;
                case STARTS_WITH -> CONTAINS;
                case CONTAINS -> MATCHES;
                case MATCHES -> NONE;
            };
        }
    }
}