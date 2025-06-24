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
package de.eintosti.buildsystem.api.world.util;

import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import java.util.function.Supplier;
import org.bukkit.entity.Player;

/**
 * @since 3.0.0
 */
public interface WorldPermissions {

    /**
     * Checks if the given player can enter the world.
     * <p>
     * A player can enter the world if:
     * <ul>
     *   <li>They have the admin permission, {@link #hasAdminPermission(Player)}</li>
     *   <li>They can bypass the view permission, {@link #canBypassViewPermission(Player)}</li>
     *   <li>They are either the creator or a builder</li>
     *   <li>The world is public, i.e. has its permission set to "{@code -}"</li>
     *   <li>They have the permission set in the world data</li>
     * </ul>
     *
     * @param player The player to check
     * @return {@code true} if the player can enter the world, {@code false} otherwise
     */
    boolean canEnter(Player player);

    /**
     * Not every player can always modify the {@link de.eintosti.buildsystem.api.world.BuildWorld} they are in.
     * <p>
     * Reasons an interaction could be cancelled:
     * <ul>
     *     <li>The world has its {@link BuildWorldStatus} set to archive;</li>
     *     <li>The world has a setting enabled which disallows certain events;</li>
     *     <li>The world only allows {@link de.eintosti.buildsystem.api.world.builder.Builder}s to build and the player is not such a builder or the creator of the world.</li>
     * </ul>
     * <p>
     * However, a player can override these reasons if:
     * <ul>
     *     <li>They have the admin permission, {@link #hasAdminPermission(Player)}</li>
     *     <li>They can bypass building restrictions, {@link #canBypassBuildRestriction(Player)}</li>
     * </ul>
     *
     * @param player          The player to check
     * @param additionalCheck An additional check if the player can modify the world. Will not apply to bypass permissions.
     * @return {@code true} if the player can modify the world, {@code false} otherwise
     */
    boolean canModify(Player player, Supplier<Boolean> additionalCheck);

    /**
     * Gets whether the given player is permitted to run a command in the given world.
     * <p>
     * <ul>
     *   <li>The creator of a world is allowed to run the command if they have the given permission, optionally
     *   ending with {@code .self}.</li>
     *   <li>All other players will need the permission {@code <permission>.other} to run the command.</li>
     * </ul>
     *
     * @param player     The player trying to run the command
     * @param permission The permission needed to run the command
     * @return {@code true} if the player is allowed to run the command, {@code false} otherwise
     */
    boolean canPerformCommand(Player player, String permission);

    /**
     * Checks if the given player has the admin permission, {@code buildsystem.admin}.
     *
     * @param player The player to check
     * @return {@code true} if the player has the admin permission, {@code false} otherwise
     */
    boolean hasAdminPermission(Player player);

    /**
     * Checks if the player can bypass the permission needed for viewing the world in the navigator.
     *
     * @param player The player to check
     * @return {@code true} if the player can bypass the permission, {@code false} otherwise
     */
    boolean canBypassViewPermission(Player player);

    /**
     * Checks if the player can bypass the build restriction by being in "build mode".
     *
     * @param player The player to check
     * @return {@code true} if the player can bypass the build restriction, {@code false} otherwise
     */
    boolean canBypassBuildRestriction(Player player);
} 