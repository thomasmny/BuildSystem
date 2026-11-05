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

import org.jspecify.annotations.NullMarked;

/**
 * A connection to a database server or file that holds one {@link EntityStore} per collection.
 */
@NullMarked
public interface Database extends AutoCloseable {

    /**
     * {@return the store for {@code kind}, created if it does not exist yet}
     */
    EntityStore collection(EntityKind kind);

    /**
     * {@return whether the table or collection for {@code kind} exists, which it does once it was opened}
     */
    boolean exists(EntityKind kind);

    /**
     * Removes the table or collection for {@code kind} with everything in it.
     */
    void drop(EntityKind kind);

    /**
     * {@return whether the table or collection that records the format of the data exists}
     */
    boolean hasMetadata();

    /**
     * {@return the store that records the format of the data, created if it does not exist yet}
     */
    EntityStore metadata();

    /**
     * Brings tables or collections written in format {@code from} up to {@link StorageBackend#FORMAT_VERSION}. An entry
     * in an older shape needs nothing here, since the codecs read every older shape.
     *
     * @return Whether everything was upgraded. If not, the format stays as it was, so the next start tries again.
     */
    default boolean upgradeFormat(int from) {
        return true;
    }

    /**
     * Creates or replaces the views that outside tools read, over the tables that exist.
     */
    default void createViews() {}

    @Override
    void close();
}
