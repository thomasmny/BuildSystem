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
package de.eintosti.buildsystem.event;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.event.world.BuildWorldManipulationEvent;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jspecify.annotations.NullMarked;

/**
 * Manages the dispatching of custom events related to build world manipulations.
 */
@NullMarked
public class EventDispatcher {

    private final WorldStorageImpl worldStorage;

    /**
     * Creates a new {@link EventDispatcher} instance.
     *
     * @param worldStorage The world storage used to retrieve {@link BuildWorld} information
     */
    public EventDispatcher(WorldStorageImpl worldStorage) {
        this.worldStorage = worldStorage;
    }

    /**
     * <p>Dispatches a build world manipulation event if the player is in a {@link BuildWorld}
     * and the parent event has not been canceled.</p>
     *
     * <p>This method checks if:</p>
     * <ol>
     *   <li>The parent event is not yet canceled</li>
     *   <li>The player is in a valid build world</li>
     * </ol>
     * <p>
     * If both conditions are met, it triggers a {@link BuildWorldManipulationEvent}
     * to allow further processing of the player's action.
     *
     * @param player      The player who performed the manipulation
     * @param parentEvent The original event that triggered this potential manipulation
     */
    public void tryDispatchManipulationEvent(Player player, Cancellable parentEvent) {
        if (parentEvent.isCancelled()) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(player.getWorld());
        if (buildWorld == null) {
            return;
        }

        Bukkit.getPluginManager().callEvent(new BuildWorldManipulationEvent(parentEvent, player, buildWorld));
    }
}
