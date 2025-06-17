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
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.world.display.FolderImpl;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class YamlFolderStorage extends FolderStorageImpl {

    private static final String FOLDERS_KEY = "folders";

    private File file;
    private FileConfiguration config;

    public YamlFolderStorage(BuildSystemPlugin plugin) {
        super(plugin);
    }

    private void loadFile() {
        this.file = new File(plugin.getDataFolder(), "folders.yml");
        this.config = YamlConfiguration.loadConfiguration(file);

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

    private void saveFile() {
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

        serializedFolder.put("category", folder.getCategory().name());
        serializedFolder.put("parent", folder.hasParent() ? folder.getParent().getName() : null);
        serializedFolder.put("material", folder.getIcon().name());
        serializedFolder.put("worlds",
                folder.getWorldUUIDs().stream().map(UUID::toString).collect(Collectors.toList()));

        return serializedFolder;
    }

    @Override
    public Collection<Folder> load() {
        loadFile();

        ConfigurationSection section = config.getConfigurationSection(FOLDERS_KEY);
        if (section == null) {
            return new ArrayList<>();
        }

        Set<String> folders = section.getKeys(false);
        if (folders.isEmpty()) {
            return new ArrayList<>();
        }

        // First pass: Create all folders without parent references
        Map<String, Folder> loadedFolders = folders.stream()
                .map(this::loadFolder)
                .collect(Collectors.toMap(Folder::getName, Function.identity()));

        // Second pass: Set up parent references
        for (String folderName : folders) {
            String parentName = config.getString(FOLDERS_KEY + "." + folderName + ".parent");
            if (parentName != null) {
                Folder folder = loadedFolders.get(folderName);
                Folder parent = loadedFolders.get(parentName);
                if (folder != null && parent != null) {
                    folder.setParent(parent);
                }
            }
        }

        return new ArrayList<>(loadedFolders.values());
    }

    private Folder loadFolder(String folderName) {
        final String path = FOLDERS_KEY + "." + folderName;

        NavigatorCategory category = NavigatorCategory.valueOf(config.getString(path + ".category"));
        XMaterial defaultMaterial = XMaterial.CHEST;
        XMaterial material = XMaterial.matchXMaterial(config.getString(path + ".material", defaultMaterial.name())).orElse(defaultMaterial);
        List<UUID> worlds = config.getStringList(path + ".worlds").stream().map(UUID::fromString).collect(Collectors.toList());

        return new FolderImpl(
                this,
                folderName,
                category,
                null, // Parent will be set in second pass
                material,
                worlds);
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