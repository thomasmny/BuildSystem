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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.util.WorldPermissions;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class WorldPermissionsImpl implements WorldPermissions {

    private final BuildWorld buildWorld;

    private WorldPermissionsImpl(@Nullable BuildWorld buildWorld) {
        this.buildWorld = buildWorld;
    }

    public static WorldPermissionsImpl of(@Nullable BuildWorld buildWorld) {
        return new WorldPermissionsImpl(buildWorld);
    }

    @Override
    public boolean canEnter(Player player) {
        if (buildWorld == null) {
            return false;
        }

        if (hasAdminPermission(player) || canBypassViewPermission(player)) {
            return true;
        }

        Builders builders = buildWorld.getBuilders();
        if (builders.isCreator(player) || builders.isBuilder(player)) {
            return true;
        }

        String permission = buildWorld.getData().permission().get();
        if (permission.equals("-")) {
            return true;
        }

        return player.hasPermission(permission);
    }

    @Override
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

        if (buildWorld.getData().status().get() == BuildWorldStatus.ARCHIVE) {
            return false;
        }

        Builders builders = buildWorld.getBuilders();
        if (builders.isCreator(player)) {
            return true;
        }

        if (!buildWorld.getData().buildersEnabled().get()) {
            return true;
        }

        return builders.isBuilder(player);
    }

    @Override
    public boolean canPerformCommand(Player player, String permission) {
        if (hasAdminPermission(player)) {
            return true;
        }

        if (buildWorld == null) {
            // Most commands require the world to be non-null.
            // Nevertheless, return true to allow a "world is null" message to be sent.
            return true;
        }

        if (buildWorld.getBuilders().isCreator(player)) {
            return (player.hasPermission(permission + ".self") || player.hasPermission(permission));
        }

        return player.hasPermission(permission + ".other");
    }

    @Override
    public boolean hasAdminPermission(Player player) {
        return player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION);
    }

    @Override
    public boolean canBypassViewPermission(Player player) {
        if (buildWorld == null) {
            return false;
        }

        WorldData worldData = buildWorld.getData();
        if (worldData.status().get() == BuildWorldStatus.ARCHIVE) {
            return player.hasPermission("buildsystem.bypass.permission.archive");
        }

        return worldData.privateWorld().get()
                ? player.hasPermission("buildsystem.bypass.permission.private")
                : player.hasPermission("buildsystem.bypass.permission.public");
    }

    @Override
    public boolean canBypassBuildRestriction(Player player) {
        BuildSystemPlugin plugin = JavaPlugin.getPlugin(BuildSystemPlugin.class);
        return plugin.getPlayerService().isInBuildMode(player);
    }
} 