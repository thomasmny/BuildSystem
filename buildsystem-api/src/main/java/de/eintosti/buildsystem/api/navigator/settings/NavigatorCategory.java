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
package de.eintosti.buildsystem.api.navigator.settings;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldData;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the different kinds of navigator types that can be opened.
 *
 * @since 3.0.0
 */
public enum NavigatorCategory {

    /**
     * The navigator inventory which contains all {@link BuildWorld}s that are still being built.
     */
    PUBLIC,

    /**
     * The navigator inventory which contains all archived {@link BuildWorld}s.
     *
     * @see BuildWorldStatus#ARCHIVE
     */
    ARCHIVE,

    /**
     * The navigator inventory which contains all private {@link BuildWorld}s that can only be modified by the world's creator and all players who have been added as a
     * {@link Builder}.
     *
     * @see WorldData#privateWorld()
     */
    PRIVATE;

    /**
     * Returns the appropriate {@link NavigatorCategory} based on the provided {@link BuildWorld}.
     *
     * @param buildWorld The {@link BuildWorld} to determine the category for
     * @return The corresponding {@link NavigatorCategory}
     */
    public static NavigatorCategory of(@NotNull BuildWorld buildWorld) {
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