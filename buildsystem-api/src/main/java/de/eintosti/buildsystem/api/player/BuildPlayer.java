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
package de.eintosti.buildsystem.api.player;

import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Represents a player managed by the BuildSystem. This interface provides access to player-specific data and settings within the BuildSystem.
 */
public interface BuildPlayer {

    /**
     * Gets the unique-id of the player.
     * <p>
     * Should match the wrapped {@link Player}'s UUID.
     *
     * @return The player's UUID
     * @see Player#getUniqueId()
     */
    UUID getUniqueId();

    /**
     * Gets the player's custom settings.
     *
     * @return The player's settings
     */
    Settings getSettings();

    /**
     * Gets values that are supposed to be cached for a short amount of time.
     *
     * @return The player's cached values
     */
    @Internal
    CachedValues getCachedValues();

    /**
     * Gets the location where the player was last before logging off.
     *
     * @return The location
     */
    @Internal
    @Nullable
    LogoutLocation getLogoutLocation();

    /**
     * Sets the location where the player was last before logging off.
     *
     * @param logoutLocation The logout location
     */
    @Internal
    void setLogoutLocation(LogoutLocation logoutLocation);

    /**
     * Gets the location the player was last at.
     * <p>
     * Usually this is the last location before teleportation.
     *
     * @return The player's previous location
     */
    @Internal
    @Nullable
    Location getPreviousLocation();

    /**
     * Sets the location the player was last at.
     * <p>
     * Usually this is the last location before teleportation.
     *
     * @param location The location
     */
    @Internal
    void setPreviousLocation(Location location);

    /**
     * Gets the {@link NavigatorCategory} the player last looked at.
     *
     * @return The last looked navigator inventory type
     */
    @Internal
    @Nullable
    NavigatorCategory getLastLookedAt();

    /**
     * Sets the {@link NavigatorCategory} the player last looked at.
     *
     * @param type The last looked navigator inventory type
     */
    @Internal
    void setLastLookedAt(NavigatorCategory type);
}