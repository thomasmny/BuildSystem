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
package de.eintosti.buildsystem.world.lifecycle;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.world.WorldContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldPermissionsImpl implements WorldPermissions {

    private final WorldContext context;

    private final @Nullable BuildWorld buildWorld;

    private WorldPermissionsImpl(WorldContext context, @Nullable BuildWorld buildWorld) {
        this.context = context;
        this.buildWorld = buildWorld;
    }

    @Contract("_, _ -> new")
    public static WorldPermissionsImpl of(WorldContext context, @Nullable BuildWorld buildWorld) {
        return new WorldPermissionsImpl(context, buildWorld);
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

        String permission = buildWorld.getData().get(WorldDataKey.PERMISSION);
        if (permission.equals("-")) {
            return true;
        }

        return player.hasPermission(permission);
    }

    @Override
    public boolean canModify(Player player) {
        return evaluateModify(player, null);
    }

    @Override
    public boolean canModify(Player player, WorldSetting setting) {
        return evaluateModify(player, setting);
    }

    private boolean evaluateModify(Player player, @Nullable WorldSetting setting) {
        if (buildWorld == null) {
            return true;
        }

        if (hasAdminPermission(player) || canBypassBuildRestriction(player)) {
            return true;
        }

        if (!buildWorld.getData().get(WorldDataKey.STATUS).isBuildingAllowed()
                && !player.hasPermission("buildsystem.bypass.archive")) {
            return false;
        }

        if (setting != null) {
            if (player.hasPermission(setting.getBypassPermission())) {
                return true;
            }
            if (!setting.isEnabled(buildWorld.getData())) {
                return false;
            }
        }

        Builders builders = buildWorld.getBuilders();
        return builders.isCreator(player)
                || builders.isBuilder(player)
                || player.hasPermission("buildsystem.bypass.builders")
                || !buildWorld.getData().get(WorldDataKey.BUILDERS_ENABLED);
    }

    @Override
    public boolean canPerformCommand(Player player, @Nullable String permission) {
        if (permission == null || permission.isEmpty()) {
            // If no permission is specified, we assume the command can be executed by anyone.
            return true;
        }

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
        if (!worldData.get(WorldDataKey.STATUS).isBuildingAllowed()) {
            return player.hasPermission("buildsystem.bypass.permission.archive");
        }

        return worldData.get(WorldDataKey.VISIBILITY).isPrivate()
                ? player.hasPermission("buildsystem.bypass.permission.private")
                : player.hasPermission("buildsystem.bypass.permission.public");
    }

    @Override
    public boolean canBypassBuildRestriction(Player player) {
        return context.playerService().isInBuildMode(player);
    }
}
