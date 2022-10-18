/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.player;

import com.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.Location;

/**
 * @author einTosti
 */
public class LogoutLocation {

    private final BuildWorld buildWorld;
    private final double x, y, z;
    private final float yaw, pitch;

    public LogoutLocation(BuildWorld buildWorld, Location location) {
        this.buildWorld = buildWorld;
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public LogoutLocation(BuildWorld buildWorld, double x, double y, double z, float yaw, float pitch) {
        this.buildWorld = buildWorld;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public BuildWorld getBuildWorld() {
        return buildWorld;
    }

    public Location getLocation() {
        return new Location(buildWorld.getWorld(), x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return buildWorld.getName() + ":" + x + ":" + y + ":" + z + ":" + yaw + ":" + pitch;
    }
}