/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.settings;

/**
 * @author einTosti
 */
public enum GlassColor {
    RED,
    ORANGE,
    YELLOW,
    PINK,
    MAGENTA,
    PURPLE,
    BROWN,
    LIME,
    GREEN,
    BLUE,
    CYAN,
    LIGHT_BLUE,
    WHITE,
    GRAY,
    LIGHT_GRAY,
    BLACK;

    public static GlassColor matchColor(String colorName) {
        try {
            return valueOf(colorName);
        } catch (IllegalArgumentException e) {
            return GlassColor.BLACK;
        }
    }
}
