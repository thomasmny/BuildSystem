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
package de.eintosti.buildsystem.api.world.data;

import com.cryptomorin.xseries.XMaterial;
import java.util.Objects;
import org.bukkit.Difficulty;
import org.jspecify.annotations.NullMarked;

/**
 * A typed key identifying a single setting of a {@link WorldData}. The catalog of built-in keys below replaces the old
 * flat list of named getters/setters with a typesafe heterogeneous container (Bloch, <i>Effective Java</i> Item 33):
 * every value is read with {@link WorldData#get(WorldDataKey)} and written with {@link WorldData#set(WorldDataKey, Object)},
 * the key's type parameter carrying the value type through the call so no cast is needed at the call site.
 *
 * <p>A key's {@link #id() id} is the stable on-disk config name for its setting; it must never change once a release
 * has persisted it. The keys are value objects compared by id, so two keys with the same id are equal.
 *
 * @param <T> The type of the value stored under this key
 * @since 4.0.0
 */
@NullMarked
public final class WorldDataKey<T> {

    /**
     * The custom spawn as a {@code x;y;z;yaw;pitch} string; see {@link WorldData#getCustomSpawnLocation()} for the
     * parsed form.
     */
    public static final WorldDataKey<String> CUSTOM_SPAWN = of("spawn", String.class);

    /**
     * The permission required to enter the world, or {@code "-"} when none is required.
     */
    public static final WorldDataKey<String> PERMISSION = of("permission", String.class);

    /**
     * The project description of the world.
     */
    public static final WorldDataKey<String> PROJECT = of("project", String.class);

    /**
     * The world's {@link Difficulty}.
     */
    public static final WorldDataKey<Difficulty> DIFFICULTY = of("difficulty", Difficulty.class);

    /**
     * The {@link XMaterial} shown for the world in the navigator menus.
     */
    public static final WorldDataKey<XMaterial> MATERIAL = of("material", XMaterial.class);

    /**
     * The skull texture used when the {@link #MATERIAL icon} is a player head; the empty string means none.
     */
    public static final WorldDataKey<String> ICON_SKULL_TEXTURE = of("icon-skull-texture", String.class);

    /**
     * The current {@link BuildWorldStatus} of the world.
     */
    public static final WorldDataKey<BuildWorldStatus> STATUS = of("status", BuildWorldStatus.class);

    /**
     * Whether block breaking is allowed.
     */
    public static final WorldDataKey<Boolean> BLOCK_BREAKING = of("block-breaking", Boolean.class);

    /**
     * Whether block interactions (doors, chests, …) are enabled.
     */
    public static final WorldDataKey<Boolean> BLOCK_INTERACTIONS = of("block-interactions", Boolean.class);

    /**
     * Whether block placement is allowed.
     */
    public static final WorldDataKey<Boolean> BLOCK_PLACEMENT = of("block-placement", Boolean.class);

    /**
     * Whether the builders feature (only designated builders may modify the world) is enabled.
     */
    public static final WorldDataKey<Boolean> BUILDERS_ENABLED = of("builders-enabled", Boolean.class);

    /**
     * Whether explosions are enabled.
     */
    public static final WorldDataKey<Boolean> EXPLOSIONS = of("explosions", Boolean.class);

    /**
     * Whether entities have artificial intelligence.
     */
    public static final WorldDataKey<Boolean> MOB_AI = of("mob-ai", Boolean.class);

    /**
     * Whether block physics (gravity, fluid flow, …) is applied.
     */
    public static final WorldDataKey<Boolean> PHYSICS = of("physics", Boolean.class);

    /**
     * Whether the world is pinned to the top of the navigator.
     */
    public static final WorldDataKey<Boolean> PINNED = of("pinned", Boolean.class);

    /**
     * The {@link Visibility} governing who may see and enter the world.
     */
    public static final WorldDataKey<Visibility> VISIBILITY = of("visibility", Visibility.class);

    /**
     * Seconds elapsed since the world's last backup.
     */
    public static final WorldDataKey<Integer> TIME_SINCE_BACKUP = of("time-since-backup", Integer.class);

    /**
     * Epoch-millis timestamp of the world's last edit.
     */
    public static final WorldDataKey<Long> LAST_EDITED = of("last-edited", Long.class);

    /**
     * Epoch-millis timestamp of the world's last load.
     */
    public static final WorldDataKey<Long> LAST_LOADED = of("last-loaded", Long.class);

    /**
     * Epoch-millis timestamp of the world's last unload.
     */
    public static final WorldDataKey<Long> LAST_UNLOADED = of("last-unloaded", Long.class);

    private final String id;
    private final Class<T> type;

    private WorldDataKey(String id, Class<T> type) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Creates a key with the given on-disk id and value type.
     *
     * @param id The stable config name for the setting
     * @param type The runtime type of the stored value
     * @param <T> The value type
     * @return A new key
     */
    public static <T> WorldDataKey<T> of(String id, Class<T> type) {
        return new WorldDataKey<>(id, type);
    }

    /**
     * {@return the stable on-disk config name identifying this setting}
     */
    public String id() {
        return id;
    }

    /**
     * {@return the runtime type of the value stored under this key}
     */
    public Class<T> type() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WorldDataKey<?> key && id.equals(key.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "WorldDataKey[" + id + "]";
    }
}
