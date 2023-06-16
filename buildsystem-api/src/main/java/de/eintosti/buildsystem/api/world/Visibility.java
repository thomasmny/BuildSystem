/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world;

public enum Visibility {
    /**
     * Public worlds are displayed in the world navigator.
     */
    PUBLIC,

    /**
     * Private worlds are displayed in an extra menu - the private world navigator.
     */
    PRIVATE,

    /**
     * Used for when the visibility of a world can be ignored.
     */
    IGNORE;

    public static Visibility matchVisibility(boolean isPrivateWorld) {
        return isPrivateWorld ? PRIVATE : PUBLIC;
    }
}