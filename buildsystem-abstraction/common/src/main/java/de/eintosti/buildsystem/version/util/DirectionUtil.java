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
package de.eintosti.buildsystem.version.util;

import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class DirectionUtil {

    public static final BlockFace[] BLOCK_SIDES = new BlockFace[]{
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    public static BlockFace getPlayerDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }

        yaw %= 360;
        int i = (int) ((yaw + 8) / 22.5);
        return switch (i) {
            case 1 -> BlockFace.SOUTH_SOUTH_WEST;
            case 2 -> BlockFace.SOUTH_WEST;
            case 3 -> BlockFace.WEST_SOUTH_WEST;
            case 4 -> BlockFace.WEST;
            case 5 -> BlockFace.WEST_NORTH_WEST;
            case 6 -> BlockFace.NORTH_WEST;
            case 7 -> BlockFace.NORTH_NORTH_WEST;
            case 8 -> BlockFace.NORTH;
            case 9 -> BlockFace.NORTH_NORTH_EAST;
            case 10 -> BlockFace.NORTH_EAST;
            case 11 -> BlockFace.EAST_NORTH_EAST;
            case 12 -> BlockFace.EAST;
            case 13 -> BlockFace.EAST_SOUTH_EAST;
            case 14 -> BlockFace.SOUTH_EAST;
            case 15 -> BlockFace.SOUTH_SOUTH_EAST;
            default -> BlockFace.SOUTH;
        };
    }

    /**
     * Gets the direction the block should be facing.
     *
     * @param player           The player placing the block
     * @param allowNonCardinal Should the block be allowed to face {@link BlockFace#UP} and {@link BlockFace#DOWN}
     * @return The direction the block should be facing
     */
    public static BlockFace getBlockDirection(Player player, boolean allowNonCardinal) {
        if (!allowNonCardinal) {
            return getCardinalDirection(player);
        }

        float pitch = player.getLocation().getPitch();
        if (pitch <= -45) {
            return BlockFace.DOWN;
        } else if (pitch >= 45) {
            return BlockFace.UP;
        } else {
            return getCardinalDirection(player);
        }
    }

    public static BlockFace getCardinalDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        yaw %= 360;

        int i = (int) ((yaw + 8) / 22.5);
        return switch (i) {
            case 3, 4, 5, 6 -> BlockFace.EAST;
            case 7, 8, 9, 10 -> BlockFace.SOUTH;
            case 11, 12, 13, 14 -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };

    }
}