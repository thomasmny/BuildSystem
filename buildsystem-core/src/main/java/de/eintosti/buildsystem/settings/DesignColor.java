/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.settings;

public enum DesignColor {
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

    public static DesignColor matchColor(String colorName) {
        try {
            return valueOf(colorName);
        } catch (IllegalArgumentException e) {
            return DesignColor.BLACK;
        }
    }
}