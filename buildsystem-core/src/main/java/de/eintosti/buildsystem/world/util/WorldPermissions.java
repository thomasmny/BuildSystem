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
package de.eintosti.buildsystem.world.util;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.builder.Builder;
import de.eintosti.buildsystem.world.data.WorldData;
import de.eintosti.buildsystem.world.data.WorldStatus;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class WorldPermissions {

    private final BuildWorld buildWorld;

    private WorldPermissions(@Nullable BuildWorld buildWorld) {
        this.buildWorld = buildWorld;
    }

    public static WorldPermissions of(@Nullable BuildWorld buildWorld) {
        return new WorldPermissions(buildWorld);
    }

    /**
     * Checks if the given player can enter the world.
     * <p>
     * A player can enter the world if:
     * <ul>
     *   <li>They have the admin permission, {@link #hasAdminPermission(Player)}</li>
     *   <li>They can bypass the view permission, {@link #canBypassViewPermission(Player)}</li>
     *   <li>They are either the creator or a builder</li>
     *   <li>The world is public, i.e., has its permission set to "{@code -}"</li>
     *   <li>They have the permission set in the world data</li>
     * </ul>
     *
     * @param player The player to check
     * @return {@code true} if the player can enter the world, {@code false} otherwise
     */
    public boolean canEnter(Player player) {
        if (buildWorld == null) {
            return false;
        }

        if (hasAdminPermission(player) || canBypassViewPermission(player)) {
            return true;
        }

        if (isCreator(player) || isBuilder(player)) {
            return true;
        }

        String permission = buildWorld.getData().permission().get();
        if (permission.equals("-")) {
            return true;
        }

        return player.hasPermission(permission);
    }

    /**
     * Not every player can always modify the {@link BuildWorld} they are in.
     * <p>
     * Reasons an interaction could be cancelled:
     * <ul>
     *     <li>The world has its {@link WorldStatus} set to archive;</li>
     *     <li>The world has a setting enabled which disallows certain events;</li>
     *     <li>The world only allows {@link Builder}s to build and the player is not such a builder or the creator of the world.</li>
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
    public boolean canModify(Player player, Supplier<Boolean> additionalCheck) {
        if (this.buildWorld == null) {
            return true;
        }

        if (hasAdminPermission(player) || canBypassBuildRestriction(player)) {
            return true;
        }

        if (!additionalCheck.get()) {
            return false;
        }

        if (buildWorld.getData().status().get() == WorldStatus.ARCHIVE) {
            return false;
        }

        if (isCreator(player)) {
            return true;
        }

        if (!buildWorld.getData().buildersEnabled().get()) {
            return true;
        }

        return buildWorld.isBuilder(player);
    }

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
    public boolean canPerformCommand(Player player, String permission) {
        if (hasAdminPermission(player)) {
            return true;
        }

        if (buildWorld == null) {
            // Most commands require the world to be non-null.
            // Nevertheless, return true to allow a "world is null" message to be sent.
            return true;
        }

        if (buildWorld.getBuilderManager().isCreator(player)) {
            return (player.hasPermission(permission + ".self") || player.hasPermission(permission));
        }

        return player.hasPermission(permission + ".other");
    }

    /**
     * Checks if the given player has the admin permission, {@link BuildSystem#ADMIN_PERMISSION}.
     *
     * @param player The player to check
     * @return {@code true} if the player has the admin permission, {@code false} otherwise
     */
    public boolean hasAdminPermission(Player player) {
        return player.hasPermission(BuildSystem.ADMIN_PERMISSION);
    }

    /**
     * Checks if the player can bypass the permission needed for viewing the world in the navigator.
     *
     * @param player The player to check
     * @return {@code true} if the player can bypass the permission, {@code false} otherwise
     */
    private boolean canBypassViewPermission(Player player) {
        if (buildWorld == null) {
            return false;
        }

        WorldData worldData = buildWorld.getData();
        if (worldData.status().get() == WorldStatus.ARCHIVE) {
            return player.hasPermission("buildsystem.bypass.permission.archive");
        }

        return worldData.privateWorld().get()
                ? player.hasPermission("buildsystem.bypass.permission.private")
                : player.hasPermission("buildsystem.bypass.permission.public");
    }

    /**
     * Checks if the player can bypass the build restriction by being in "build mode".
     *
     * @param player The player to check
     * @return {@code true} if the player can bypass the build restriction, {@code false} otherwise
     */
    private boolean canBypassBuildRestriction(Player player) {
        BuildSystem plugin = JavaPlugin.getPlugin(BuildSystem.class);
        return plugin.getPlayerManager().isInBuildMode(player);
    }

    /**
     * Checks if the given player is the creator of the world.
     *
     * @param player The player to check
     * @return {@code true} if the player is the creator, {@code false} otherwise
     */
    public boolean isCreator(Player player) {
        if (buildWorld == null) {
            return false;
        }

        Builder creator = buildWorld.getCreator();
        return creator != null && creator.getUniqueId().equals(player.getUniqueId());
    }

    /**
     * Checks if the given player is the creator or a builder of the world.
     *
     * @param player The player to check
     * @return {@code true} if the player is the creator or a builder, {@code false} otherwise
     */
    public boolean isBuilder(Player player) {
        if (this.buildWorld == null) {
            return false;
        }

        List<Builder> builders = buildWorld.getBuilders();
        return builders.stream().anyMatch(builder -> builder.getUniqueId().equals(player.getUniqueId()));
    }
} 