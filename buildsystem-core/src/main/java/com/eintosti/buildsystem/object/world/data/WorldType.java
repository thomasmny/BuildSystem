/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.world.data;

import com.eintosti.buildsystem.util.ConfigValues;
import org.bukkit.World.Environment;

/**
 * @author einTosti
 */
public enum WorldType {
    /**
     * The equivalent to a default Minecraft world with {@link Environment#NORMAL}.
     */
    NORMAL,

    /**
     * The equivalent to a super-flat Minecraft world.
     */
    FLAT,

    /**
     * The equivalent to a default Minecraft world with {@link Environment#NETHER}.
     */
    NETHER,

    /**
     * The equivalent to a default Minecraft world with {@link Environment#THE_END}.
     */
    END,

    /**
     * A completely empty world with no blocks at all, except the block a player spawns on.
     *
     * @see ConfigValues#isVoidBlock()
     */
    VOID,

    /**
     * A world which is an identical copy of a provided template.
     */
    TEMPLATE,

    /**
     * A world which by default cannot be modified by any player except for the creator.
     */
    PRIVATE,

    /**
     * A world which was not created by BuildSystem but was imported, so it can be used by the plugin.
     */
    IMPORTED,

    /**
     * A world with a custom chunk generator
     */
    CUSTOM,

    /**
     * A world with an unknown type.
     */
    UNKNOWN
}
