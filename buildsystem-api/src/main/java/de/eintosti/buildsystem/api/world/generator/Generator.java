/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.world.generator;

public enum Generator {
    /**
     * A normal world
     */
    NORMAL,

    /**
     * A flat world
     */
    FLAT,

    /**
     * A void world
     */
    VOID,

    /**
     * A custom world
     */
    CUSTOM;
}