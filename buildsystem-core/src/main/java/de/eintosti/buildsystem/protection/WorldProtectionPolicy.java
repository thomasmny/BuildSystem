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
import de.eintosti.buildsystem.api.world.access.WorldSetting;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * Answers "may this player modify this world right now?" in one place so every listener and integration checks it the
 * same way. Composes three independent restrictions: the world's {@link BuildWorldStatus status} disallowing building,
 * the builders feature, and the per-action {@link WorldSetting settings} — each short-circuited by the build bypass.
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
     * Checks whether the world's current status permits building. Any status whose
     * {@link BuildWorldStatus#isBuildingAllowed() building-allowed} flag is off locks the world, not just the built-in
     * archive; the bypass permission node stays {@code buildsystem.bypass.archive} for backwards compatibility.
     *
     * @param player The player attempting to build
     * @param world The world being modified
     * @return {@link Denial#STATUS_LOCKED} when the status forbids building, otherwise {@link Denial#NONE}
     */
    public Denial checkStatus(Player player, BuildWorld world) {
        if (world.getPermissions().canBypassBuildRestriction(player)
                || player.hasPermission("buildsystem.bypass.archive")) {
            return Denial.NONE;
        }

        if (!world.getData().get(WorldDataKey.STATUS).isBuildingAllowed()) {
            return Denial.STATUS_LOCKED;
        }

        return Denial.NONE;
    }

    /**
     * Checks whether the builders feature blocks the player. When it is enabled, only the creator and registered
     * builders may modify the world.
     *
     * @param player The player attempting to build
     * @param world The world being modified
     * @return {@link Denial#NOT_A_BUILDER} when the player is not allowed, otherwise {@link Denial#NONE}
     */
    public Denial checkBuilders(Player player, BuildWorld world) {
        if (world.getPermissions().canBypassBuildRestriction(player)
                || player.hasPermission("buildsystem.bypass.builders")) {
            return Denial.NONE;
        }

        Builders builders = world.getBuilders();
        if (builders.isCreator(player)) {
            return Denial.NONE;
        }

        if (world.getData().get(WorldDataKey.BUILDERS_ENABLED) && !builders.isBuilder(player)) {
            return Denial.NOT_A_BUILDER;
        }

        return Denial.NONE;
    }

    /**
     * Checks whether a per-action {@link WorldSetting} (e.g. block placement) is enabled for the world.
     *
     * @param player The player attempting the action
     * @param world The world being modified
     * @param setting The setting governing the action
     * @return {@link Denial#SETTING_DISABLED} when the setting is off, otherwise {@link Denial#NONE}
     */
    public Denial checkSetting(Player player, BuildWorld world, WorldSetting setting) {
        if (world.getPermissions().canBypassBuildRestriction(player)) {
            return Denial.NONE;
        }

        if (!setting.isEnabled(world.getData())) {
            return Denial.SETTING_DISABLED;
        }

        return Denial.NONE;
    }

    /**
     * Runs the full modification check (status, then builders), returning the first {@link Denial} that applies.
     *
     * @param player The player attempting to build
     * @param world The world being modified
     * @return The first applicable denial, or {@link Denial#NONE} when the modification is allowed
     */
    public Denial mayModify(Player player, BuildWorld world) {
        if (world.getPermissions().canBypassBuildRestriction(player)) {
            return Denial.NONE;
        }

        Denial status = checkStatus(player, world);
        if (status != Denial.NONE) {
            return status;
        }

        return checkBuilders(player, world);
    }

    /**
     * Runs the full modification check for a setting-gated action (status, then the setting, then builders), returning
     * the first {@link Denial} that applies.
     *
     * @param player The player attempting the action
     * @param world The world being modified
     * @param setting The setting governing the action
     * @return The first applicable denial, or {@link Denial#NONE} when the modification is allowed
     */
    public Denial mayModify(Player player, BuildWorld world, WorldSetting setting) {
        if (world.getPermissions().canBypassBuildRestriction(player)) {
            return Denial.NONE;
        }

        Denial status = checkStatus(player, world);
        if (status != Denial.NONE) {
            return status;
        }

        Denial settingDenial = checkSetting(player, world, setting);
        if (settingDenial != Denial.NONE) {
            return settingDenial;
        }

        return checkBuilders(player, world);
    }
}
