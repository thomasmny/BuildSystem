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
package de.eintosti.buildsystem.api.event.world;

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;

/**
 * Called after a {@link BuildWorld} has been renamed.
 *
 * <p>By the time this event fires the rename has completed, so {@link BuildWorld#getName()} already returns the new
 * name.
 *
 * @since TODO
 */
@NullMarked
public class BuildWorldRenameEvent extends BuildWorldEvent {

    private final String oldName;
    private final String newName;

    /**
     * Constructs a new {@link BuildWorldRenameEvent}.
     *
     * @param buildWorld The {@link BuildWorld} that has been renamed
     * @param oldName The name the world had before the rename
     * @param newName The name the world has after the rename
     */
    @Internal
    public BuildWorldRenameEvent(BuildWorld buildWorld, String oldName, String newName) {
        super(buildWorld);
        this.oldName = oldName;
        this.newName = newName;
    }

    /**
     * Gets the name the world had before the rename.
     *
     * @return The previous name of the world
     */
    public String getOldName() {
        return oldName;
    }

    /**
     * Gets the name the world has after the rename.
     *
     * @return The new name of the world
     */
    public String getNewName() {
        return newName;
    }
}
