/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * @author einTosti
 * @since 2.21.0
 */
public class LogoutLocation {

    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;

    public LogoutLocation(String worldName, Location location) {
        this.worldName = worldName;
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public LogoutLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getWorldName() {
        return worldName;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return worldName + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + pitch;
    }
}