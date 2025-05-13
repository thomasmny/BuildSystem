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
import de.eintosti.buildsystem.world.display.Folder;
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

    private final Map<String, Folder> folders;

    public FolderStorage(BuildSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        this.folders = load().stream().collect(Collectors.toMap(Folder::getName, Function.identity()));
    }

    /**
     * Gets a list of all {@link Folder}s.
     *
     * @return An unmodifiable list of all folders
     */
    @Unmodifiable
    public Collection<Folder> getFolders() {
        return Collections.unmodifiableCollection(folders.values());
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
            return folders.containsKey(folderName);
        } else {
            return folders.keySet().stream().anyMatch(name -> name.equalsIgnoreCase(folderName));
        }
    }
}
