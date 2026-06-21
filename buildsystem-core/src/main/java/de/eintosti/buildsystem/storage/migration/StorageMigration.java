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
package de.eintosti.buildsystem.storage.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The v3 → v4 on-disk migration for the cache-backed data files. v3 keyed {@code worlds.yml}/{@code folders.yml}
 * sections by entity <em>name</em>; v4 keys them by <em>UUID</em> and stores the name as a field, so a rename becomes a
 * field update rather than a key move (which v3 left orphaned on disk). Folders additionally switch their stored parent
 * reference from the parent's name to its UUID.
 *
 * <p>The two transforms are pure functions over a loaded configuration — version stamping, backup, and saving are the
 * storage's responsibility, which lets a storage gate them behind its version check and I/O lock. A missing or garbled
 * {@code uuid} is regenerated rather than dropping the entity, so a corrupt id costs an identity, not a world.
 */
@NullMarked
public final class StorageMigration {

    public static final int CURRENT_VERSION = 4;
    public static final String VERSION_KEY = "version";
    public static final String BACKUP_SUFFIX = ".v3.bak";

    private static final String WORLDS = "worlds";
    private static final String FOLDERS = "folders";
    private static final String NAME = "name";
    private static final String UUID_KEY = "uuid";
    private static final String PARENT = "parent";

    private StorageMigration() {}

    /**
     * Re-keys every world section under its UUID, adding a {@code name} field carrying the old (name) section key.
     *
     * @param config The loaded {@code worlds.yml}
     * @param logger The logger for regeneration warnings
     */
    public static void migrateWorlds(ConfigurationSection config, Logger logger) {
        ConfigurationSection worlds = config.getConfigurationSection(WORLDS);
        if (worlds == null) {
            return;
        }

        for (String name : new ArrayList<>(worlds.getKeys(false))) {
            if (isUuid(name)) {
                continue; // Already v4-shaped; re-keying would clobber its name field with the UUID.
            }
            ConfigurationSection entry = worlds.getConfigurationSection(name);
            if (entry == null) {
                continue;
            }
            String uuid =
                    parseOrGenerate(entry.getString(UUID_KEY), name, logger).toString();
            Map<String, Object> values = deepCopy(entry);
            values.put(NAME, name);
            values.put(UUID_KEY, uuid);
            worlds.set(name, null);
            // Clear any prior section under this UUID so a clean replace (not a merge) wins. A pre-4.0 rename left the
            // old name key orphaned alongside the live one, both carrying the same UUID; collapsing them here keeps the
            // later (live) entry and discards the stale orphan.
            worlds.set(uuid, null);
            worlds.createSection(uuid, values);
        }
    }

    /**
     * Re-keys every folder section under its UUID and rewrites its parent reference from the parent's name to the
     * parent's UUID, using a name → UUID map built across all folders first (the parent may be re-keyed too).
     *
     * @param config The loaded {@code folders.yml}
     * @param logger The logger for regeneration warnings
     */
    public static void migrateFolders(ConfigurationSection config, Logger logger) {
        ConfigurationSection folders = config.getConfigurationSection(FOLDERS);
        if (folders == null) {
            return;
        }
        List<String> names = new ArrayList<>(folders.getKeys(false));

        Map<String, String> nameToUuid = new HashMap<>();
        for (String name : names) {
            if (isUuid(name)) {
                nameToUuid.put(name, name); // Already v4-shaped; maps to itself for parent resolution.
                continue;
            }
            ConfigurationSection entry = folders.getConfigurationSection(name);
            if (entry == null) {
                continue;
            }
            nameToUuid.put(
                    name,
                    parseOrGenerate(entry.getString(UUID_KEY), name, logger).toString());
        }

        for (String name : names) {
            if (isUuid(name)) {
                continue; // Already v4-shaped; re-keying would clobber its name field with the UUID.
            }
            ConfigurationSection entry = folders.getConfigurationSection(name);
            if (entry == null) {
                continue;
            }
            String uuid = nameToUuid.get(name);
            Map<String, Object> values = deepCopy(entry);
            values.put(NAME, name);
            values.put(UUID_KEY, uuid);
            String parentName = entry.getString(PARENT);
            values.put(PARENT, parentName != null ? nameToUuid.get(parentName) : null);
            folders.set(name, null);
            folders.set(uuid, null);
            folders.createSection(uuid, values);
        }
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static UUID parseOrGenerate(@Nullable String raw, String entityName, Logger logger) {
        if (raw != null) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                logger.warning("Regenerating unparseable uuid \"" + raw + "\" for \"" + entityName + "\".");
            }
        }
        return UUID.randomUUID();
    }

    /**
     * Deep-copies a section into a nested map, so it can be re-inserted under a new key via
     * {@link ConfigurationSection#createSection(String, Map)} without aliasing the original. Null values are dropped to
     * match the absent-key semantics callers rely on (e.g. a parentless folder).
     */
    private static Map<String, Object> deepCopy(ConfigurationSection section) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                copy.put(key, deepCopy(child));
            } else if (value != null) {
                copy.put(key, value);
            }
        }
        return copy;
    }
}
