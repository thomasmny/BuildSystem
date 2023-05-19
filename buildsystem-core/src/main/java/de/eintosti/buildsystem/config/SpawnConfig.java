/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.config;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.Location;

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