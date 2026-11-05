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

import de.eintosti.buildsystem.api.world.builder.Builder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Codec for a creator and a list of builders, each stored as a map with a {@code uuid} and a {@code name}.
 *
 * <p>Before 4.1 they were strings: {@code <uuid>,<name>} and a {@code ;}-joined list of those. Both still load, so an
 * entry keeps working until it is next saved. An entry in a list that is incomplete is skipped and logged, so one
 * corrupt builder never drops the others.
 */
@NullMarked
public final class BuilderCodec {

    private static final Logger LOGGER = Logger.getLogger(BuilderCodec.class.getName());
    private static final String UUID_KEY = "uuid";
    private static final String NAME = "name";
    private static final String LEGACY_DELIMITER = ";";

    private BuilderCodec() {}

    public static Map<String, Object> serialize(Builder builder) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        serialized.put(UUID_KEY, builder.getUniqueId().toString());
        serialized.put(NAME, builder.getName());
        return serialized;
    }

    public static List<Map<String, Object>> serialize(Collection<Builder> builders) {
        return builders.stream().map(BuilderCodec::serialize).toList();
    }

    /**
     * {@return the builder stored under {@code key}, or {@code null} when there is none or it is incomplete}
     */
    public static @Nullable Builder parse(ConfigurationSection section, String key) {
        ConfigurationSection fields = section.getConfigurationSection(key);
        return fields != null
                ? of(fields.getString(UUID_KEY), fields.getString(NAME))
                : Builder.deserialize(section.getString(key));
    }

    /**
     * {@return a mutable list of the builders stored under {@code key}}
     */
    public static List<Builder> parseList(ConfigurationSection section, String key) {
        List<Builder> builders = new ArrayList<>();
        if (section.isList(key)) {
            for (Map<?, ?> fields : section.getMapList(key)) {
                addIfValid(builders, of(string(fields.get(UUID_KEY)), string(fields.get(NAME))), fields);
            }
            return builders;
        }
        String legacy = section.getString(key);
        if (legacy != null && !legacy.isEmpty()) {
            for (String entry : legacy.split(LEGACY_DELIMITER)) {
                addIfValid(builders, Builder.deserialize(entry), entry);
            }
        }
        return builders;
    }

    private static void addIfValid(List<Builder> builders, @Nullable Builder builder, Object stored) {
        if (builder == null) {
            LOGGER.warning("Ignoring malformed builder entry '" + stored + "'");
            return;
        }
        builders.add(builder);
    }

    private static @Nullable Builder of(@Nullable String uuid, @Nullable String name) {
        return uuid == null || name == null ? null : Builder.of(UUID.fromString(uuid), name);
    }

    private static @Nullable String string(@Nullable Object value) {
        return value == null ? null : value.toString();
    }
}
