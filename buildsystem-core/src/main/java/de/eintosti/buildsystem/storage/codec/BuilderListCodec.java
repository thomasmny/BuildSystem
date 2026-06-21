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
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Codec for a world's trusted-builder list, persisted as a single {@code ;}-joined string of {@link Builder#toString()}
 * entries (each {@code <uuid>,<name>}).
 *
 * <p>Centralizes the encoding that previously lived inline in the world storage and parses <em>defensively</em>: a
 * blank value yields an empty list, and an individual entry that cannot be deserialized is skipped (logged) rather than
 * aborting the whole list — one corrupt builder never drops the others.
 */
@NullMarked
public final class BuilderListCodec {

    private static final Logger LOGGER = Logger.getLogger(BuilderListCodec.class.getName());
    private static final String DELIMITER = ";";

    private BuilderListCodec() {}

    /**
     * Formats a collection of builders as the persisted {@code ;}-joined string.
     *
     * @param builders The builders to encode
     * @return The encoded string, round-trippable by {@link #parse(String)}
     */
    public static String format(Collection<Builder> builders) {
        return builders.stream().map(Builder::toString).collect(Collectors.joining(DELIMITER));
    }

    /**
     * Parses a persisted builder-list string into its builders.
     *
     * @param raw The stored {@code ;}-joined string, or {@code null} when no builders were stored
     * @return A mutable list of the parsed builders; empty when {@code raw} is blank or holds no valid entries
     */
    public static List<Builder> parse(@Nullable String raw) {
        List<Builder> builders = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return builders;
        }

        for (String entry : raw.split(DELIMITER)) {
            Builder builder = Builder.deserialize(entry);
            if (builder == null) {
                LOGGER.warning("Ignoring malformed builder entry '" + entry + "'");
                continue;
            }
            builders.add(builder);
        }
        return builders;
    }
}
