/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.util.config;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.object.world.WorldStatus;
import com.eintosti.buildsystem.object.world.WorldType;

/**
 * @author einTosti
 */
public class SetupConfig extends ConfigurationFile {

    public SetupConfig(BuildSystem plugin) {
        super(plugin, "setup.yml");
    }

    public void saveCreateItem(WorldType worldType, XMaterial material) {
        getFile().set("setup.type." + worldType.name().toLowerCase() + ".create", material.name());
        saveFile();
    }

    public void saveDefaultItem(WorldType worldType, XMaterial material) {
        getFile().set("setup.type." + worldType.name().toLowerCase() + ".default", material.name());
        saveFile();
    }

    public void saveStatusItem(WorldStatus worldStatus, XMaterial material) {
        getFile().set("setup.status." + worldStatus.name().toLowerCase(), material.name());
        saveFile();
    }
}
