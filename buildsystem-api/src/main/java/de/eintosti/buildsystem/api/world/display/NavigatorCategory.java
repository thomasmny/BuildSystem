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
package de.eintosti.buildsystem.api.world.display;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import org.jspecify.annotations.NullMarked;

/**
 * Represents the different categories used to organize and display {@link BuildWorld}s in the navigator menus. Each category corresponds to a distinct filter or access level for
 * worlds.
 *
 * @since 3.0.0
 */
@NullMarked
public enum NavigatorCategory {

    /**
     * Represents the category for public worlds. This navigator inventory contains all {@link BuildWorld}s that are still being built or are generally accessible.
     */
    PUBLIC,

    /**
     * Represents the category for archived worlds. This navigator inventory contains {@link BuildWorld}s that have been marked with {@link BuildWorldStatus#ARCHIVE}. These worlds
     * are typically read-only and no longer actively built upon.
     *
     * @see BuildWorldStatus#ARCHIVE
     */
    ARCHIVE,

    /**
     * Represents the category for private worlds. This navigator inventory contains {@link BuildWorld}s that are set as private. These worlds can typically only be modified by
     * their creator and explicitly added {@link Builder}s.
     *
     * @see WorldData#privateWorld()
     */
    PRIVATE;

    /**
     * Determines the appropriate {@link NavigatorCategory} for a given {@link BuildWorld} based on its properties.
     * <p>
     * First checks if the world is private ({@link #PRIVATE}), then if it's archived ({@link #ARCHIVE}), otherwise it defaults to {@link #PUBLIC}.
     *
     * @param buildWorld The {@link BuildWorld} for which to determine the category
     * @return The corresponding category
     */
    public static NavigatorCategory of(BuildWorld buildWorld) {
        WorldData worldData = buildWorld.getData();
        if (worldData.privateWorld().get()) {
            return PRIVATE;
        } else if (worldData.status().get() == BuildWorldStatus.ARCHIVE) {
            return ARCHIVE;
        } else {
            return PUBLIC;
        }
    }
}