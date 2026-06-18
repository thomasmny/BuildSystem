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
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;

/**
 * Fired when a player modifies a {@link BuildWorld}, providing a single hook for the many distinct Bukkit events that
 * represent world modification, such as:
 *
 * <ul>
 *   <li>breaking a block
 *   <li>placing a block
 *   <li>other modification-related interactions
 * </ul>
 *
 * <p>This event wraps the {@link #getParentEvent() parent Bukkit event} that triggered it: its cancellation state is
 * delegated to the parent, so cancelling this event also cancels the underlying interaction (and vice versa).
 *
 * <p>BuildSystem itself cancels this event at {@link org.bukkit.event.EventPriority#LOW} when the player is not allowed
 * to modify the world; listeners running at a later priority can observe or override that decision.
 *
 * @since 4.0.0
 */
@NullMarked
public class BuildWorldManipulationEvent extends BuildWorldEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Cancellable parentEvent;
    private final Player player;

    @Internal
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
     * Gets the underlying Bukkit event that triggered this manipulation event. This event's cancellation state is
     * delegated to the returned parent.
     *
     * @return The parent event that caused this manipulation event to fire
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

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
