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
package de.eintosti.buildsystem.storage.codec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The stored fields of one entity type, each declared once with its key, how it is written and how it is read back.
 * Fields are written in declaration order. A dotted key such as {@code settings.world-display.sort} is written as
 * nested sections.
 *
 * @param <T> The entity being written
 * @param <B> What a read field is applied to: a builder, or the entity itself when it has setters
 */
@NullMarked
final class FieldCodec<T, B> {

    private final List<Field<T, B, ?>> fields;

    @SafeVarargs
    FieldCodec(Field<T, B, ?>... fields) {
        this(List.of(fields));
    }

    FieldCodec(List<Field<T, B, ?>> fields) {
        this.fields = List.copyOf(fields);
    }

    /**
     * Writes every field whose value is present. A field that writes {@code null} is left out of the map.
     */
    Map<String, Object> serialize(T value) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        for (Field<T, B, ?> field : fields) {
            Object written = field.writer().apply(value);
            if (written != null) {
                put(serialized, field.key(), written);
            }
        }
        return serialized;
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, Object> map, String key, Object value) {
        int dot = key.indexOf('.');
        if (dot < 0) {
            map.put(key, value);
            return;
        }
        Map<String, Object> section = (Map<String, Object>)
                map.computeIfAbsent(key.substring(0, dot), ignored -> new LinkedHashMap<String, Object>());
        put(section, key.substring(dot + 1), value);
    }

    /**
     * Reads every field that has a setter from the section and applies it to {@code target}.
     */
    void read(ConfigurationSection section, B target) {
        for (Field<T, B, ?> field : fields) {
            field.readInto(section, target);
        }
    }

    /**
     * Reads a field's value. A reader decides on its own fallback when the key is missing or unparseable.
     */
    @FunctionalInterface
    interface Reader<V> {
        V read(ConfigurationSection section, String key);
    }

    /**
     * One stored field.
     *
     * @param key The key, dotted for a nested section
     * @param writer The value as it is written to disk, or {@code null} to leave the key out
     * @param reader Reads the value back
     * @param setter Applies the read value, or {@code null} for a field the codec's caller reads itself
     */
    record Field<T, B, V>(
            String key,
            Function<T, @Nullable Object> writer,
            Reader<V> reader,
            @Nullable BiConsumer<B, V> setter) {

        private void readInto(ConfigurationSection section, B target) {
            if (setter != null) {
                setter.accept(target, reader.read(section, key));
            }
        }
    }

    static <T, B> Field<T, B, String> string(
            String key, String fallback, Function<T, String> getter, BiConsumer<B, String> setter) {
        return new Field<>(key, getter::apply, (section, path) -> section.getString(path, fallback), setter);
    }

    static <T, B> Field<T, B, @Nullable String> optionalString(
            String key, Function<T, @Nullable String> getter, BiConsumer<B, @Nullable String> setter) {
        return new Field<>(key, getter::apply, ConfigurationSection::getString, setter);
    }

    static <T, B> Field<T, B, Boolean> bool(
            String key, boolean fallback, Function<T, Boolean> getter, BiConsumer<B, Boolean> setter) {
        return new Field<>(key, getter::apply, (section, path) -> section.getBoolean(path, fallback), setter);
    }

    /**
     * A field that is written but read by the codec's caller, such as a reference that can only be resolved once every
     * entity is loaded.
     */
    static <T, B> Field<T, B, Object> writeOnly(String key, Function<T, @Nullable Object> writer) {
        return new Field<>(key, writer, ConfigurationSection::get, null);
    }
}
