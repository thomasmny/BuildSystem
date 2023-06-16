/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.settings;

import de.eintosti.buildsystem.api.world.BuildWorld;

import java.util.function.Predicate;

public interface WorldFilter {

    enum Mode {
        NONE("world_filter_mode_none"),
        STARTS_WITH("world_filter_mode_starts_with"),
        CONTAINS("world_filter_mode_contains"),
        MATCHES("world_filter_mode_matches");

        private final String loreKey;

        Mode(String loreKey) {
            this.loreKey = loreKey;
        }

        public String getLoreKey() {
            return loreKey;
        }

        public Mode getNext() {
            switch (this) {
                default: // NONE
                    return STARTS_WITH;
                case STARTS_WITH:
                    return CONTAINS;
                case CONTAINS:
                    return MATCHES;
                case MATCHES:
                    return NONE;
            }
        }
    }

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
     * Apply the filter to a {@link BuildWorld}.
     *
     * @return The filter predicate
     */
    Predicate<BuildWorld> apply();
}