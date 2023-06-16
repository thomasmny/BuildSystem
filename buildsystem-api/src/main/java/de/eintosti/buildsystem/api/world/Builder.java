/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface Builder {

    /**
     * Returns a unique and persistent id for the builder.
     * Should be equal to the corresponding {@link Player}'s unique id.
     *
     * @return The uuid
     * @see Player#getUniqueId()
     */
    UUID getUuid();

    /**
     * Gets the name of the builder.
     *
     * @return The builder name
     */
    String getName();

    /**
     * Sets the name of the builder.
     *
     * @param name The name to change to
     */
    void setName(String name);
}