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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Turns Bukkit objects such as the items of an archive snapshot into plain maps for a database, and back. It uses the
 * same {@code ==} type key YAML writes, so the database holds what players.yml would.
 */
@NullMarked
public final class SerializedObjects {

    private SerializedObjects() {}

    /**
     * {@return {@code values} with every {@link ConfigurationSerializable} in it replaced by its map}
     */
    public static Map<String, Object> flatten(Map<String, Object> values) {
        Map<String, Object> flat = new LinkedHashMap<>();
        values.forEach((key, value) -> flat.put(key, flattenValue(value)));
        return flat;
    }

    /**
     * {@return {@code values} with every map holding a {@code ==} type key turned back into its object}
     */
    public static Map<String, Object> revive(Map<String, Object> values) {
        Map<String, Object> revived = new LinkedHashMap<>();
        values.forEach((key, value) -> revived.put(key, reviveValue(value)));
        return revived;
    }

    private static @Nullable Object flattenValue(@Nullable Object value) {
        if (value instanceof ConfigurationSerializable serializable) {
            Map<String, Object> flat = new LinkedHashMap<>();
            flat.put(
                    ConfigurationSerialization.SERIALIZED_TYPE_KEY,
                    ConfigurationSerialization.getAlias(serializable.getClass()));
            flat.putAll(flatten(serializable.serialize()));
            return flat;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> flat = new LinkedHashMap<>();
            map.forEach((key, nested) -> flat.put(String.valueOf(key), flattenValue(nested)));
            return flat;
        }
        if (value instanceof List<?> list) {
            List<@Nullable Object> flat = new ArrayList<>();
            list.forEach(element -> flat.add(flattenValue(element)));
            return flat;
        }
        return value;
    }

    private static @Nullable Object reviveValue(@Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> revived = new LinkedHashMap<>();
            map.forEach((key, nested) -> revived.put(String.valueOf(key), reviveValue(nested)));
            return revived.containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)
                    ? ConfigurationSerialization.deserializeObject(asYamlNumbers(revived))
                    : revived;
        }
        if (value instanceof List<?> list) {
            List<@Nullable Object> revived = new ArrayList<>();
            list.forEach(element -> revived.add(reviveValue(element)));
            return revived;
        }
        return value;
    }

    /**
     * {@return {@code value} with every whole number that fits an int as an Integer, at any depth} YAML hands
     * deserializers such numbers as Integer, and some cast to it, such as for enchantment levels.
     */
    @SuppressWarnings("unchecked")
    private static <V> V asYamlNumbers(V value) {
        if (value instanceof Long number && number == number.intValue()) {
            return (V) Integer.valueOf(number.intValue());
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> converted = new LinkedHashMap<>();
            map.forEach((key, nested) -> converted.put(key, asYamlNumbers(nested)));
            return (V) converted;
        }
        if (value instanceof List<?> list) {
            List<@Nullable Object> converted = new ArrayList<>();
            list.forEach(element -> converted.add(asYamlNumbers(element)));
            return (V) converted;
        }
        return value;
    }
}
