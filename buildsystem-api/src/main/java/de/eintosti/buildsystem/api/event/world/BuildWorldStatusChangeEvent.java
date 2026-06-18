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
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;

/**
 * Called when a {@link BuildWorld}'s {@link BuildWorldStatus} changes.
 *
 * <p>This event is fired synchronously from whichever thread mutates the status, and only when the value actually
 * changes.
 *
 * @since 4.0.0
 */
@NullMarked
public class BuildWorldStatusChangeEvent extends BuildWorldEvent {

    private final BuildWorldStatus previousStatus;
    private final BuildWorldStatus newStatus;

    /**
     * Constructs a new {@link BuildWorldStatusChangeEvent}.
     *
     * @param buildWorld The {@link BuildWorld} whose status changed
     * @param previousStatus The status the world had before the change
     * @param newStatus The status the world has after the change
     */
    @Internal
    public BuildWorldStatusChangeEvent(
            BuildWorld buildWorld, BuildWorldStatus previousStatus, BuildWorldStatus newStatus) {
        super(buildWorld);
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    /**
     * Gets the status the world had before the change.
     *
     * @return The previous status of the world
     */
    public BuildWorldStatus getPreviousStatus() {
        return previousStatus;
    }

    /**
     * Gets the status the world has after the change.
     *
     * @return The new status of the world
     */
    public BuildWorldStatus getNewStatus() {
        return newStatus;
    }
}
