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
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.codec.FolderCodec;
import de.eintosti.buildsystem.storage.migration.StorageMigration;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class YamlFolderStorage extends FolderStorageImpl {

    private static final String FOLDERS_KEY = "folders";
    private static final int LEGACY_VERSION = 1;

    private final BuildSystemPlugin plugin;
    private final YamlStore store;
    private final FileConfiguration config;
    private final FolderCodec codec;

    public YamlFolderStorage(BuildSystemPlugin plugin, WorldStorage worldStorage) {
        super(plugin.getLogger(), worldStorage);
        this.plugin = plugin;
        this.store = new YamlStore(plugin.getDataFolder(), "folders.yml", plugin.getLogger());
        this.config = store.config();
        this.codec = new FolderCodec(WorldContext.fromPlugin(plugin), plugin.getNavigatorCategoryRegistry());
    }

    @Override
    protected Folder newFolder(String name, NavigatorCategory category, @Nullable Folder parent, Builder creator) {
        return new FolderImpl(WorldContext.fromPlugin(plugin), name, category, parent, creator);
    }

    @Override
    public CompletableFuture<Void> save(Folder folder) {
        return CompletableFuture.runAsync(() -> store.atomicSave(() -> {
            config.set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
            config.set(FOLDERS_KEY + "." + codec.key(folder), codec.serialize(folder));
        }));
    }

    @Override
    public CompletableFuture<Void> save(Collection<Folder> folders) {
        return CompletableFuture.runAsync(() -> store.atomicSave(() -> {
            config.set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
            folders.forEach(folder -> config.set(FOLDERS_KEY + "." + codec.key(folder), codec.serialize(folder)));
        }));
    }

    @Override
    @Contract("-> new")
    public CompletableFuture<Collection<Folder>> load() {
        return CompletableFuture.supplyAsync(() -> store.locked(() -> {
            Set<String> folderKeys = loadFolderKeys();

            // First pass: load each folder by its key (UUID) without its parent reference.
            Map<String, Folder> loadedByKey = new HashMap<>();
            for (String folderKey : folderKeys) {
                ConfigurationSection section = config.getConfigurationSection(FOLDERS_KEY + "." + folderKey);
                if (section == null) {
                    continue;
                }
                try {
                    loadedByKey.put(folderKey, codec.deserialize(folderKey, section));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Skipping folder \"" + folderKey + "\": could not be loaded", e);
                }
            }

            // Second pass: link parents (referenced by UUID) now that every folder exists.
            for (Map.Entry<String, Folder> entry : loadedByKey.entrySet()) {
                ConfigurationSection section = config.getConfigurationSection(FOLDERS_KEY + "." + entry.getKey());
                if (section == null) {
                    continue;
                }
                String parentKey = codec.parentReference(section);
                if (parentKey != null) {
                    Folder parent = loadedByKey.get(parentKey);
                    if (parent != null) {
                        entry.getValue().setParent(parent);
                    }
                }
            }

            return new ArrayList<>(loadedByKey.values());
        }));
    }

    private Set<String> loadFolderKeys() {
        if (!store.reload()) {
            return Set.of();
        }
        migrateIfNeeded();

        ConfigurationSection section = config.getConfigurationSection(FOLDERS_KEY);
        if (section == null) {
            return Set.of();
        }

        return section.getKeys(false);
    }

    /** Brings a legacy (name-keyed) {@code folders.yml} up to the current UUID-keyed format, once, before parsing. */
    private void migrateIfNeeded() {
        if (config.getInt(StorageMigration.VERSION_KEY, LEGACY_VERSION) >= StorageMigration.CURRENT_VERSION) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection(FOLDERS_KEY);
        if (section != null && !section.getKeys(false).isEmpty()) {
            store.backupOnce(StorageMigration.BACKUP_SUFFIX);
            StorageMigration.migrateFolders(config, logger);
        }
        config.set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
        store.save();
    }

    @Override
    public CompletableFuture<Void> delete(Folder folder) {
        return delete(folder.getUniqueId().toString());
    }

    @Override
    public CompletableFuture<Void> delete(String folderKey) {
        return CompletableFuture.runAsync(
                () -> store.atomicSave(() -> config.set(FOLDERS_KEY + "." + folderKey, null)));
    }
}
