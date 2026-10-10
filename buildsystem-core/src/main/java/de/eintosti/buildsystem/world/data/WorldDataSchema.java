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
package de.eintosti.buildsystem.world.data;

import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.PhysicsCategory;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;

/**
 * The persisted world settings: every {@link WorldDataKey} a world stores, with its fallback value. Adding a setting
 * means adding its key here; the codec reads and writes every key in this table.
 *
 * <p>A stored world that lacks a key loads with the fallback, except for two keys the codec handles itself: the
 * material falls back to {@link Material#BEDROCK}, and the physics exceptions fall back to the config's defaults.
 */
@NullMarked
public final class WorldDataSchema {

    /**
     * Every key except {@link WorldDataKey#STATUS}, whose fallback is the registry's default status.
     */
    private static final Map<WorldDataKey<?>, Object> FALLBACKS = fallbacks();

    private WorldDataSchema() {}

    private static Map<WorldDataKey<?>, Object> fallbacks() {
        Map<WorldDataKey<?>, Object> fallbacks = new LinkedHashMap<>();
        fallbacks.put(WorldDataKey.CUSTOM_SPAWN, "");
        fallbacks.put(WorldDataKey.PERMISSION, "-");
        fallbacks.put(WorldDataKey.PROJECT, "-");
        fallbacks.put(WorldDataKey.DIFFICULTY, Difficulty.PEACEFUL);
        fallbacks.put(WorldDataKey.MATERIAL, Material.GRASS_BLOCK);
        fallbacks.put(WorldDataKey.ICON_SKULL_TEXTURE, "");
        fallbacks.put(WorldDataKey.BLOCK_BREAKING, true);
        fallbacks.put(WorldDataKey.BLOCK_INTERACTIONS, true);
        fallbacks.put(WorldDataKey.BLOCK_PLACEMENT, true);
        fallbacks.put(WorldDataKey.BUILDERS_ENABLED, false);
        fallbacks.put(WorldDataKey.EXPLOSIONS, true);
        fallbacks.put(WorldDataKey.MOB_AI, true);
        fallbacks.put(WorldDataKey.PHYSICS, true);
        for (PhysicsCategory category : PhysicsCategory.values()) {
            // Blocked unless the world or the config's physics-exceptions allow it.
            fallbacks.put(category.key(), false);
        }
        fallbacks.put(WorldDataKey.PINNED, false);
        fallbacks.put(WorldDataKey.VISIBILITY, Visibility.EVERYONE);
        fallbacks.put(WorldDataKey.TIME_SINCE_BACKUP, 0);
        // -1 means never.
        fallbacks.put(WorldDataKey.LAST_EDITED, -1L);
        fallbacks.put(WorldDataKey.LAST_LOADED, -1L);
        fallbacks.put(WorldDataKey.LAST_UNLOADED, -1L);
        return Collections.unmodifiableMap(fallbacks);
    }

    /**
     * {@return every key a world stores}
     */
    public static Set<WorldDataKey<?>> keys() {
        Set<WorldDataKey<?>> keys = new LinkedHashSet<>(FALLBACKS.keySet());
        keys.add(WorldDataKey.STATUS);
        return keys;
    }

    /**
     * {@return data for the named world with every key at its fallback}
     *
     * @param status The world's status, which has no fixed fallback
     */
    public static WorldDataImpl create(String worldName, BuildWorldStatus status) {
        Map<WorldDataKey<?>, Object> values = new LinkedHashMap<>();
        values.put(WorldDataKey.STATUS, status);
        values.putAll(FALLBACKS);
        return new WorldDataImpl(worldName, values);
    }

    /**
     * {@return the value as it is written to disk} Enums by name and statuses by id.
     */
    public static Object toYaml(Object value) {
        return switch (value) {
            case BuildWorldStatus status -> status.getId();
            case Enum<?> constant -> constant.name();
            default -> value;
        };
    }
}
