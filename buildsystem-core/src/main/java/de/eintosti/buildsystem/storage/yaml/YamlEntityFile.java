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
package de.eintosti.buildsystem.storage.yaml;

import de.eintosti.buildsystem.storage.codec.Codec;
import de.eintosti.buildsystem.storage.migration.StorageMigration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One YAML file holding entities of one kind under a root key ({@code worlds}, {@code folders}, {@code players}), each
 * filed under its {@link Codec#key codec key}.
 *
 * <p>Entities are serialized on the calling thread, where their state is owned, and only the captured maps are written
 * off it. Writes run one after another in the order they were submitted, so a save queued before a delete can never
 * land after it and bring the entry back.
 */
@NullMarked
public final class YamlEntityFile<T> {

    private static final int LEGACY_VERSION = 1;

    private final YamlStore store;
    private final String rootKey;
    private final String label;
    private final Supplier<Codec<T>> codecFactory;
    private final Executor background;
    private final Logger logger;
    private final @Nullable BiConsumer<ConfigurationSection, Logger> migration;

    private @Nullable Codec<T> codec;
    private CompletableFuture<Void> lastWrite = CompletableFuture.completedFuture(null);

    /**
     * @param rootKey The section the entities live under
     * @param label What one entity is called in log messages
     * @param codecFactory Builds the codec on first use: it may need services that only exist once the plugin has
     *     enabled
     * @param migration Brings a legacy name-keyed file up to {@link StorageMigration#CURRENT_VERSION}, or {@code null}
     *     for a file that is not versioned
     */
    public YamlEntityFile(
            YamlStore store,
            String rootKey,
            String label,
            Supplier<Codec<T>> codecFactory,
            Executor background,
            Logger logger,
            @Nullable BiConsumer<ConfigurationSection, Logger> migration) {
        this.store = store;
        this.rootKey = rootKey;
        this.label = label;
        this.codecFactory = codecFactory;
        this.background = background;
        this.logger = logger;
        this.migration = migration;
    }

    private synchronized Codec<T> codec() {
        if (codec == null) {
            codec = codecFactory.get();
        }
        return codec;
    }

    public CompletableFuture<Void> save(T value) {
        return save(List.of(value));
    }

    /**
     * Writes the given entities, leaving every other entry in the file as it is. Entries that failed to load stay on
     * disk this way, rather than being erased by the next save.
     */
    public CompletableFuture<Void> save(Collection<T> values) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        for (T value : values) {
            serialized.put(codec().key(value), codec().serialize(value));
        }
        return write(config -> {
            if (migration != null) {
                config.set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
            }
            serialized.forEach((key, value) -> config.set(rootKey + "." + key, value));
        });
    }

    public CompletableFuture<Void> delete(T value) {
        return delete(codec().key(value));
    }

    public CompletableFuture<Void> delete(String key) {
        return write(config -> config.set(rootKey + "." + key, null));
    }

    private synchronized CompletableFuture<Void> write(Consumer<FileConfiguration> mutation) {
        FileConfiguration config = store.config();
        CompletableFuture<Void> next = lastWrite
                .handle((ignored, throwable) -> null)
                .thenRunAsync(() -> store.atomicSave(() -> mutation.accept(config)), background);
        lastWrite = next;
        return next;
    }

    public CompletableFuture<Map<String, T>> load() {
        return load((loaded, root) -> {});
    }

    /**
     * Re-reads the file and parses every entry, skipping (and logging) one that cannot be parsed so the rest still load.
     *
     * @param link Runs under the file lock once every entry is parsed, with the root section, to resolve references
     *     between entries
     * @return The entries by key, in file order
     */
    public CompletableFuture<Map<String, T>> load(BiConsumer<Map<String, T>, ConfigurationSection> link) {
        return CompletableFuture.supplyAsync(
                () -> store.locked(() -> {
                    Map<String, T> loaded = new LinkedHashMap<>();
                    if (!store.reload()) {
                        return loaded;
                    }
                    migrateIfNeeded();

                    ConfigurationSection root = store.config().getConfigurationSection(rootKey);
                    if (root == null) {
                        return loaded;
                    }
                    for (String key : root.getKeys(false)) {
                        ConfigurationSection section = root.getConfigurationSection(key);
                        if (section == null) {
                            continue;
                        }
                        try {
                            loaded.put(key, codec().deserialize(key, section));
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Skipping " + label + " \"" + key + "\": could not be loaded", e);
                        }
                    }
                    link.accept(loaded, root);
                    return loaded;
                }),
                background);
    }

    /** Brings a legacy (name-keyed) file up to the current UUID-keyed format, once, before parsing. */
    private void migrateIfNeeded() {
        if (migration == null) {
            return;
        }
        FileConfiguration config = store.config();
        if (config.getInt(StorageMigration.VERSION_KEY, LEGACY_VERSION) >= StorageMigration.CURRENT_VERSION) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection(rootKey);
        if (section != null && !section.getKeys(false).isEmpty()) {
            store.backupOnce(StorageMigration.BACKUP_SUFFIX);
            migration.accept(config, logger);
        }
        config.set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
        store.save();
    }
}
