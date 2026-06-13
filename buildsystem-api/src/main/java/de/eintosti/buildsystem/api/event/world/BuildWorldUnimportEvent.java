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
 * Called when a {@link BuildWorld} is removed from BuildSystem's registry without its directory being deleted.
 *
 * <p>This event is also fired as the first stage of a deletion, followed by {@link BuildWorldPostDeleteEvent}.
 *
 * @since TODO
 */
@NullMarked
public class BuildWorldUnimportEvent extends BuildWorldEvent {

    /**
     * Constructs a new {@link BuildWorldUnimportEvent}.
     *
     * @param buildWorld The {@link BuildWorld} that has been unimported
     */
    @Internal
    public BuildWorldUnimportEvent(BuildWorld buildWorld) {
        super(buildWorld);
    }
}
