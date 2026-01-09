/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
package de.eintosti.buildsystem.api.world.data;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.jspecify.annotations.NullMarked;

/**
 * Defines the visibility settings for a {@link BuildWorld} within the BuildSystem. These settings determine how worlds are displayed and accessed in the world navigator.
 *
 * @since 3.0.0
 */
@NullMarked
public enum Visibility {

    /**
     * Indicates that a world is publicly accessible and displayed in the main world navigator.
     */
    PUBLIC,

    /**
     * Indicates that a world is private, typically only visible and accessible to its creator and designated builders. Private worlds are usually displayed in a separate,
     * dedicated menu.
     */
    PRIVATE,

    /**
     * A special state indicating that the visibility setting of a world should be disregarded. This is useful for internal operations or specific contexts where visibility rules
     * do not apply.
     */
    IGNORE;

    /**
     * Returns the appropriate {@link Visibility} enum based on whether a world is private.
     *
     * @param isPrivateWorld A boolean indicating if the world is private
     * @return {@link #PRIVATE} if {@link WorldData#privateWorld()} is true, otherwise {@link #PUBLIC}
     */
    public static Visibility matchVisibility(boolean isPrivateWorld) {
        return isPrivateWorld ? PRIVATE : PUBLIC;
    }
}