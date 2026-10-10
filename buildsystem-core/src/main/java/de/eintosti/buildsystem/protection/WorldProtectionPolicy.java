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
package de.eintosti.buildsystem.protection;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.access.WorldPermissions;
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.util.Permissions;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Answers "may this player modify this world right now?" in one place so every listener and integration checks it the
 * same way. Composes three independent restrictions: the world's {@link BuildWorldStatus status} disallowing building,
 * the builders feature, and the per-action {@link WorldSetting settings}, each short-circuited by build mode and the
 * admin permission. {@link WorldPermissions#canModify} answers from here too.
 */
@NullMarked
public final class WorldProtectionPolicy {

    /**
     * Why a modification was denied, or {@link #NONE} when it is allowed.
     */
    public enum Denial {
        /**
         * The modification is allowed.
         */
        NONE,
        /**
         * The world's current status forbids building — an archived world, or any custom status whose
         * {@link BuildWorldStatus#isBuildingAllowed() building-allowed} flag is off.
         */
        STATUS_LOCKED,
        /**
         * The builders feature is on and the player is neither the creator nor a registered builder.
         */
        NOT_A_BUILDER,
        /**
         * A {@link WorldSetting} (block breaking/placement/interaction) is disabled for the world.
         */
        SETTING_DISABLED
    }

    /**
     * Runs the full modification check (status, then builders), returning the first {@link Denial} that applies.
     *
     * @param player The player attempting to build
     * @param world The world being modified
     * @return The first applicable denial, or {@link Denial#NONE} when the modification is allowed
     */
    public Denial mayModify(Player player, BuildWorld world) {
        if (isExempt(player, world)) {
            return Denial.NONE;
        }

        Denial status = checkStatus(player, world);
        return status != Denial.NONE ? status : checkBuilders(player, world);
    }

    /**
     * Runs the full modification check for a setting-gated action (status, then the setting, then builders), returning
     * the first {@link Denial} that applies. Holding the setting's {@link WorldSetting#getBypassPermission() bypass
     * permission} skips the setting and the builders check.
     *
     * @param player The player attempting the action
     * @param world The world being modified
     * @param setting The setting governing the action
     * @return The first applicable denial, or {@link Denial#NONE} when the modification is allowed
     */
    public Denial mayModify(Player player, BuildWorld world, WorldSetting setting) {
        if (isExempt(player, world)) {
            return Denial.NONE;
        }

        Denial status = checkStatus(player, world);
        if (status != Denial.NONE) {
            return status;
        }

        if (player.hasPermission(setting.getBypassPermission())) {
            return Denial.NONE;
        }

        if (!setting.isEnabled(world.getData())) {
            return Denial.SETTING_DISABLED;
        }

        return checkBuilders(player, world);
    }

    /**
     * Any status whose {@link BuildWorldStatus#isBuildingAllowed() building-allowed} flag is off locks the world, not
     * just the built-in archive. The bypass node stays {@code buildsystem.bypass.archive} for backwards compatibility.
     */
    private static Denial checkStatus(Player player, BuildWorld world) {
        if (player.hasPermission(Permissions.BYPASS_ARCHIVE)
                || world.getData().get(WorldDataKey.STATUS).isBuildingAllowed()) {
            return Denial.NONE;
        }
        return Denial.STATUS_LOCKED;
    }

    /**
     * When the builders feature is on, only the creator and registered builders may modify the world.
     */
    private static Denial checkBuilders(Player player, BuildWorld world) {
        if (player.hasPermission(Permissions.BYPASS_BUILDERS)) {
            return Denial.NONE;
        }

        Builders builders = world.getBuilders();
        if (builders.isCreator(player)
                || !world.getData().get(WorldDataKey.BUILDERS_ENABLED)
                || builders.isBuilder(player)) {
            return Denial.NONE;
        }
        return Denial.NOT_A_BUILDER;
    }

    private static boolean isExempt(Player player, BuildWorld world) {
        return world.getPermissions().canBypassBuildRestriction(player)
                || world.getPermissions().hasAdminPermission(player);
    }
}
