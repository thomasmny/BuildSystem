/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.player;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface PlayerManager {

    /**
     * Gets a build-player object by the given uuid.
     *
     * @param uuid The uuid to look up
     * @return The player, if found, otherwise {@code null}
     */
    BuildPlayer getBuildPlayer(UUID uuid);

    /**
     * Gets a build-player object by the given player.
     *
     * @param player The uuid to look up
     * @return The player, if found, otherwise {@code null}
     */
    BuildPlayer getBuildPlayer(Player player);

    /**
     * Gets a collection of all {@link BuildPlayer}s.
     *
     * @return A collection of all build-players.
     */
    @Unmodifiable
    Collection<BuildPlayer> getBuildPlayers();

    /**
     * Gets a set of all players currently in "build mode".
     *
     * @return A set of all players in "build mode".
     * @see #isInBuildMode(Player)
     */
    Set<UUID> getBuildModePlayers();

    /**
     * Gets whether a player is currently in "build mode".
     *
     * @param player The player
     * @return {@code true} if the player is in "build mode", otherwise {@code false}
     */
    boolean isInBuildMode(Player player);

    /**
     * Gets whether the given player is allowed to create a new {@link BuildWorld}.<br>
     * This depends on the following factors:
     * <ul>
     *  <li>Is the maximum amount of worlds set by the config less than the amount of existing worlds?</li>
     *  <li>Is the maximum amount of worlds created by the player less than the amount of worlds said player is allowed to create?</li>
     * </ul>
     *
     * @param player     The player trying to create a world
     * @param visibility The visibility of the world trying to be created
     * @return {@code true} if the player is allowed to create a world, otherwise {@code false}
     */
    boolean canCreateWorld(Player player, Visibility visibility);

    /**
     * Returns the maximum amount of {@link BuildWorld}s a player can create.
     * <p>
     * If the player has the permission {@code buildsystem.admin}, unlimited worlds can be created.
     * Otherwise, there are two different permissions to set said amount:
     * <p>
     * To set the maximum of...
     * <ul>
     *  <li>...public worlds, use {@literal buildsystem.create.public.<amount>}</li>
     *  <li>...private worlds, use {@literal buildsystem.create.private.<amount>}</li>
     * </ul>
     *
     * @param player     The player object
     * @param visibility The visibility of the worlds to check the maximum of. Possible values: {@link Visibility#PUBLIC} or {@link Visibility#PRIVATE}
     * @return If set, the maximum amount of worlds a player can create, otherwise -1
     */
    int getMaxWorlds(Player player, Visibility visibility);
}