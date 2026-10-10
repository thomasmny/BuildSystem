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
import de.eintosti.buildsystem.storage.codec.Codec;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * The file behind a status or category registry, holding every entry under one root key. Small and edited on the main
 * thread, so it is read and written synchronously. A malformed entry is logged and skipped rather than failing the
 * whole load.
 */
@NullMarked
public final class YamlRegistryStorage<E> {

    private final YamlStore store;
    private final String rootKey;
    private final String label;
    private final Codec<E> codec;
    private final Logger logger;

    public YamlRegistryStorage(
            BuildSystemPlugin plugin, String fileName, String rootKey, String label, Codec<E> codec) {
        this.logger = plugin.getLogger();
        this.store = new YamlStore(plugin.getDataFolder(), fileName, logger);
        this.rootKey = rootKey;
        this.label = label;
        this.codec = codec;
        store.locked(store::reload);
    }

    public Map<String, E> load() {
        Map<String, E> entries = new LinkedHashMap<>();
        ConfigurationSection root = store.config().getConfigurationSection(rootKey);
        if (root == null) {
            return entries;
        }
        for (String id : root.getKeys(false)) {
            try {
                ConfigurationSection section = root.getConfigurationSection(id);
                if (section == null) {
                    throw new IllegalArgumentException("not a section");
                }
                entries.put(id, codec.deserialize(id, section));
            } catch (Exception e) {
                logger.warning("Skipping " + label + " \"" + id + "\": " + e.getMessage());
            }
        }
        return entries;
    }

    /**
     * Replaces every stored entry with {@code entries}.
     */
    public void saveAll(Collection<E> entries) {
        store.atomicSave(() -> {
            FileConfiguration config = store.config();
            config.set(rootKey, null);
            entries.forEach(entry -> config.set(path(entry), codec.serialize(entry)));
        });
    }

    public void save(E entry) {
        store.atomicSave(() -> store.config().set(path(entry), codec.serialize(entry)));
    }

    public void delete(String id) {
        store.atomicSave(() -> store.config().set(rootKey + "." + id, null));
    }

    /**
     * {@return a value stored next to the entries, outside the root key}
     */
    public int getInt(String key, int fallback) {
        return store.config().getInt(key, fallback);
    }

    /**
     * Stores a value next to the entries, outside the root key.
     */
    public void set(String key, Object value) {
        store.atomicSave(() -> store.config().set(key, value));
    }

    private String path(E entry) {
        return rootKey + "." + codec.key(entry);
    }
}
