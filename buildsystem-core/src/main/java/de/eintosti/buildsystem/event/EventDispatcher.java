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
package de.eintosti.buildsystem.event;

import de.eintosti.buildsystem.event.world.BuildWorldManipulationEvent;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Manages the dispatching of custom events related to build world manipulations.
 *
 * @since TODO
 */
public class EventDispatcher {

    private final WorldManager worldManager;

    /**
     * @param worldManager the world manager used to retrieve build world information
     */
    public EventDispatcher(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    /**
     * <p>Dispatches a build world manipulation event if the player is in a build world
     * and the parent event has not been cancelled.</p>
     *
     * <p>This method checks if:</p>
     * <ol>
     *   <li>The parent event is not already cancelled</li>
     *   <li>The player is in a valid build world</li>
     * </ol>
     * <p>
     * If both conditions are met, it triggers a {@link BuildWorldManipulationEvent}
     * to allow further processing of the player's action.
     *
     * @param player      The player who performed the manipulation
     * @param parentEvent The original event that triggered this potential manipulation
     */
    public void dispatchManipulationEventIfPlayerInBuildWorld(Player player, Cancellable parentEvent) {
        if (parentEvent.isCancelled()) {
            return;
        }
        BuildWorld world = worldManager.getBuildWorld(player.getWorld().getName());
        if (world == null) {
            return;
        }
        Bukkit.getPluginManager().callEvent(new BuildWorldManipulationEvent(parentEvent, player, world));
    }


}
