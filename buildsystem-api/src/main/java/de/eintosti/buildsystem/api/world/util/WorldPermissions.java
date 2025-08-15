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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Manages and checks permissions related to {@link BuildWorld}s within the BuildSystem. This interface handles permissions for actions such as entering, modifying, and executing
 * commands in worlds.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldPermissions {

    /**
     * Checks if the given {@link Player} is allowed to enter the world associated with these permissions.
     * <p>
     * A player can enter if any of the following conditions are met:
     * <ul>
     *   <li>They have the administrative permission ({@link #hasAdminPermission(Player)}).</li>
     *   <li>They can bypass the view permission ({@link #canBypassViewPermission(Player)}).</li>
     *   <li>They are either the world's creator or an assigned builder.</li>
     *   <li>The world is public (its permission is set to "{@code -}").</li>
     *   <li>They possess the specific permission defined in the world's data.</li>
     * </ul>
     *
     * @param player The {@link Player} to check
     * @return {@code true} if the player can enter the world, {@code false} otherwise
     */
    boolean canEnter(Player player);

    /**
     * Determines if the given {@link Player} can modify the {@link BuildWorld} they are currently in.
     * <p>
     * Modifications might be disallowed due to:
     * <ul>
     *     <li>The world having its {@link BuildWorldStatus} set to {@link BuildWorldStatus#ARCHIVE}.</li>
     *     <li>A world setting is enabled that specifically prohibits certain events (e.g., block placement/breaking).</li>
     *     <li>The world is configured to only allow designated {@link Builder}s, and the player is neither a builder nor the world's creator.</li>
     * </ul>
     * However, a player can bypass these restrictions if:
     * <ul>
     *     <li>They have the administrative permission ({@link #hasAdminPermission(Player)}).</li>
     *     <li>They are in a "build mode" that allows them to bypass building restrictions ({@link #canBypassBuildRestriction(Player)}).</li>
     * </ul>
     *
     * @param player          The {@link Player} attempting to modify the world
     * @param additionalCheck An optional {@link Supplier} that provides an additional boolean check for modification permission. This check does not apply if bypass permissions
     *                        are active.
     * @return {@code true} if the player is allowed to modify the world, {@code false} otherwise
     */
    boolean canModify(Player player, Supplier<Boolean> additionalCheck);

    /**
     * Checks if the given {@link Player} is permitted to execute a specific command within the context of the current world.
     * <p>
     * Permissions are handled as follows:
     * <ul>
     *   <li>The world's creator can run the command if they have the base permission, optionally ending with {@code .self}.</li>
     *   <li>All other players require the permission {@code <permission>.other} to execute the command.</li>
     * </ul>
     *
     * @param player     The {@link Player} attempting to run the command
     * @param permission The base permission string required for the command (e.g., "buildsystem.command.mycommand")
     * @return {@code true} if the player is authorized to run the command, {@code false} otherwise
     */
    boolean canPerformCommand(Player player, @Nullable String permission);

    /**
     * Checks if the given {@link Player} possesses the administrative permission, typically "{@code buildsystem.admin}". Players with this permission can bypass many
     * world-specific restrictions.
     *
     * @param player The {@link Player} to check
     * @return {@code true} if the player has the administrative permission, {@code false} otherwise
     */
    boolean hasAdminPermission(Player player);

    /**
     * Checks if the player can bypass the permission required to view a private world in the navigator. This is separate from the `canEnter` permission and relates specifically to
     * listing the world.
     *
     * @param player The {@link Player} to check
     * @return {@code true} if the player can bypass the view permission, {@code false} otherwise
     */
    boolean canBypassViewPermission(Player player);

    /**
     * Checks if the given {@link Player} can bypass standard building restrictions due to being in a special "build mode" or having a bypass permission. This allows players to
     * modify worlds even if general building is disabled.
     *
     * @param player The {@link Player} to check
     * @return {@code true} if the player can bypass build restrictions, {@code false} otherwise
     */
    boolean canBypassBuildRestriction(Player player);
} 