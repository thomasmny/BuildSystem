/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.config;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public abstract class ConfigurationFile {

    private final File file;
    private final FileConfiguration configuration;

    public ConfigurationFile(BuildSystem plugin, String fileName) {
        this.file = new File(plugin.getDataFolder(), fileName);
        this.configuration = YamlConfiguration.loadConfiguration(file);
        loadFile();
    }

    public void loadFile() {
        if (!file.exists()) {
            configuration.options().copyDefaults(true);
            saveFile();
            return;
        }

        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void saveFile() {
        try {
            configuration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getFile() {
        return configuration;
    }
}