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
 * Called on the main thread after a {@link BuildWorld}'s directory has been deleted.
 *
 * <p>At this point the {@link BuildWorld} object is no longer registered and must only be used for reading.
 *
 * @since 4.0.0
 */
@NullMarked
public class BuildWorldPostDeleteEvent extends BuildWorldEvent {

    /**
     * Constructs a new {@link BuildWorldPostDeleteEvent}.
     *
     * @param buildWorld The {@link BuildWorld} that has been deleted
     */
    @Internal
    public BuildWorldPostDeleteEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}
