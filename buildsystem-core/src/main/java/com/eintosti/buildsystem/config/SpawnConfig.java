/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.config;

import com.eintosti.buildsystem.BuildSystem;
import org.bukkit.Location;

/**
 * @author einTosti
 */
public class SpawnConfig extends ConfigurationFile {

    public SpawnConfig(BuildSystem plugin) {
        super(plugin, "spawn.yml");
    }

    public void saveSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        getFile().set("spawn", location.getWorld().getName() + ":"
                + location.getX() + ":"
                + location.getY() + ":"
                + location.getZ() + ":"
                + location.getYaw() + ":"
                + location.getPitch()
        );
        saveFile();
    }
}