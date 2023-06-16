/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.settings;

import de.eintosti.buildsystem.api.world.BuildWorld;

public interface WorldDisplay {

    /**
     * Gets the order in which the {@link BuildWorld}s are sorted.
     *
     * @return The world sort order
     */
    WorldSort getWorldSort();

    /**
     * Sets the order in which the {@link BuildWorld}s are sorted.
     *
     * @param worldSort The world sort order
     */
    void setWorldSort(WorldSort worldSort);

    /**
     * Gets the filter which removed non-matching {@link BuildWorld}s from the navigator
     *
     * @return The world filter
     */
    WorldFilter getWorldFilter();
}