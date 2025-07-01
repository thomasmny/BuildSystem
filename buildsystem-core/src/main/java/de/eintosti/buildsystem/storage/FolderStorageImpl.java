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
package de.eintosti.buildsystem.storage;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.FolderStorage;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.api.world.display.NavigatorCategory;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.display.FolderImpl;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class FolderStorageImpl implements FolderStorage {

    protected final Logger logger;
    protected final BuildSystemPlugin plugin;
    protected final WorldServiceImpl worldService;

    private final Map<String, Folder> foldersByName;

    public FolderStorageImpl(BuildSystemPlugin plugin, WorldServiceImpl worldService) {
        this.logger = plugin.getLogger();
        this.plugin = plugin;
        this.worldService = worldService;

        this.foldersByName = new HashMap<>();
    }

    public void loadFolders() {
        try {
            this.foldersByName.putAll(
                    load().get().stream().collect(Collectors.toMap(Folder::getName, Function.identity()))
            );
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Failed to load folders from storage: " + e.getMessage());
            return;
        }
    }

    @Override
    @Unmodifiable
    public Collection<Folder> getFolders() {
        return Collections.unmodifiableCollection(foldersByName.values());
    }

    @Override
    public Folder createFolder(String folderName, NavigatorCategory category, Builder creator) {
        return createFolder(folderName, category, null, creator);
    }

    @Override
    public Folder createFolder(String folderName, NavigatorCategory category, @Nullable Folder parent, Builder creator) {
        Folder folder = new FolderImpl(folderName, category, parent, creator);
        foldersByName.put(folderName, folder);
        return folder;
    }

    @Override
    public void removeFolder(String folderName) {
        Folder removed = foldersByName.remove(folderName);
        if (removed == null) {
            return;
        }

        WorldStorage worldStorage = plugin.getWorldService().getWorldStorage();
        removed.getWorldUUIDs()
                .stream()
                .map(worldStorage::getBuildWorld)
                .filter(Objects::nonNull)
                .forEach(buildWorld -> buildWorld.setFolder(null));
    }

    @Override
    public void removeFolder(Folder folder) {
        removeFolder(folder.getName());
    }

    @Override
    public boolean folderExists(String folderName) {
        return folderExists(folderName, false);
    }

    @Override
    public boolean folderExists(String folderName, boolean caseSensitive) {
        if (caseSensitive) {
            return foldersByName.containsKey(folderName);
        } else {
            return foldersByName.keySet().stream().anyMatch(name -> name.equalsIgnoreCase(folderName));
        }
    }

    @Nullable
    @Override
    public Folder getFolder(String folderName) {
        return getFolder(folderName, false);
    }

    @Nullable
    @Override
    public Folder getFolder(String folderName, boolean caseSensitive) {
        if (caseSensitive) {
            return foldersByName.get(folderName);
        } else {
            return foldersByName.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(folderName))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
    }
}
