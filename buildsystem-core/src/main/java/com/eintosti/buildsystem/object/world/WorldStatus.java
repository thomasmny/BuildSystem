/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.world;

/**
 * @author einTosti
 */
public enum WorldStatus {
    /**
     * Represent a world that has not been modified.
     */
    NOT_STARTED,

    /**
     * Represents a world that is currently being built.
     * <p>
     * The status is automatically switched to this when a block is placed/broken.
     */
    IN_PROGRESS,

    /**
     * Represents a world that has almost been completed.
     */
    ALMOST_FINISHED,

    /**
     * Represents a world that has completed its building phase.
     */
    FINISHED,

    /**
     * Represents an old world that has been finished for a while. Blocks cannot be placed/broken in archived worlds.
     */
    ARCHIVE,

    /**
     * Represents a world that is not shown in the navigator.
     */
    HIDDEN
}
