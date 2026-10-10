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

import java.util.Map;
import java.util.SequencedMap;
import org.jspecify.annotations.NullMarked;

/**
 * Where the entries of one {@link EntityCollection} are kept: a YAML file, a table or a Mongo collection. An entry is a
 * key and the map its codec serialized, whose values are strings, numbers, booleans, lists and nested maps.
 *
 * <p>Every method blocks, and the collection calls them off the main thread, one write at a time.
 */
@NullMarked
public interface EntityStore {

    /**
     * {@return every stored entry by key, in an order that is the same on every call} An entry the store cannot read is
     * logged and left out, but stays stored.
     */
    SequencedMap<String, Map<String, Object>> loadAll();

    /**
     * {@return whether nothing is stored}
     */
    default boolean isEmpty() {
        return loadAll().isEmpty();
    }

    /**
     * Inserts or replaces the given entries, all of them or none.
     */
    void upsert(Map<String, Map<String, Object>> entries);

    void delete(String key);
}
