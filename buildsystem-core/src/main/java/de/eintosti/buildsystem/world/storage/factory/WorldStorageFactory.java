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
import de.eintosti.buildsystem.world.storage.WorldStorage;
import de.eintosti.buildsystem.world.storage.yaml.YamlWorldStorage;

/**
 * Factory for creating world storage implementations.
 */
public class WorldStorageFactory {

    private final BuildSystem plugin;

    public WorldStorageFactory(BuildSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a world storage implementation based on the configured storage type.
     *
     * @return The world storage implementation
     */
    public WorldStorage createStorage() {
        return new YamlWorldStorage(plugin);
    }
} 