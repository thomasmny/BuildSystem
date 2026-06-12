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

import de.eintosti.buildsystem.api.data.Type;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class WorldProtectionPolicy {

    public enum Denial {
        NONE,
        ARCHIVED,
        NOT_A_BUILDER,
        SETTING_DISABLED
    }

    public Denial checkArchive(Player player, BuildWorld world) {
        if (world.getPermissions().canBypassBuildRestriction(player)
                || player.hasPermission("buildsystem.bypass.archive")) {
            return Denial.NONE;
        }
        if (world.getData().status().get() == BuildWorldStatus.ARCHIVE) {
            return Denial.ARCHIVED;
        }
        return Denial.NONE;
    }

    public Denial checkBuilders(Player player, BuildWorld world) {
        if (world.getPermissions().canBypassBuildRestriction(player)
                || player.hasPermission("buildsystem.bypass.builders")) {
            return Denial.NONE;
        }
        Builders builders = world.getBuilders();
        if (builders.isCreator(player)) {
            return Denial.NONE;
        }
        if (world.getData().buildersEnabled().get() && !builders.isBuilder(player)) {
            return Denial.NOT_A_BUILDER;
        }
        return Denial.NONE;
    }

    public Denial checkSetting(Player player, BuildWorld world, Type<Boolean> setting) {
        if (world.getPermissions().canBypassBuildRestriction(player)) {
            return Denial.NONE;
        }
        if (!setting.get()) {
            return Denial.SETTING_DISABLED;
        }
        return Denial.NONE;
    }

    public Denial mayModify(Player player, BuildWorld world) {
        if (world.getPermissions().canBypassBuildRestriction(player)) {
            return Denial.NONE;
        }
        Denial archive = checkArchive(player, world);
        if (archive != Denial.NONE) {
            return archive;
        }
        return checkBuilders(player, world);
    }

    public Denial mayModify(Player player, BuildWorld world, Type<Boolean> setting) {
        if (world.getPermissions().canBypassBuildRestriction(player)) {
            return Denial.NONE;
        }
        Denial archive = checkArchive(player, world);
        if (archive != Denial.NONE) {
            return archive;
        }
        Denial sett = checkSetting(player, world, setting);
        if (sett != Denial.NONE) {
            return sett;
        }
        return checkBuilders(player, world);
    }
}
