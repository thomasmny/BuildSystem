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

import java.util.Collection;

public interface Storage<T> {

    /**
     * Saves the given object to the storage.
     *
     * @param object The object to save
     */
    void save(T object);

    /**
     * Saves all the given objects to the storage.
     *
     * @param objects The objects to save
     */
    void save(Collection<T> objects);

    /**
     * Loads all objects from the storage.
     *
     * @return A collection of loaded objects
     */
    Collection<T> load();

    /**
     * Deletes the given object from the storage.
     *
     * @param object The object to delete
     */
    void delete(T object);


    /**
     * Deletes the object with the given key from the storage.
     *
     * @param key The key of the object to delete
     */
    void delete(String key);
}
