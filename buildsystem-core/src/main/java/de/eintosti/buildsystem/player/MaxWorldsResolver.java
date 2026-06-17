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
package de.eintosti.buildsystem.player;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.data.Visibility;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jspecify.annotations.NullMarked;

/**
 * Resolves how many worlds a player may create from their {@code buildsystem.create.<visibility>.<amount>} permission
 * nodes. {@code <amount>} is either a number (the highest one wins) or {@code *} for unlimited.
 */
@NullMarked
public final class MaxWorldsResolver {

    private static final int UNLIMITED = -1;

    private final Logger logger;

    public MaxWorldsResolver(Logger logger) {
        this.logger = logger;
    }

    /**
     * Gets the maximum amount of worlds with the given visibility the player may create.
     *
     * @param player The player to resolve the limit for
     * @param visibility The world visibility the limit applies to
     * @return The maximum amount, or {@code -1} for unlimited
     */
    public int getMaxWorlds(Player player, Visibility visibility) {
        int max = UNLIMITED;
        if (player.hasPermission(BuildSystemPlugin.ADMIN_PERMISSION)) {
            return max;
        }

        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String permissionString = permission.getPermission();
            String[] splitPermission = permissionString.split("\\.");

            if (splitPermission.length != 4) {
                continue;
            }

            if (!splitPermission[1].equalsIgnoreCase("create")) {
                continue;
            }

            if (!splitPermission[2].equalsIgnoreCase(visibility.getPermissionNode())) {
                continue;
            }

            String amountString = splitPermission[3];
            if (amountString.equals("*")) {
                return UNLIMITED;
            }

            try {
                int amount = Integer.parseInt(amountString);
                if (amount > max) {
                    max = amount;
                }
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Invalid max. world amount (must be int)", e);
            }
        }

        return max;
    }
}
