/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.world.data;

import com.eintosti.buildsystem.BuildSystem;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author einTosti
 */
public enum WorldStatus {
    /**
     * Represent a world that has not been modified.
     */
    NOT_STARTED("status_not_started"),

    /**
     * Represents a world that is currently being built.
     * <p>
     * The status is automatically switched to this when a block is placed/broken.
     */
    IN_PROGRESS("status_in_progress"),

    /**
     * Represents a world that has almost been completed.
     */
    ALMOST_FINISHED("status_almost_finished"),

    /**
     * Represents a world that has completed its building phase.
     */
    FINISHED("status_finished"),

    /**
     * Represents an old world that has been finished for a while. Blocks cannot be placed/broken in archived worlds.
     */
    ARCHIVE("status_archive"),

    /**
     * Represents a world that is not shown in the navigator.
     */
    HIDDEN("status_hidden");

    private final String typeNameKey;

    WorldStatus(String typeNameKey) {
        this.typeNameKey = typeNameKey;
    }

    /**
     * Get the display name of the {@link WorldStatus}.
     *
     * @return The type's display name
     */
    public String getName() {
        return JavaPlugin.getPlugin(BuildSystem.class).getString(typeNameKey);
    }
}