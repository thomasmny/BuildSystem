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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Services;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.storage.codec.WorldCodec;
import de.eintosti.buildsystem.storage.migration.StorageMigration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class YamlWorldStorage extends WorldStorageImpl {

    private static final String WORLDS_KEY = "worlds";
    private static final int LEGACY_VERSION = 1;

    private final Services services;
    private final YamlStore store;
    private final FileConfiguration config;
    private final Executor background;
    private @Nullable WorldCodec codec;

    public YamlWorldStorage(BuildSystemPlugin plugin, Services services) {
        super(plugin.getLogger());
        this.services = services;
        this.store = new YamlStore(plugin.getDataFolder(), "worlds.yml", plugin.getLogger());
        this.config = store.config();
        this.background = services.scheduler().background();
    }

    /**
     * The codec, built lazily on first use. Worlds are loaded during plugin enable, before some of the services the
     * {@code WorldContext} bundles exist; deferring construction to first load (after enable completes the service
     * graph) keeps startup from resolving a not-yet-created service.
     */
    private WorldCodec codec() {
        if (codec == null) {
            codec = new WorldCodec(services.worldContext(), services.playerLookup());
        }
        return codec;
    }

    @Override
    public CompletableFuture<Void> save(BuildWorld buildWorld) {
        // Serialize on the calling (main) thread, where the world's data is owned: the async block must only write the
        // already-captured map to disk, never read live domain state off the main thread.
        String worldKey = codec().key(buildWorld);
        Map<String, @Nullable Object> serialized = codec().serialize(buildWorld);
        return CompletableFuture.runAsync(
                () -> store.atomicSave(() -> {
                    config.set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
                    config.set(WORLDS_KEY + "." + worldKey, serialized);
                }),
                background);
    }

    @Override
    public CompletableFuture<Void> save(Collection<BuildWorld> buildWorlds) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        for (BuildWorld buildWorld : buildWorlds) {
            serialized.put(codec().key(buildWorld), codec().serialize(buildWorld));
        }
        return CompletableFuture.runAsync(
                () -> store.atomicSave(() -> {
                    config.set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
                    serialized.forEach((worldKey, value) -> config.set(WORLDS_KEY + "." + worldKey, value));
                }),
                background);
    }

    @Override
    public CompletableFuture<Collection<BuildWorld>> load() {
        return CompletableFuture.supplyAsync(
                () -> store.locked(() -> {
                    Collection<BuildWorld> worlds = new ArrayList<>();
                    for (String worldKey : loadWorldKeys()) {
                        try {
                            ConfigurationSection section = config.getConfigurationSection(WORLDS_KEY + "." + worldKey);
                            if (section == null) {
                                continue;
                            }
                            worlds.add(codec().deserialize(worldKey, section));
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Skipping world \"" + worldKey + "\": could not be loaded", e);
                        }
                    }
                    return worlds;
                }),
                background);
    }

    private Set<String> loadWorldKeys() {
        if (!store.reload()) {
            return Set.of();
        }
        migrateIfNeeded();

        ConfigurationSection section = config.getConfigurationSection(WORLDS_KEY);
        if (section == null) {
            return Set.of();
        }

        return section.getKeys(false);
    }

    /** Brings a legacy (name-keyed) {@code worlds.yml} up to the current UUID-keyed format, once, before parsing. */
    private void migrateIfNeeded() {
        if (config.getInt(StorageMigration.VERSION_KEY, LEGACY_VERSION) >= StorageMigration.CURRENT_VERSION) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection(WORLDS_KEY);
        if (section != null && !section.getKeys(false).isEmpty()) {
            store.backupOnce(StorageMigration.BACKUP_SUFFIX);
            StorageMigration.migrateWorlds(config, logger);
        }
        config.set(StorageMigration.VERSION_KEY, StorageMigration.CURRENT_VERSION);
        store.save();
    }

    @Override
    public CompletableFuture<Void> delete(BuildWorld buildWorld) {
        return delete(buildWorld.getUniqueId().toString());
    }

    @Override
    public CompletableFuture<Void> delete(String worldKey) {
        return CompletableFuture.runAsync(
                () -> store.atomicSave(() -> config.set(WORLDS_KEY + "." + worldKey, null)), background);
    }
}
