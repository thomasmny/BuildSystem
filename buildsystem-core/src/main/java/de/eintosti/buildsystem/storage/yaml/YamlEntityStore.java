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

import de.eintosti.buildsystem.storage.EntityStore;
import de.eintosti.buildsystem.storage.migration.StorageMigration;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * One YAML file holding entries of one kind under a root key ({@code worlds}, {@code folders}, {@code players}). The
 * root key doubles as the collection's name in the other backends.
 */
@NullMarked
public final class YamlEntityStore implements EntityStore {

    private static final int LEGACY_VERSION = 1;

    private final YamlStore store;
    private final String rootKey;
    private final Logger logger;
    private final @Nullable BiConsumer<ConfigurationSection, Logger> migration;

    /**
     * @param rootKey The section the entries live under
     * @param migration Brings a legacy name-keyed file up to {@link StorageMigration#CURRENT_VERSION}, or {@code null}
     *     for a file that is not versioned
     */
    public YamlEntityStore(
            YamlStore store,
            String rootKey,
            Logger logger,
            @Nullable BiConsumer<ConfigurationSection, Logger> migration) {
        this.store = store;
        this.rootKey = rootKey;
        this.logger = logger;
        this.migration = migration;
    }

    public static YamlEntityStore worlds(File dataFolder, Logger logger) {
        return new YamlEntityStore(
                new YamlStore(dataFolder, "worlds.yml", logger), "worlds", logger, StorageMigration::migrateWorlds);
    }

    public static YamlEntityStore folders(File dataFolder, Logger logger) {
        return new YamlEntityStore(
                new YamlStore(dataFolder, "folders.yml", logger), "folders", logger, StorageMigration::migrateFolders);
    }

    public static YamlEntityStore players(File dataFolder, Logger logger) {
        return new YamlEntityStore(new YamlStore(dataFolder, "players.yml", logger), "players", logger, null);
    }

    /**
     * Re-reads the file, migrating a legacy one first.
     *
     * @return The entries in file order
     */
    @Override
    public SequencedMap<String, Map<String, Object>> loadAll() {
        return store.locked(() -> {
            SequencedMap<String, Map<String, Object>> entries = new LinkedHashMap<>();
            if (!store.reload()) {
                return entries;
            }
            migrateIfNeeded();

            ConfigurationSection root = store.config().getConfigurationSection(rootKey);
            if (root == null) {
                return entries;
            }
            for (String key : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section != null) {
                    entries.put(key, toMap(section));
                }
            }
            return entries;
        });
    }

    /**
     * Writes the given entries, leaving every other entry in the file as it is.
     */
    @Override
    public void upsert(Map<String, Map<String, Object>> entries) {
        store.atomicSave(() -> {
            if (migration != null) {
                store.config().set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
            }
            entries.forEach((key, values) -> store.config().set(rootKey + "." + key, values));
        });
    }

    @Override
    public void delete(String key) {
        store.atomicSave(() -> store.config().set(rootKey + "." + key, null));
    }

    private static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        section.getValues(false)
                .forEach((key, value) ->
                        values.put(key, value instanceof ConfigurationSection nested ? toMap(nested) : value));
        return values;
    }

    /**
     * Brings a legacy (name-keyed) file up to the current UUID-keyed format, once, before parsing.
     */
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
