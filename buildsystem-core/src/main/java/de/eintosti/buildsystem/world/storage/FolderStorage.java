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
package de.eintosti.buildsystem.world.storage;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.display.Folder;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Unmodifiable;

public abstract class FolderStorage implements Storage<Folder> {

    protected final BuildSystem plugin;
    protected final Logger logger;

    private final Map<String, Folder> foldersByName;
    private final Map<String, String> worldToFolderMap;

    public FolderStorage(BuildSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        this.foldersByName = load().stream()
                .collect(Collectors.toMap(Folder::getName, Function.identity()));
        this.worldToFolderMap = this.foldersByName.values().stream()
                .flatMap(folder -> folder.getWorlds().stream()
                        .map(world -> new AbstractMap.SimpleEntry<>(world, folder.getName())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets a list of all {@link Folder}s.
     *
     * @return An unmodifiable list of all folders
     */
    @Unmodifiable
    public Collection<Folder> getFolders() {
        return Collections.unmodifiableCollection(foldersByName.values());
    }

    /**
     * Adds a {@link Folder} and updates the world-to-folder mapping for all worlds contained in the folder.
     *
     * @param folder the folder to add
     */
    public void addFolder(Folder folder) {
        foldersByName.put(folder.getName(), folder);
        for (String worldName : folder.getWorlds()) {
            worldToFolderMap.put(worldName, folder.getName());
        }
    }

    /**
     * Removes a folder and all of its worlds from the world-to-folder mapping.
     *
     * @param folderName the name of the folder to remove
     */
    public void removeFolder(String folderName) {
        Folder removed = foldersByName.remove(folderName);
        if (removed == null) {
            return;
        }

        for (String worldName : removed.getWorlds()) {
            worldToFolderMap.remove(worldName);
        }
    }

    /**
     * Checks if a {@link Folder} with the given name (case-insensitive) exists.
     *
     * @param folderName The name of the folder to check
     * @return {@code true} if the folder exists, {@code false} otherwise
     */
    public boolean folderExists(String folderName) {
        return folderExists(folderName, false);
    }

    /**
     * Checks if a {@link Folder} with the given name exists.
     *
     * @param folderName    The name of the folder to check
     * @param caseSensitive Whether to check the name case-sensitive or not
     * @return {@code true} if the folder exists, {@code false} otherwise
     */
    public boolean folderExists(String folderName, boolean caseSensitive) {
        if (caseSensitive) {
            return foldersByName.containsKey(folderName);
        } else {
            return foldersByName.keySet().stream().anyMatch(name -> name.equalsIgnoreCase(folderName));
        }
    }

    /**
     * Adds a {@link BuildWorld} to the specified {@link Folder} and updates the folder and world-to-folder mappings.
     *
     * @param worldName  the name of the world to add
     * @param folderName the name of the folder to add the world to
     */
    public void addWorldToFolder(String worldName, String folderName) {
        Folder folder = foldersByName.get(folderName);
        if (folder == null) {
            plugin.getLogger().warning(String.format("Attempting to add world %s to unknown folder %s", worldName, folderName));
            return;
        }

        folder.addWorld(worldName);
        worldToFolderMap.put(worldName, folderName);
    }

    /**
     * Removes a world from the specified {@link Folder} and updates the world-to-folder mapping.
     *
     * @param worldName  the name of the world to remove
     * @param folderName the name of the folder to remove the world from
     */
    public void removeWorldFromFolder(String worldName, String folderName) {
        Folder folder = foldersByName.get(folderName);
        if (folder == null) {
            plugin.getLogger().warning(String.format("Attempting to remove world %s from unknown folder %s", worldName, folderName));
            return;
        }

        folder.removeWorld(worldName);
        worldToFolderMap.remove(worldName);
    }

    /**
     * Checks whether the given {@link BuildWorld} is assigned to any {@link Folder}.
     *
     * @param buildWorld the build world to check
     * @return {@code true} if the world is in any folder; {@code false} otherwise
     */
    public boolean isWorldInAnyFolder(BuildWorld buildWorld) {
        return worldToFolderMap.containsKey(buildWorld.getName());
    }
}
