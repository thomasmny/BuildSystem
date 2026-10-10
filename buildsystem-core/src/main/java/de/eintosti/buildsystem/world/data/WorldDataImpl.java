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
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.world.WorldNames;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A world's settings, one value per {@link WorldDataSchema} key. Created through {@link WorldDataSchema#create}.
 */
@NullMarked
public class WorldDataImpl implements WorldData {

    private final Map<WorldDataKey<?>, Object> values;

    /**
     * Stored values that could not be resolved when loading, such as a status deleted since, as they were read. They
     * are saved in place of the fallback in effect, so a missing status or an unknown material does not overwrite the
     * stored one. Setting the key drops its entry.
     */
    private final Map<WorldDataKey<?>, String> unresolved = new HashMap<>();

    private String worldName;
    private Function<WorldDataKey<?>, @Nullable Object> override = key -> null;
    private @Nullable BiConsumer<BuildWorldStatus, BuildWorldStatus> statusChangeListener;

    WorldDataImpl(String worldName, Map<WorldDataKey<?>, Object> values) {
        this.worldName = worldName;
        this.values = values;
    }

    /**
     * Sets where overriding values come from, such as a folder's permission. An override only changes what
     * {@link #get} returns; the world's own value is kept and is what gets saved.
     *
     * @param override Returns the value in effect instead of the world's own, or {@code null} for none
     */
    public void setOverride(Function<WorldDataKey<?>, @Nullable Object> override) {
        this.override = override;
    }

    public void setStatusChangeListener(BiConsumer<BuildWorldStatus, BuildWorldStatus> listener) {
        this.statusChangeListener = listener;
    }

    private void requireKnown(WorldDataKey<?> key) {
        if (!values.containsKey(key)) {
            throw new IllegalArgumentException("Unknown world data key: " + key.id());
        }
    }

    @Override
    public <T> T get(WorldDataKey<T> key) {
        requireKnown(key);
        Object overriding = override.apply(key);
        return key.type().cast(overriding != null ? overriding : values.get(key));
    }

    @Override
    public <T> void set(WorldDataKey<T> key, T value) {
        requireKnown(key);
        Object previous = values.put(key, key.type().cast(Objects.requireNonNull(value, "value")));
        unresolved.remove(key);
        BiConsumer<BuildWorldStatus, BuildWorldStatus> listener = this.statusChangeListener;
        if (listener != null && key.equals(WorldDataKey.STATUS) && !value.equals(previous)) {
            listener.accept((BuildWorldStatus) previous, (BuildWorldStatus) value);
        }
    }

    /**
     * {@return the world's own values, without overrides, as they are saved}
     */
    public Map<WorldDataKey<?>, Object> storedValues() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * Keeps a stored value that could not be resolved, to be saved instead of the key's current value.
     */
    public void keepUnresolved(WorldDataKey<?> key, String raw) {
        requireKnown(key);
        unresolved.put(key, raw);
    }

    /**
     * {@return the stored value of {@code key} as it was read, if it could not be resolved and has not been set since}
     */
    public @Nullable String unresolved(WorldDataKey<?> key) {
        return unresolved.get(key);
    }

    /**
     * {@return whether the value of {@code key} is only a fallback for a stored value that could not be resolved}
     */
    public static boolean isUnresolved(WorldData data, WorldDataKey<?> key) {
        return data instanceof WorldDataImpl impl && impl.unresolved(key) != null;
    }

    /**
     * Changes the fallback a world shows for {@code key}, such as when the default status is deleted, without dropping
     * a stored value that could not be resolved. That value is still what gets saved. For any other world this is
     * {@link #set}.
     */
    public static <T> void setFallback(WorldData data, WorldDataKey<T> key, T value) {
        String raw = data instanceof WorldDataImpl impl ? impl.unresolved(key) : null;
        data.set(key, value);
        if (raw != null) {
            ((WorldDataImpl) data).keepUnresolved(key, raw);
        }
    }

    @Override
    public @Nullable Location getCustomSpawnLocation() {
        return CustomSpawn.parse(WorldNames.bukkitWorld(worldName), get(WorldDataKey.CUSTOM_SPAWN));
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
}
