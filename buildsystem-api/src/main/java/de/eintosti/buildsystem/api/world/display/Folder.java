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
package de.eintosti.buildsystem.api.world.display;

import de.eintosti.buildsystem.api.world.BuildWorld;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Unmodifiable;

public interface Folder extends Displayable {

    /**
     * Gets a list of all world UUIDs in this folder.
     *
     * @return An unmodifiable list of world UUIDs
     */
    @Unmodifiable
    List<UUID> getWorldUUIDs();

    /**
     * Gets whether this folder contains the specific {@link BuildWorld}.
     *
     * @param buildWorld The world to check
     * @return {@code true} if the folder contains the world, {@code false} otherwise
     */
    boolean containsWorld(BuildWorld buildWorld);

    /**
     * Adds a {@link BuildWorld} to this folder.
     *
     * @param buildWorld The world to add
     */
    void addWorld(BuildWorld buildWorld);

    /**
     * Removes a {@link BuildWorld} from this folder.
     *
     * @param buildWorld The world to remove
     */
    void removeWorld(BuildWorld buildWorld);

    /**
     * Gets the number of worlds in this folder.
     *
     * @return The number of worlds in this folder
     */
    int getWorldCount();
} 