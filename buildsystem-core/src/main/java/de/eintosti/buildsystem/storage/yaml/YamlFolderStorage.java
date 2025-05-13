/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.world.display.FolderImpl;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class YamlFolderStorage extends FolderStorageImpl {

    private static final String FOLDERS_KEY = "folders";

    private final File file;
    private final FileConfiguration config;

    public YamlFolderStorage(BuildSystemPlugin plugin) {
        super(plugin);
        this.file = new File(plugin.getDataFolder(), "folders.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadFile();
    }

    public void loadFile() {
        if (!file.exists()) {
            config.options().copyDefaults(true);
            saveFile();
            return;
        }

        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.SEVERE, "Could not load folders.yml file", e);
        }
    }

    public void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save folders.yml file", e);
        }
    }

    @Override
    public void save(Folder folder) {
        config.set(FOLDERS_KEY + "." + folder.getName(), serializeFolder(folder));
        saveFile();
    }

    @Override
    public void save(Collection<Folder> folders) {
        folders.forEach(folder -> config.set(FOLDERS_KEY + "." + folder.getName(), serializeFolder(folder)));
        saveFile();
    }

    public @NotNull Map<String, Object> serializeFolder(Folder folder) {
        Map<String, Object> serializedFolder = new HashMap<>();

        serializedFolder.put("material", folder.getMaterial().name());
        serializedFolder.put("worlds", folder.getWorlds());

        return serializedFolder;
    }

    @Override
    public Collection<Folder> load() {
        ConfigurationSection section = config.getConfigurationSection(FOLDERS_KEY);
        if (section == null) {
            return new ArrayList<>();
        }

        Set<String> folders = section.getKeys(false);
        if (folders.isEmpty()) {
            return new ArrayList<>();
        }

        return folders.stream()
                .map(this::loadFolder)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Folder loadFolder(String folderName) {
        final String path = FOLDERS_KEY + "." + folderName;

        XMaterial defaultMaterial = XMaterial.CHEST;
        XMaterial material = XMaterial.matchXMaterial(config.getString(path + ".material", defaultMaterial.name())).orElse(defaultMaterial);
        List<String> worlds = config.getStringList(path + ".worlds");

        return new FolderImpl(
                folderName,
                material,
                worlds
        );
    }

    @Override
    public void delete(Folder folder) {
        delete(folder.getName());
    }

    @Override
    public void delete(String folderKey) {
        config.set(FOLDERS_KEY + "." + folderKey, null);
        saveFile();
    }
}