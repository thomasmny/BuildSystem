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
package de.eintosti.buildsystem.api.event.world;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Called when a player's build mode is toggled. This event can be triggered by the player themselves or by another plugin/player.
 */
@NullMarked
public class PlayerBuildModeToggleEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final boolean buildMode;
    private final Player causer;
    private boolean cancelled;

    /**
     * Constructs a new {@link PlayerBuildModeToggleEvent}.
     *
     * @param who       The player whose build mode is being toggled
     * @param buildMode The new build mode status ({@code true} for enabled, {@code false} for disabled)
     * @param causer    The player who caused the build mode to be toggled
     */
    public PlayerBuildModeToggleEvent(Player who, boolean buildMode, Player causer) {
        super(who);
        this.buildMode = buildMode;
        this.causer = causer;
        this.cancelled = false;
    }

    /**
     * Gets the new build mode status.
     *
     * @return {@code true} if build mode is being enabled, {@code false} otherwise
     */
    public boolean isBuildMode() {
        return this.buildMode;
    }

    /**
     * Gets the player who caused the build mode to be toggled.
     * <p>
     * This will return the player themselves if they toggled their own build mode.
     *
     * @return The player who caused the action, or null.
     */
    public Player getCauser() {
        return this.causer;
    }

    /**
     * Gets the cancellation state of this event. A cancelled event will not be executed in the server, but will still pass to other plugins
     *
     * @return true if this event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Sets the cancellation state of this event. A cancelled event will not be executed in the server, but will still pass to other plugins.
     *
     * @param cancel true if you wish to cancel this event
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
