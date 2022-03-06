/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.event.data;

import com.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.Material;

/**
 * @author einTosti
 */
public class BuildWorldMaterialChangeEvent extends BuildWorldDataChangeEvent<Material> {

    public BuildWorldMaterialChangeEvent(BuildWorld buildWorld, Material oldMaterial, Material newMaterial) {
        super(buildWorld, oldMaterial, newMaterial);
    }

    public Material getOldMaterial() {
        return getOldData();
    }

    public Material getNewMaterial() {
        return getNewData();
    }
}
