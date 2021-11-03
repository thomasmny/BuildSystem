/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.util.config;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.object.settings.Settings;

import java.util.UUID;

/**
 * @author einTosti
 */
public class SettingsConfig extends ConfigurationFile {

    public SettingsConfig(BuildSystem plugin) {
        super(plugin, "settings.yml");
    }

    public void saveSettings(UUID uuid, Settings settings) {
        getFile().set("settings." + uuid.toString(), settings.serialize());
        saveFile();
    }
}
