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
package de.eintosti.buildsystem.api.exception;

/**
 * Thrown when a world's directory is not found at the expected path.
 *
 * @since 3.0.0
 */
public class WorldDirectoryNotFoundException extends WorldException {

    /**
     * Constructs a new {@link WorldDirectoryNotFoundException} with the specified world name and path.
     *
     * @param worldName The name of the world
     * @param path      The path to the expected world directory
     */
    public WorldDirectoryNotFoundException(String worldName, String path) {
        super("World directory for '" + worldName + "' not found at: " + path);
    }
}
