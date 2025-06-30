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
package de.eintosti.buildsystem.api.storage;

import de.eintosti.buildsystem.api.player.BuildPlayer;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interface for managing the storage of {@link BuildPlayer} objects.
 *
 * @since 3.0.0
 */
@NullMarked
public interface PlayerStorage extends Storage<BuildPlayer> {

    /**
     * Creates a new {@link BuildPlayer} with the given uuid and settings.
     *
     * @param uuid     The uuid of the player
     * @return The created build-player
     */
    BuildPlayer createBuildPlayer(UUID uuid);

    /**
     * Creates a new {@link BuildPlayer} with the given player.
     *
     * @param player The player
     * @return The created build-player
     */
    BuildPlayer createBuildPlayer(Player player);

    /**
     * Gets the {@link BuildPlayer} whose unique-id matches the given uuid.
     *
     * @param uuid The uuid of the player
     * @return The player, if found, otherwise {@code null}
     */
    @Nullable
    BuildPlayer getBuildPlayer(UUID uuid);

    /**
     * Gets the {@link BuildPlayer} which wraps the given player.
     * <p>
     * If the player is not found, a new {@link BuildPlayer} will be created using {@link #createBuildPlayer(Player)}.
     *
     * @param player The wrapped player
     * @return The player
     * @see #createBuildPlayer(Player)
     */
    BuildPlayer getBuildPlayer(Player player);

    /**
     * Gets a collection of all {@link BuildPlayer}s.
     *
     * @return A collection of all build-players.
     */
    @Unmodifiable
    Collection<BuildPlayer> getBuildPlayers();
}
