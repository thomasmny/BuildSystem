/*
 * Copyright (c) 2018-2025, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.api.player.settings;

/**
 * A {@link DesignColor} is the color which glass panes are tinted to in different menus.
 *
 * @since 3.0.0
 */
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

    /**
     * Gets the {@link DesignColor} from a string.
     *
     * @param colorName The name of the color
     * @return The {@link DesignColor} or {@link DesignColor#BLACK} if the color does not exist
     */
    public static DesignColor matchColor(String colorName) {
        try {
            return valueOf(colorName);
        } catch (IllegalArgumentException e) {
            return DesignColor.BLACK;
        }
    }
}