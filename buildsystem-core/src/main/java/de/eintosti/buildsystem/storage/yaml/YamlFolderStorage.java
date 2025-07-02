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
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.display.FolderImpl;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class YamlFolderStorage extends FolderStorageImpl {

    private static final String FOLDERS_KEY = "folders";

    private final File file;
    private final FileConfiguration config;

    public YamlFolderStorage(BuildSystemPlugin plugin, WorldServiceImpl worldService) {
        super(plugin, worldService);
        this.file = new File(plugin.getDataFolder(), "folders.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public CompletableFuture<Void> save(Folder folder) {
        return CompletableFuture.runAsync(() -> {
            config.set(FOLDERS_KEY + "." + folder.getName(), serializeFolder(folder));
            saveFile();
        });
    }

    @Override
    public CompletableFuture<Void> save(Collection<Folder> folders) {
        return CompletableFuture.runAsync(() -> {
            folders.forEach(folder -> config.set(FOLDERS_KEY + "." + folder.getName(), serializeFolder(folder)));
            saveFile();
        });
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save folders.yml file", e);
        }
    }

    public Map<String, @Nullable Object> serializeFolder(Folder folder) {
        Map<String, @Nullable Object> serializedFolder = new HashMap<>();

        serializedFolder.put("creator", folder.getCreator().toString());
        serializedFolder.put("creation", folder.getCreation());
        serializedFolder.put("category", folder.getCategory().name());
        serializedFolder.put("parent", folder.hasParent() ? folder.getParent().getName() : null);
        serializedFolder.put("material", folder.getIcon().name());
        serializedFolder.put("permission", folder.getPermission());
        serializedFolder.put("project", folder.getProject());
        serializedFolder.put("worlds", folder.getWorldUUIDs().stream().map(UUID::toString).toList());

        return serializedFolder;
    }

    @Override
    public CompletableFuture<Collection<Folder>> load() {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> folders = loadFolderKeys();

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
        });
    }

    private Set<String> loadFolderKeys() {
        if (!file.exists()) {
            config.options().copyDefaults(true);
            saveFile();
            return Set.of();
        }

        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            logger.log(Level.SEVERE, "Could not load folders.yml file", e);
        }

        ConfigurationSection section = config.getConfigurationSection(FOLDERS_KEY);
        if (section == null) {
            return Set.of();
        }

        return section.getKeys(false);
    }

    private Folder loadFolder(String folderName) {
        final String path = FOLDERS_KEY + "." + folderName;

        Builder creator = Objects.requireNonNull(Builder.deserialize(config.getString(path + ".creator")), "Creator cannot be null for folder: " + folderName);
        long creation = config.getLong(path + ".creation", System.currentTimeMillis());
        NavigatorCategory category = NavigatorCategory.valueOf(config.getString(path + ".category"));
        XMaterial defaultMaterial = XMaterial.CHEST;
        XMaterial material = XMaterial.matchXMaterial(config.getString(path + ".material", defaultMaterial.name())).orElse(defaultMaterial);
        String permission = config.getString(path + ".permission", "-");
        String project = config.getString(path + ".project", "-");
        List<UUID> worlds = config.getStringList(path + ".worlds").stream().map(UUID::fromString).collect(Collectors.toList());

        return new FolderImpl(
                folderName,
                creation,
                category,
                null, // Parent will be set in second pass
                creator,
                material,
                permission,
                project,
                worlds);
    }

    @Override
    public CompletableFuture<Void> delete(Folder folder) {
        return delete(folder.getName());
    }

    @Override
    public CompletableFuture<Void> delete(String folderKey) {
        return CompletableFuture.runAsync(() -> {
            config.set(FOLDERS_KEY + "." + folderKey, null);
            saveFile();
        });
    }
}