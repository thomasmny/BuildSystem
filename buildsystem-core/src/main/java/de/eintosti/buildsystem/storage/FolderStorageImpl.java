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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.world.display.FolderImpl;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Unmodifiable;

public abstract class FolderStorageImpl implements FolderStorage {

    protected final BuildSystemPlugin plugin;
    protected final Logger logger;

    private final Map<String, Folder> foldersByName;
    private final Map<UUID, String> worldToFolderMap;

    public FolderStorageImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        this.foldersByName = load().stream()
                .collect(Collectors.toMap(Folder::getName, Function.identity()));
        this.worldToFolderMap = this.foldersByName.values().stream()
                .flatMap(folder -> folder.getWorldUUIDs().stream()
                        .map(world -> new AbstractMap.SimpleEntry<>(world, folder.getName())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @Unmodifiable
    public Collection<Folder> getFolders() {
        return Collections.unmodifiableCollection(foldersByName.values());
    }

    @Override
    public Folder createFolder(String folderName) {
        Folder folder = new FolderImpl(this, folderName);
        foldersByName.put(folderName, folder);
        return folder;
    }

    @Override
    public void removeFolder(String folderName) {
        Folder removed = foldersByName.remove(folderName);
        if (removed == null) {
            return;
        }

        for (UUID worldUuid : removed.getWorldUUIDs()) {
            worldToFolderMap.remove(worldUuid);
        }
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

    @Override
    public Folder getFolder(String folderName) {
        return getFolder(folderName, false);
    }

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

    /**
     * Caches which {@link Folder} a {@link BuildWorld} is assigned to.
     *
     * @param buildWorld The world to assign
     * @param folderName The name of the folder
     */
    public void assignWorldToFolder(BuildWorld buildWorld, String folderName) {
        this.worldToFolderMap.put(buildWorld.getUniqueId(), folderName);
    }

    /**
     * Removes the mapping of a {@link BuildWorld} to its assigned {@link Folder}.
     *
     * @param buildWorld The world to unassign
     */
    public void unassignWorldToFolder(BuildWorld buildWorld) {
        this.worldToFolderMap.remove(buildWorld.getUniqueId());
    }

    @Override
    public boolean isAssignedToAnyFolder(BuildWorld buildWorld) {
        return worldToFolderMap.containsKey(buildWorld.getUniqueId());
    }

    @Override
    public Folder getAssignedFolder(BuildWorld buildWorld) {
        String folderName = worldToFolderMap.get(buildWorld.getUniqueId());
        return getFolder(folderName);
    }
}
