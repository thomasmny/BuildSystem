/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world.data;

import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.world.BuildWorld;

public enum WorldStatus {
    /**
     * Represent a world that has not been modified.
     */
    NOT_STARTED("status_not_started", 1),

    /**
     * Represents a world that is currently being built.
     * <p>
     * The status is automatically switched to this when a block is placed/broken.
     */
    IN_PROGRESS("status_in_progress", 2),

    /**
     * Represents a world that has almost been completed.
     */
    ALMOST_FINISHED("status_almost_finished", 3),

    /**
     * Represents a world that has completed its building phase.
     */
    FINISHED("status_finished", 4),

    /**
     * Represents an old world that has been finished for a while. Blocks cannot be placed/broken in archived worlds.
     */
    ARCHIVE("status_archive", 5),

    /**
     * Represents a world that is not shown in the navigator.
     */
    HIDDEN("status_hidden", 6);

    private final String typeNameKey;
    private final int stage;

    WorldStatus(String typeNameKey, int stage) {
        this.typeNameKey = typeNameKey;
        this.stage = stage;
    }

    /**
     * Get the display name of the {@link WorldStatus}.
     *
     * @return The type's display name
     */
    public String getName() {
        return Messages.getString(typeNameKey);
    }

    /**
     * Gets the stage in which the {@link BuildWorld} is currently in.
     * A higher value means the world is further in development.
     *
     * @return the stage in which the world is currently in.
     */
    public int getStage() {
        return stage;
    }
}