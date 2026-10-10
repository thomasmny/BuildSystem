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

import de.eintosti.buildsystem.player.LogoutLocation;
import java.util.Arrays;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Codec for a player's {@link LogoutLocation}, persisted as the string {@code world:x:y:z:yaw:pitch} produced by
 * {@link LogoutLocation#toString()}. A namespaced world ({@code maps:lobby}) adds one more separator, so the
 * coordinates are read from the end.
 *
 * <p>Centralizes the parsing that previously lived inline in the player storage and fails <em>soft</em>: a blank or
 * malformed value yields {@code null} (logged) instead of propagating a {@link NumberFormatException} into the load
 * path, so a single corrupt entry costs the player their saved location, not their whole record.
 */
@NullMarked
public final class LogoutLocationCodec {

    private static final Logger LOGGER = Logger.getLogger(LogoutLocationCodec.class.getName());
    private static final String DELIMITER = ":";
    private static final int COORDINATE_PARTS = 5;

    private LogoutLocationCodec() {}

    /**
     * Formats a logout location as the persisted {@code world:x:y:z:yaw:pitch} string.
     *
     * @param logoutLocation The location to encode
     * @return The encoded string, round-trippable by {@link #parse(String)}
     */
    public static String format(LogoutLocation logoutLocation) {
        return logoutLocation.toString();
    }

    /**
     * Parses a persisted logout-location string.
     *
     * @param raw The stored {@code world:x:y:z:yaw:pitch} string, or {@code null} when none was stored
     * @return The parsed location, or {@code null} when {@code raw} is blank or cannot be parsed
     */
    public static @Nullable LogoutLocation parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] parts = raw.split(DELIMITER);
        int worldParts = parts.length - COORDINATE_PARTS;
        if (worldParts != 1 && worldParts != 2) {
            LOGGER.warning("Ignoring malformed logout location '" + raw + "': expected a world and " + COORDINATE_PARTS
                    + " ':'-separated values but found " + parts.length + " values");
            return null;
        }

        try {
            return new LogoutLocation(
                    String.join(DELIMITER, Arrays.copyOfRange(parts, 0, worldParts)),
                    Double.parseDouble(parts[worldParts]),
                    Double.parseDouble(parts[worldParts + 1]),
                    Double.parseDouble(parts[worldParts + 2]),
                    Float.parseFloat(parts[worldParts + 3]),
                    Float.parseFloat(parts[worldParts + 4]));
        } catch (NumberFormatException e) {
            LOGGER.warning("Ignoring malformed logout location '" + raw + "': " + e.getMessage());
            return null;
        }
    }
}
