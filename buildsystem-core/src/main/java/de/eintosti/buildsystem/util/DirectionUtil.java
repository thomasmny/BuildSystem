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
package de.eintosti.buildsystem.util;

import org.bukkit.Axis;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.Sign;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DirectionUtil {

    public static final BlockFace[] BLOCK_SIDES = new BlockFace[] {
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
     * @param player The player placing the block
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

    /**
     * Orients a block towards the given face. Directional and sign blocks face it directly; orientable blocks (logs,
     * portals) snap to the matching {@link Axis}. Blocks that carry no orientation are left untouched.
     *
     * @param block The block to orient
     * @param direction The face (or axis) to orient towards
     */
    public static void rotateBlock(Block block, BlockFace direction) {
        switch (block.getBlockData()) {
            case Directional directional -> {
                directional.setFacing(direction);
                block.setBlockData(directional);
            }
            case Orientable orientable -> {
                Axis axis =
                        switch (direction) {
                            case UP, DOWN -> Axis.Y;
                            case EAST, WEST -> Axis.X;
                            default -> Axis.Z;
                        };
                orientable.setAxis(axis);
                block.setBlockData(orientable);
            }
            case Sign sign -> {
                sign.setRotation(direction);
                block.setBlockData(sign);
            }
            case HangingSign hangingSign -> {
                hangingSign.setRotation(direction);
                block.setBlockData(hangingSign);
            }
            default -> {}
        }
    }
}
