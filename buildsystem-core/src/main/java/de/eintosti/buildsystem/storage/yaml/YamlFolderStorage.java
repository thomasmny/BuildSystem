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

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.api.world.display.NavigatorCategoryRegistry;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class YamlFolderStorage extends FolderStorageImpl {

    private static final String FOLDERS_KEY = "folders";

    private final BuildSystemPlugin plugin;
    private final YamlStore store;
    private final FileConfiguration config;

    public YamlFolderStorage(BuildSystemPlugin plugin, WorldStorage worldStorage) {
        super(plugin.getLogger(), worldStorage);
        this.plugin = plugin;
        this.store = new YamlStore(plugin.getDataFolder(), "folders.yml", plugin.getLogger());
        this.config = store.config();
    }

    @Override
    protected Folder newFolder(String name, NavigatorCategory category, @Nullable Folder parent, Builder creator) {
        return new FolderImpl(plugin, name, category, parent, creator);
    }

    @Override
    public CompletableFuture<Void> save(Folder folder) {
        return CompletableFuture.runAsync(() ->
                store.atomicSave(() -> config.set(FOLDERS_KEY + "." + folder.getName(), serializeFolder(folder))));
    }

    @Override
    public CompletableFuture<Void> save(Collection<Folder> folders) {
        return CompletableFuture.runAsync(() -> store.atomicSave(() ->
                folders.forEach(folder -> config.set(FOLDERS_KEY + "." + folder.getName(), serializeFolder(folder)))));
    }

    public Map<String, @Nullable Object> serializeFolder(Folder folder) {
        Map<String, @Nullable Object> serializedFolder = new HashMap<>();

        serializedFolder.put("uuid", folder.getUniqueId().toString());
        serializedFolder.put("creator", folder.getCreator().toString());
        serializedFolder.put("creation", folder.getCreation());
        serializedFolder.put("category", folder.getCategory().getId());
        serializedFolder.put("parent", folder.hasParent() ? folder.getParent().getName() : null);
        serializedFolder.put("material", folder.getIcon().name());
        serializedFolder.put("icon-skull-texture", folder.getIconSkullTexture());
        serializedFolder.put("permission", folder.getPermission());
        serializedFolder.put("project", folder.getProject());
        serializedFolder.put(
                "worlds", folder.getWorldUUIDs().stream().map(UUID::toString).toList());

        return serializedFolder;
    }

    @Override
    @Contract("-> new")
    public CompletableFuture<Collection<Folder>> load() {
        return CompletableFuture.supplyAsync(() -> store.locked(() -> {
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
        }));
    }

    private Set<String> loadFolderKeys() {
        if (!store.reload()) {
            return Set.of();
        }

        ConfigurationSection section = config.getConfigurationSection(FOLDERS_KEY);
        if (section == null) {
            return Set.of();
        }

        return section.getKeys(false);
    }

    @Contract("_ -> new")
    private Folder loadFolder(String folderName) {
        final String path = FOLDERS_KEY + "." + folderName;

        UUID uuid =
                config.isString(path + ".uuid") ? UUID.fromString(config.getString(path + ".uuid")) : UUID.randomUUID();
        Builder creator = Objects.requireNonNull(
                Builder.deserialize(config.getString(path + ".creator")),
                "Creator cannot be null for folder: " + folderName);
        long creation = config.getLong(path + ".creation", System.currentTimeMillis());
        NavigatorCategory category = resolveCategory(path);
        XMaterial defaultMaterial = XMaterial.CHEST;
        XMaterial material = XMaterial.matchXMaterial(config.getString(path + ".material", defaultMaterial.name()))
                .orElse(defaultMaterial);
        String permission = config.getString(path + ".permission", "-");
        String project = config.getString(path + ".project", "-");
        List<UUID> worlds = config.getStringList(path + ".worlds").stream()
                .map(UUID::fromString)
                .toList();

        FolderImpl folder = new FolderImpl(
                plugin,
                uuid,
                folderName,
                creation,
                category,
                null, // Parent will be set in second pass
                creator,
                material,
                permission,
                project,
                worlds,
                new ArrayList<>());
        folder.setIconSkullTexture(config.getString(path + ".icon-skull-texture"));
        return folder;
    }

    /**
     * Resolves a folder's {@link NavigatorCategory} from its stored {@code category} id. Pre-4.0 stored this as an
     * upper-case enum name ({@code PUBLIC}/{@code ARCHIVE}/{@code PRIVATE}), which lower-casing normalises to the
     * built-in category id. Falls back to the default category when the key is missing or unknown.
     */
    private NavigatorCategory resolveCategory(String path) {
        NavigatorCategoryRegistry registry = plugin.getNavigatorCategoryRegistry();
        String categoryId = config.getString(path + ".category");
        categoryId = categoryId != null ? categoryId.toLowerCase(Locale.ROOT) : null;
        return categoryId != null
                ? registry.getCategory(categoryId).orElseGet(registry::getDefaultCategory)
                : registry.getDefaultCategory();
    }

    @Override
    public CompletableFuture<Void> delete(Folder folder) {
        return delete(folder.getName());
    }

    @Override
    public CompletableFuture<Void> delete(String folderKey) {
        return CompletableFuture.runAsync(
                () -> store.atomicSave(() -> config.set(FOLDERS_KEY + "." + folderKey, null)));
    }
}
