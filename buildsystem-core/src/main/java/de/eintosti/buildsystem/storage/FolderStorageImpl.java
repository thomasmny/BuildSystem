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
package de.eintosti.buildsystem.storage;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.event.folder.FolderCreatedEvent;
import de.eintosti.buildsystem.api.event.folder.FolderDeletedEvent;
import de.eintosti.buildsystem.api.storage.FolderStorage;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.storage.codec.FolderCodec;
import de.eintosti.buildsystem.storage.migration.StorageMigration;
import de.eintosti.buildsystem.storage.yaml.YamlEntityFile;
import de.eintosti.buildsystem.storage.yaml.YamlStore;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.folder.FolderImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FolderStorageImpl implements FolderStorage {

    protected final Logger logger;
    protected final WorldStorage worldStorage;

    /**
     * Keyed by UUID, since a folder can be renamed through the API without the storage hearing about it.
     */
    private final ConcurrentHashMap<UUID, Folder> folders;

    private final YamlEntityFile<Folder> file;
    private final Supplier<WorldContext> context;

    public FolderStorageImpl(BuildSystemPlugin plugin, WorldStorage worldStorage, Services services) {
        this(
                plugin.getLogger(),
                worldStorage,
                services::worldContext,
                new YamlEntityFile<>(
                        new YamlStore(plugin.getDataFolder(), "folders.yml", plugin.getLogger()),
                        "folders",
                        "folder",
                        () -> new FolderCodec(services.worldContext(), services.navigatorCategoryRegistry()),
                        services.scheduler().background(),
                        plugin.getLogger(),
                        StorageMigration::migrateFolders));
    }

    FolderStorageImpl(
            Logger logger, WorldStorage worldStorage, Supplier<WorldContext> context, YamlEntityFile<Folder> file) {
        this.logger = logger;
        this.worldStorage = worldStorage;
        this.folders = new ConcurrentHashMap<>();
        this.context = context;
        this.file = file;
    }

    @Override
    public CompletableFuture<Void> save(Folder folder) {
        return file.save(folder);
    }

    @Override
    public CompletableFuture<Void> save(Collection<Folder> folders) {
        return file.save(folders);
    }

    /**
     * Loads every folder, then links each to its parent, which is stored by UUID.
     */
    @Override
    public CompletableFuture<Collection<Folder>> load() {
        return file.load((loaded, root) -> loaded.forEach((key, folder) -> {
                    ConfigurationSection section = root.getConfigurationSection(key);
                    String parentKey = section == null ? null : FolderCodec.parentReference(section);
                    Folder parent = parentKey == null ? null : loaded.get(parentKey);
                    if (parent != null) {
                        folder.setParent(parent);
                    }
                }))
                .thenApply(loaded -> new ArrayList<>(loaded.values()));
    }

    @Override
    public CompletableFuture<Void> delete(Folder folder) {
        return file.delete(folder);
    }

    @Override
    public CompletableFuture<Void> delete(String folderKey) {
        return file.delete(folderKey);
    }

    public void loadFolders() {
        try {
            this.folders.putAll(
                    load().get().stream().collect(Collectors.toMap(Folder::getUniqueId, Function.identity())));
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Failed to load folders from storage: " + e.getMessage());
        }
    }

    @Override
    @Unmodifiable
    public Collection<Folder> getFolders() {
        return Collections.unmodifiableCollection(folders.values());
    }

    @Nullable @Override
    public Folder getFolder(String name) {
        return folders.values().stream()
                .filter(folder -> folder.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean folderExists(String name) {
        return getFolder(name) != null;
    }

    @Override
    public Folder createFolder(String name, NavigatorCategory category, Builder creator) {
        return createFolder(name, category, null, creator);
    }

    @Override
    public Folder createFolder(String name, NavigatorCategory category, @Nullable Folder parent, Builder creator) {
        Folder folder = new FolderImpl(context.get(), name, category, parent, creator);
        folders.put(folder.getUniqueId(), folder);
        fireEvent(new FolderCreatedEvent(folder));
        return folder;
    }

    @Override
    public void removeFolder(String name) {
        Folder folder = getFolder(name);
        if (folder != null) {
            removeFolder(folder);
        }
    }

    @Override
    public void removeFolder(Folder removed) {
        if (folders.remove(removed.getUniqueId()) == null) {
            return;
        }

        getFolders().stream()
                .filter(folder -> Objects.equals(folder.getParent(), removed))
                .forEach(this::removeFolder);

        removed.getWorldUUIDs().stream()
                .map(worldStorage::getBuildWorld)
                .filter(Objects::nonNull)
                .forEach(buildWorld -> buildWorld.setFolder(null));

        delete(removed).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "Failed to delete folder \"" + removed.getName() + "\" from storage", throwable);
            return null;
        });
        fireEvent(new FolderDeletedEvent(removed));
    }

    /**
     * Fires a Bukkit event for a folder lifecycle change. Overridable so unit tests can run without a Bukkit server.
     */
    protected void fireEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }
}
