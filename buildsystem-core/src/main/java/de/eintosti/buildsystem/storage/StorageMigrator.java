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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.yaml.YamlEntityStore;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Moves worlds, folders and players to another backend while the server runs.
 *
 * <p>What is in memory is copied, since that is what the server holds to be true. Writes queued before the copy finish
 * on the old backend first, and writes queued during it wait and then go to the new one, so none is lost. Nothing
 * switches until every collection is copied and read back, and the config is written before anything switches, so on
 * any failure the old backend stays in use and config.yml stays as it was.
 */
@NullMarked
public final class StorageMigrator {

    private final List<Part<?>> parts;
    private final Supplier<StorageBackend> current;
    private final Consumer<StorageBackend> switchBackend;
    private final File dataFolder;
    private final Supplier<FileConfiguration> liveConfig;
    private final Executor background;
    private final Executor mainThread;
    private final Logger logger;
    private final AtomicBoolean running = new AtomicBoolean();

    private volatile @Nullable CompletableFuture<Void> gate;

    public StorageMigrator(
            BuildSystemPlugin plugin,
            Services services,
            WorldStorageImpl worlds,
            FolderStorageImpl folders,
            PlayerStorageImpl players) {
        this(
                List.of(
                        new Part<>(EntityKind.WORLDS, worlds.collection(), worlds::getBuildWorlds),
                        new Part<>(EntityKind.FOLDERS, folders.collection(), folders::getFolders),
                        new Part<>(EntityKind.PLAYERS, players.collection(), players::getBuildPlayers)),
                services::storage,
                services::switchStorage,
                plugin.getDataFolder(),
                plugin::getConfig,
                services.scheduler().background(),
                services.scheduler().mainThread(),
                plugin.getLogger());
    }

    StorageMigrator(
            List<Part<?>> parts,
            Supplier<StorageBackend> current,
            Consumer<StorageBackend> switchBackend,
            File dataFolder,
            Supplier<FileConfiguration> liveConfig,
            Executor background,
            Executor mainThread,
            Logger logger) {
        this.parts = parts;
        this.current = current;
        this.switchBackend = switchBackend;
        this.dataFolder = dataFolder;
        this.liveConfig = liveConfig;
        this.background = background;
        this.mainThread = mainThread;
        this.logger = logger;
    }

    public boolean running() {
        return running.get();
    }

    /**
     * {@return how many entries of each kind are in memory, which is what a migration copies}
     */
    public Map<EntityKind, Integer> counts() {
        Map<EntityKind, Integer> counts = new EnumMap<>(EntityKind.class);
        parts.forEach(part -> counts.put(part.kind(), part.live().get().size()));
        return counts;
    }

    /**
     * Copies what is in memory to {@code type}, checks it by reading it back, writes {@code storage.type} and switches
     * to it. Call on the main thread, where the entities are owned.
     *
     * @param settings The table prefix and connection blocks to use, as config.yml holds them now
     * @return Completes with how many entries of each kind were copied once switched, or exceptionally with the old
     *     backend still in use and config.yml untouched
     */
    public CompletableFuture<Map<EntityKind, Integer>> migrate(
            StorageType type, PluginConfig.Storage.Database settings) {
        for (Part<?> part : parts) {
            if (!part.collection().loaded()) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("The stored " + part.kind().id()
                                + " have not loaded, so what is in memory may be incomplete. See the startup log."));
            }
        }
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A storage migration is already running"));
        }
        CompletableFuture<Void> gate = new CompletableFuture<>();
        this.gate = gate;
        AtomicReference<@Nullable StorageBackend> target = new AtomicReference<>();
        Map<EntityKind, Map<String, Map<String, Object>>> snapshots = new EnumMap<>(EntityKind.class);
        // Writes are only held once the target is reachable, so a server that never answers holds nothing back.
        return CompletableFuture.supplyAsync(() -> open(type, settings, target), background)
                .thenComposeAsync(stores -> hold(gate, snapshots).thenApply(ignored -> stores), mainThread)
                .thenApplyAsync(stores -> copy(type, stores, snapshots), background)
                .thenApplyAsync(stores -> switchTo(type, stores, target, gate), mainThread)
                .thenApply(ignored -> {
                    Map<EntityKind, Integer> counts = new EnumMap<>(EntityKind.class);
                    snapshots.forEach((kind, entries) -> counts.put(kind, entries.size()));
                    return counts;
                })
                .whenComplete((counts, failure) -> {
                    if (failure != null) {
                        gate.complete(null);
                        StorageBackend opened = target.get();
                        if (opened != null) {
                            opened.close();
                        }
                    }
                    running.set(false);
                });
    }

    /**
     * Lets held writes go on to the old backend and stops a running migration from switching. For plugin disable,
     * which waits for those writes.
     */
    public void abort() {
        CompletableFuture<Void> gate = this.gate;
        if (gate != null) {
            gate.complete(null);
        }
    }

    /**
     * {@return whether {@code type} already holds entries of any kind, checked in the background} Connects and closes
     * the connection again, and fails as a migration would when the backend cannot be reached.
     */
    public CompletableFuture<Boolean> holdsData(StorageType type, PluginConfig.Storage.Database settings) {
        return CompletableFuture.supplyAsync(
                () -> {
                    StorageBackend backend = StorageBackend.connect(type, settings, dataFolder, logger);
                    try {
                        for (Part<?> part : parts) {
                            EntityStore store = backend.collection(part.kind());
                            // Reading a YAML file that is not there would create it.
                            boolean present = !(store instanceof YamlEntityStore yaml) || yaml.exists();
                            if (present && !store.isEmpty()) {
                                return true;
                            }
                        }
                        return false;
                    } finally {
                        backend.close();
                    }
                },
                background);
    }

    private Map<EntityKind, EntityStore> open(
            StorageType type,
            PluginConfig.Storage.Database settings,
            AtomicReference<@Nullable StorageBackend> target) {
        StorageBackend backend = StorageBackend.connect(type, settings, dataFolder, logger);
        target.set(backend);
        Map<EntityKind, EntityStore> stores = new EnumMap<>(EntityKind.class);
        parts.forEach(part -> stores.put(part.kind(), backend.collection(part.kind())));
        return stores;
    }

    /**
     * Takes the snapshots and holds the writes in one step on the main thread, so no change slips between them.
     *
     * @return Completes once the writes submitted before are done
     */
    private CompletableFuture<Void> hold(
            CompletableFuture<Void> gate, Map<EntityKind, Map<String, Map<String, Object>>> snapshots) {
        if (gate.isDone()) {
            throw new IllegalStateException("BuildSystem is shutting down");
        }
        List<CompletableFuture<Void>> drained = new ArrayList<>();
        for (Part<?> part : parts) {
            snapshots.put(part.kind(), part.snapshot());
            drained.add(part.collection().hold(gate));
        }
        return CompletableFuture.allOf(drained.toArray(CompletableFuture[]::new));
    }

    private Map<EntityKind, EntityStore> copy(
            StorageType type,
            Map<EntityKind, EntityStore> stores,
            Map<EntityKind, Map<String, Map<String, Object>>> snapshots) {
        snapshots.forEach((kind, entries) -> {
            EntityStore store = stores.get(kind);
            store.replaceAll(entries);
            if (!normalize(store.loadAll()).equals(normalize(entries))) {
                throw new StorageException(
                        "The " + kind.id() + " read back from " + type.displayName() + " differ from those written",
                        null);
            }
        });
        return stores;
    }

    private Void switchTo(
            StorageType type,
            Map<EntityKind, EntityStore> stores,
            AtomicReference<@Nullable StorageBackend> target,
            CompletableFuture<Void> gate) {
        if (gate.isDone()) {
            throw new IllegalStateException("BuildSystem is shutting down");
        }
        writeConfig(type);
        parts.forEach(part -> part.collection().switchTo(stores.get(part.kind())));
        StorageBackend previous = current.get();
        switchBackend.accept(target.getAndSet(null));
        gate.complete(null);
        try {
            previous.close();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Could not close the previous storage", e);
        }
        logger.info("Worlds, folders and players are now stored in " + type.displayName());
        return null;
    }

    private void writeConfig(StorageType type) {
        // Edited on disk, since the loaded config may predate changes made to the file since.
        File file = new File(dataFolder, "config.yml");
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
            config.set("storage.type", type.key());
            config.save(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new StorageException("Could not write storage.type to config.yml: " + e.getMessage(), e);
        }
        liveConfig.get().set("storage.type", type.key());
    }

    /**
     * Makes stored values comparable whatever the backend: whole numbers come back as int or long, lists and maps as
     * any implementation.
     */
    private static @Nullable Object normalize(@Nullable Object value) {
        return switch (value) {
            case Map<?, ?> map -> {
                Map<Object, @Nullable Object> normalized = new HashMap<>();
                map.forEach((key, nested) -> normalized.put(key, normalize(nested)));
                yield normalized;
            }
            case List<?> list -> list.stream().map(StorageMigrator::normalize).toList();
            case Float number -> Double.parseDouble(number.toString());
            case Double number -> number;
            case Number number -> number.longValue();
            case null, default -> value;
        };
    }

    record Part<T>(EntityKind kind, EntityCollection<T> collection, Supplier<? extends Collection<T>> live) {

        Map<String, Map<String, Object>> snapshot() {
            return collection.serialize(live.get());
        }
    }
}
