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

import de.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.Location;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Manages and provides access to the settings of a {@link BuildWorld}. Settings are addressed through typed
 * {@link WorldDataKey keys} rather than a separate getter/setter per setting: {@link #get(WorldDataKey)} reads a value
 * and {@link #set(WorldDataKey, Object)} writes one, the key's type parameter carrying the value type so the call site
 * needs no cast. See {@link WorldDataKey} for the catalog of built-in keys.
 *
 * @since 3.0.0
 */
@NullMarked
public interface WorldData {

    /**
     * Reads the value of a setting.
     *
     * @param key The setting to read
     * @param <T> The value type carried by the key
     * @return The current value
     * @since 4.0.0
     */
    <T> T get(WorldDataKey<T> key);

    /**
     * Writes the value of a setting.
     *
     * @param key The setting to write
     * @param value The new value
     * @param <T> The value type carried by the key
     * @since 4.0.0
     */
    <T> void set(WorldDataKey<T> key, T value);

    /**
     * Gets the {@link BuildWorld}'s custom spawn as a {@link Location}, parsed from {@link WorldDataKey#CUSTOM_SPAWN}.
     *
     * @return The custom spawn as a location, or {@code null} if not set or invalid
     * @since 3.0.0
     */
    @Nullable Location getCustomSpawnLocation();
}
