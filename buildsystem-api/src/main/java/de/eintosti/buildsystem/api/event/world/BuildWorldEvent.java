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
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a {@link BuildWorld} related event.
 *
 * @since 3.0.0
 */
@NullMarked
public class BuildWorldEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final BuildWorld buildWorld;

    /**
     * Constructs a new {@link BuildWorldEvent}.
     *
     * @param buildWorld The {@link BuildWorld} involved in this event
     */
    @Internal
    public BuildWorldEvent(BuildWorld buildWorld) {
        this.buildWorld = buildWorld;
    }

    /**
     * Gets the {@link BuildWorld} involved in this event
     *
     * @return The world involved in this event
     */
    public BuildWorld getBuildWorld() {
        return buildWorld;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Gets the handler list for this event.
     *
     * @return The handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}