/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.storage.yaml;

import de.eintosti.buildsystem.BuildSystemPlugin;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractYamlStorage {

    private final File file;
    private final FileConfiguration configuration;
    private final Logger logger;

    public AbstractYamlStorage(BuildSystemPlugin plugin, String fileName) {
        this.file = new File(plugin.getDataFolder(), fileName);
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.logger = plugin.getLogger();
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
            logger.log(Level.SEVERE, "Failed to load configuration file: " + file.getName(), e);
        }
    }

    public void saveFile() {
        try {
            configuration.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save configuration file: " + file.getName(), e);
        }
    }

    @Nullable
    public FileConfiguration getFile() {
        return configuration;
    }
}