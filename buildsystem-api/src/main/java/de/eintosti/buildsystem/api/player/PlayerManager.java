/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.player;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.Visibility;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface PlayerManager {

    Collection<BuildPlayer> getBuildPlayers();

    BuildPlayer getBuildPlayer(UUID uuid);

    BuildPlayer getBuildPlayer(Player player);

    Set<UUID> getBuildModePlayers();

    boolean isInBuildMode(Player player);

    /**
     * Gets whether the given player is allowed to create a new {@link BuildWorld}.<br>
     * This depends on the following factors:
     * <ul>
     *  <li>Is the maximum amount of worlds set by the config less than the amount of existing worlds?</li>
     *  <li>Is the maximum amount of worlds created by the player less than the amount of worlds said player is allowed to create?</li>
     * <ul>
     *
     * @param player     The player trying to create a world
     * @param visibility The visibility of the world trying to be created
     * @return {@code true} if the player is allowed to create a world, otherwise {@code false}
     */
    boolean canCreateWorld(Player player, Visibility visibility);

    /**
     * Returns the maximum amount of {@link BuildWorld}s a player can create.<br>
     * If the player has the permission {@code buildsystem.admin}</li>, unlimited worlds can be created.<br>
     * Otherwise, there are two different permissions to set said amount:<br>
     * To set the maximum of...
     * <ul>
     *  <li>...public worlds, use {@literal buildsystem.create.public.<amount>}</li>
     *  <li>...private worlds, use {@literal buildsystem.create.private.<amount>}</li>
     * <ul>
     *
     * @param player The player object
     * @return If set, the maximum amount of worlds a player can create, otherwise -1
     */
    int getMaxWorlds(Player player, boolean privateWorld);
}