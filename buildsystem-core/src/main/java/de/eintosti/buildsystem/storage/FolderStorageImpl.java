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

import de.eintosti.buildsystem.api.event.folder.FolderCreatedEvent;
import de.eintosti.buildsystem.api.event.folder.FolderDeletedEvent;
import de.eintosti.buildsystem.api.storage.FolderStorage;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class FolderStorageImpl implements FolderStorage {

    protected final Logger logger;
    protected final WorldStorage worldStorage;

    private final ConcurrentHashMap<String, Folder> foldersByName;

    protected FolderStorageImpl(Logger logger, WorldStorage worldStorage) {
        this.logger = logger;
        this.worldStorage = worldStorage;
        this.foldersByName = new ConcurrentHashMap<>();
    }

    public void loadFolders() {
        try {
            this.foldersByName.putAll(load().get().stream()
                    .collect(Collectors.toMap(folder -> folder.getName().toLowerCase(), Function.identity())));
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Failed to load folders from storage: " + e.getMessage());
        }
    }

    @Override
    @Unmodifiable
    public Collection<Folder> getFolders() {
        return Collections.unmodifiableCollection(foldersByName.values());
    }

    @Nullable @Override
    public Folder getFolder(String name) {
        return foldersByName.get(name.toLowerCase());
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
        Folder folder = newFolder(name, category, parent, creator);
        foldersByName.put(name.toLowerCase(), folder);
        fireEvent(new FolderCreatedEvent(folder));
        return folder;
    }

    /**
     * Creates the folder instance to register; implementations decide the concrete type and its dependencies.
     */
    protected abstract Folder newFolder(
            String name, NavigatorCategory category, @Nullable Folder parent, Builder creator);

    @Override
    public void removeFolder(String name) {
        Folder removed = foldersByName.remove(name.toLowerCase());
        if (removed == null) {
            return;
        }

        getFolders().stream()
                .filter(folder -> Objects.equals(folder.getParent(), removed))
                .forEach(this::removeFolder);

        removed.getWorldUUIDs().stream()
                .map(worldStorage::getBuildWorld)
                .filter(Objects::nonNull)
                .forEach(buildWorld -> buildWorld.setFolder(null));

        fireEvent(new FolderDeletedEvent(removed));
    }

    /**
     * Fires a Bukkit event for a folder lifecycle change. Overridable so unit tests can run without a Bukkit server.
     */
    protected void fireEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    @Override
    public void removeFolder(Folder folder) {
        removeFolder(folder.getName());
    }
}
