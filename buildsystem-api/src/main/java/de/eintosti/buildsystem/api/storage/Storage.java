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
package de.eintosti.buildsystem.api.storage;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;

/**
 * A generic, backend-agnostic interface for asynchronous persistence operations.
 *
 * <p>All operations are performed off the main server thread and report their result through a {@link CompletableFuture}.
 * If an operation fails, the returned future completes exceptionally with the underlying cause.
 *
 * @param <T> The type of objects to be stored
 * @apiNote The returned futures complete on a background thread. Bukkit's API is not thread-safe, so any continuation
 *     that touches the server (worlds, entities, scheduler, etc.) must first hop back onto the main thread, e.g. via
 *     {@code thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> ...))}.
 * @since 3.0.0
 */
@NullMarked
public interface Storage<T> {

    /**
     * Saves the given object to the storage, overwriting any existing entry with the same key.
     *
     * @param object The object to save
     * @return A {@link CompletableFuture} that completes when the object has been persisted, or completes exceptionally
     *     if the operation fails
     */
    CompletableFuture<Void> save(T object);

    /**
     * Saves all the given objects to the storage, overwriting any existing entries with the same keys.
     *
     * @param objects The objects to save
     * @return A {@link CompletableFuture} that completes when every object has been persisted, or completes exceptionally
     *     if the operation fails
     */
    CompletableFuture<Void> save(Collection<T> objects);

    /**
     * Loads all objects currently held in the storage.
     *
     * @return A {@link CompletableFuture} that completes with all stored objects, or with an empty collection if the
     *     storage holds none; completes exceptionally if the operation fails
     */
    CompletableFuture<Collection<T>> load();

    /**
     * Deletes the given object from the storage. Completes normally even if no matching entry exists.
     *
     * @param object The object to delete
     * @return A {@link CompletableFuture} that completes when the deletion finishes, or completes exceptionally if the
     *     operation fails
     */
    CompletableFuture<Void> delete(T object);

    /**
     * Deletes the object stored under the given key. Completes normally even if no matching entry exists.
     *
     * @param key The key of the object to delete
     * @return A {@link CompletableFuture} that completes when the deletion finishes, or completes exceptionally if the
     *     operation fails
     */
    CompletableFuture<Void> delete(String key);
}
