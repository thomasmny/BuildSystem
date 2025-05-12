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
package de.eintosti.buildsystem.world.storage.factory;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.world.storage.FolderStorage;
import de.eintosti.buildsystem.world.storage.yaml.YamlFolderStorage;

/**
 * Factory for creating folder storage implementations.
 */
public class FolderStorageFactory {

    private final BuildSystem plugin;

    public FolderStorageFactory(BuildSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a folder storage implementation based on the configured storage type.
     *
     * @return The folder storage implementation
     */
    public FolderStorage createStorage() {
        return new YamlFolderStorage(plugin);
    }
} 