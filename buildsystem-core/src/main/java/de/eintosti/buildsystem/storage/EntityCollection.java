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

import de.eintosti.buildsystem.storage.codec.Codec;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The entities of one kind, mapped by their {@link Codec} onto an {@link EntityStore}, whichever backend that is.
 *
 * <p>Entities are serialized on the calling thread, where their state is owned, and only the captured maps are written
 * off it. Writes run one after another in the order they were submitted, so a save queued before a delete can never
 * land after it and bring the entry back.
 */
@NullMarked
public final class EntityCollection<T> {

    private volatile EntityStore store;
    private final String label;
    private final Supplier<Codec<T>> codecFactory;
    private final Executor background;
    private final Logger logger;

    private @Nullable Codec<T> codec;
    private CompletableFuture<Void> lastWrite = CompletableFuture.completedFuture(null);
    private volatile boolean loaded;
    private volatile boolean loadFailed;
    private volatile boolean refusalLogged;
    /**
     * The entries the last load could not parse, as they are stored. They are never written over, so the stored
     * version survives until it is fixed or deleted.
     */
    private final Map<String, Map<String, Object>> skipped = new ConcurrentHashMap<>();

    private final Set<String> refusalsLogged = ConcurrentHashMap.newKeySet();

    /**
     * @param label What one entity is called in log messages
     * @param codecFactory Builds the codec on first use: it may need services that only exist once the plugin has
     *     enabled
     */
    public EntityCollection(
            EntityStore store, String label, Supplier<Codec<T>> codecFactory, Executor background, Logger logger) {
        this.store = store;
        this.label = label;
        this.codecFactory = codecFactory;
        this.background = background;
        this.logger = logger;
    }

    private synchronized Codec<T> codec() {
        if (codec == null) {
            codec = codecFactory.get();
        }
        return codec;
    }

    /**
     * Writes one entity, logging a failure: the callers, such as a player joining, leaving or changing worlds, have
     * nobody to tell.
     */
    public CompletableFuture<Void> save(T value) {
        String key = codec().key(value);
        return save(List.of(value)).whenComplete((ignored, failure) -> {
            if (failure != null) {
                logger.log(Level.SEVERE, "Could not save " + label + " \"" + key + "\"", failure);
            }
        });
    }

    /**
     * Writes the given entities, leaving every other stored entry as it is. Entries that failed to load stay stored
     * this way, rather than being erased by the next save.
     */
    public CompletableFuture<Void> save(Collection<T> values) {
        if (refusedAfterFailedLoad()) {
            return CompletableFuture.completedFuture(null);
        }
        Map<String, Map<String, Object>> serialized = serialize(values);
        serialized.keySet().removeIf(this::skipped);
        return write(() -> store.upsert(serialized));
    }

    /**
     * Whether the last load failed. What is in memory may then be missing some or all of the stored entries, or hold
     * defaults in their place, and writing it would replace the stored ones.
     */
    private boolean refusedAfterFailedLoad() {
        if (!loadFailed) {
            return false;
        }
        if (!refusalLogged) {
            refusalLogged = true;
            logger.severe("Not saving the " + label + " entries, since they failed to load. Changes made meanwhile are"
                    + " not kept: fix the storage, then restart the server.");
        }
        return true;
    }

    private boolean skipped(String key) {
        if (!skipped.containsKey(key)) {
            return false;
        }
        if (refusalsLogged.add(key)) {
            logger.severe(
                    "Not saving " + label + " \"" + key + "\": it is stored but could not be loaded. Fix or delete"
                            + " the stored entry, then restart the server.");
        }
        return true;
    }

    /**
     * {@return the entities as they would be stored, by key} Must run where their state is owned, like {@link #save}.
     */
    public Map<String, Map<String, Object>> serialize(Collection<T> values) {
        Map<String, Map<String, Object>> serialized = new LinkedHashMap<>();
        for (T value : values) {
            serialized.put(codec().key(value), codec().serialize(value));
        }
        return serialized;
    }

    /**
     * Replaces every stored entry with {@code values}, keeping the entries that failed to parse as they are stored.
     * Refused until the stored entries have loaded, since what is in memory may then be missing some or all of them.
     */
    public CompletableFuture<Void> replaceAll(Collection<T> values) {
        if (!loaded) {
            return CompletableFuture.failedFuture(new StorageException(
                    "Not replacing the stored " + label + " entries, since they have not loaded", null));
        }
        Map<String, Map<String, Object>> serialized = serialize(values);
        serialized.keySet().removeIf(this::skipped);
        serialized.putAll(skipped);
        return write(() -> store.replaceAll(serialized));
    }

    public CompletableFuture<Void> delete(T value) {
        return delete(codec().key(value));
    }

    public CompletableFuture<Void> delete(String key) {
        if (refusedAfterFailedLoad()) {
            return CompletableFuture.completedFuture(null);
        }
        skipped.remove(key);
        return write(() -> store.delete(key));
    }

    /**
     * Holds back every write submitted from now on until {@code gate} completes, without blocking a thread.
     *
     * @return Completes once every write submitted before this call is done
     */
    synchronized CompletableFuture<Void> hold(CompletableFuture<Void> gate) {
        CompletableFuture<Void> drained = lastWrite.handle((ignored, throwable) -> null);
        lastWrite = drained.thenCompose(ignored -> gate);
        return drained;
    }

    /**
     * Sends every write that has not started yet to {@code store}. Only safe while writes are held.
     */
    void switchTo(EntityStore store) {
        this.store = store;
    }

    /**
     * {@return whether the stored entries have loaded, so that what is in memory is all of them} False before the
     * first load and after a failed one.
     */
    boolean loaded() {
        return loaded;
    }

    /**
     * {@return where the entries are stored now}
     */
    EntityStore store() {
        return store;
    }

    private synchronized CompletableFuture<Void> write(Runnable io) {
        CompletableFuture<Void> next =
                lastWrite.handle((ignored, throwable) -> null).thenRunAsync(io, background);
        lastWrite = next;
        return next;
    }

    public CompletableFuture<Map<String, T>> load() {
        return load((loaded, root) -> {});
    }

    /**
     * Reads every stored entry and parses it, skipping (and logging) one that cannot be parsed so the rest still load.
     *
     * @param link Runs once every entry is parsed, with a section holding each entry under its key, to resolve
     *     references between entries
     * @return The entries by key, in the store's order
     */
    public CompletableFuture<Map<String, T>> load(BiConsumer<Map<String, T>, ConfigurationSection> link) {
        return CompletableFuture.supplyAsync(
                        () -> {
                            MemoryConfiguration root = new MemoryConfiguration();
                            Map<String, T> loaded = new LinkedHashMap<>();
                            Map<String, Map<String, Object>> unparsed = new LinkedHashMap<>();
                            Map<String, Map<String, Object>> stored = store.loadAll();
                            stored.forEach((key, values) -> {
                                try {
                                    // Throws for an empty key or field name, which only a database can hold.
                                    ConfigurationSection section = root.createSection(key, values);
                                    loaded.put(key, codec().deserialize(key, section));
                                } catch (Exception e) {
                                    unparsed.put(key, values);
                                    try {
                                        root.set(key, null);
                                    } catch (IllegalArgumentException ignored) {
                                        // A key like "a." cannot be removed either; link then sees what was left.
                                    }
                                    logger.log(
                                            Level.WARNING,
                                            "Skipping " + label + " \"" + key + "\": could not be loaded",
                                            e);
                                }
                            });
                            link.accept(loaded, root);
                            skipped.clear();
                            skipped.putAll(unparsed);
                            refusalsLogged.clear();
                            return loaded;
                        },
                        background)
                .whenComplete((entries, throwable) -> {
                    this.loaded = throwable == null;
                    this.loadFailed = throwable != null;
                    this.refusalLogged = false;
                });
    }
}
