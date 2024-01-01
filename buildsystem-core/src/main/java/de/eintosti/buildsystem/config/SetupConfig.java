/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.config;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.world.data.WorldStatus;
import de.eintosti.buildsystem.world.data.WorldType;

import java.util.Locale;

public class SetupConfig extends ConfigurationFile {

    public SetupConfig(BuildSystem plugin) {
        super(plugin, "setup.yml");
    }

    public void saveCreateItem(WorldType worldType, XMaterial material) {
        getFile().set("setup.type." + worldType.name().toLowerCase(Locale.ROOT) + ".create", material.name());
        saveFile();
    }

    public void saveDefaultItem(WorldType worldType, XMaterial material) {
        getFile().set("setup.type." + worldType.name().toLowerCase(Locale.ROOT) + ".default", material.name());
        saveFile();
    }

    public void saveStatusItem(WorldStatus worldStatus, XMaterial material) {
        getFile().set("setup.status." + worldStatus.name().toLowerCase(Locale.ROOT), material.name());
        saveFile();
    }
}