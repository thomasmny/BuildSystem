/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.version.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class DirectionUtil {

    public static BlockFace getDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        yaw %= 360;

        int i = (int) ((yaw + 8) / 22.5);
        switch (i) {
            case 15:
            case 0:
            case 1:
            case 2:
                return BlockFace.NORTH;
            case 3:
            case 4:
            case 5:
            case 6:
                return BlockFace.EAST;
            case 7:
            case 8:
            case 9:
            case 10:
                return BlockFace.SOUTH;
            case 11:
            case 12:
            case 13:
            case 14:
                return BlockFace.WEST;
        }

        return BlockFace.NORTH;
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
            return getDirection(player);
        }

        float pitch = player.getLocation().getPitch();
        if (pitch <= -45) {
            return BlockFace.DOWN;
        } else if (pitch >= 45) {
            return BlockFace.UP;
        } else {
            return getDirection(player);
        }
    }

    public static boolean isTop(Player player, Block block) {
        Location location = player.getEyeLocation().clone();
        while ((!location.getBlock().equals(block)) && location.distance(player.getEyeLocation()) < 6) {
            location.add(player.getLocation().getDirection().multiply(0.06));
        }
        return location.getY() % 1 > 0.5;
    }
}