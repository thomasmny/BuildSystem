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
package de.eintosti.buildsystem.event.world;

import de.eintosti.buildsystem.api.event.world.BuildWorldEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.listener.WorldManipulateListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jspecify.annotations.NullMarked;

/**
 * This event reduces duplicated code.
 * <p>
 * It will be called when:
 * <ul>
 *     <li>Breaking Blocks</li>
 *     <li>Placing Blocks</li>
 *     <li>Other modification-related stuff</li>
 * </ul>
 * Cancelling this event will affect the parent-Event, which has caused the ManipulationEvent to fire.
 * <p>Expect the manipulation event to be canceled at {@link org.bukkit.event.EventPriority#LOW} if the player is not allowed to interact with the world.</p>
 *
 * @see WorldManipulateListener
 */
@NullMarked
public class BuildWorldManipulationEvent extends BuildWorldEvent implements Cancellable {

    private final Cancellable parentEvent;
    private final Player player;

    public BuildWorldManipulationEvent(Cancellable parentEvent, Player player, BuildWorld buildWorld) {
        super(buildWorld);
        this.parentEvent = parentEvent;
        this.player = player;
    }

    @Override
    public boolean isCancelled() {
        return parentEvent.isCancelled();
    }

    @Override
    public void setCancelled(boolean cancelled) {
        parentEvent.setCancelled(cancelled);
    }

    /**
     * @return the event which has caused the manipulation event to fire.
     */
    public Cancellable getParentEvent() {
        return parentEvent;
    }

    /**
     * Returns the player manipulating the {@link BuildWorld}.
     *
     * @return The player manipulating the world
     */
    public Player getPlayer() {
        return player;
    }
}
