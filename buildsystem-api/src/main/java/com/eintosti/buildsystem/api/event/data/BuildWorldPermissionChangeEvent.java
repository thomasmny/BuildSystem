/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.event.data;

import com.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class BuildWorldPermissionChangeEvent extends BuildWorldDataChangeEvent<String> {

    public BuildWorldPermissionChangeEvent(BuildWorld buildWorld, String oldPermission, String newPermission) {
        super(buildWorld, oldPermission, newPermission);
    }

    public String getOldPermission() {
        return getOldData();
    }

    public String getNewPermission() {
        return getNewData();
    }
}
