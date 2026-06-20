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

import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Codec for a world's custom-spawn {@link Location}, persisted as the string {@code x;y;z;yaw;pitch}.
 *
 * <p>Centralizes the encoding that previously lived in two places — the set-spawn command (formatting) and
 * {@code WorldDataImpl} (parsing) — and parses <em>defensively</em>: a blank, malformed, or legacy value yields
 * {@code null} (logged) instead of propagating a {@link NumberFormatException} into rendering or teleport code paths.
 */
@NullMarked
public final class CustomSpawn {

    private static final Logger LOGGER = Logger.getLogger(CustomSpawn.class.getName());
    private static final String DELIMITER = ";";
    private static final int EXPECTED_PARTS = 5;

    private CustomSpawn() {}

    /**
     * Formats a location as the persisted {@code x;y;z;yaw;pitch} string.
     *
     * @param location The location to encode
     * @return The encoded string, round-trippable by {@link #parse(World, String)}
     */
    public static String format(Location location) {
        return location.getX()
                + DELIMITER
                + location.getY()
                + DELIMITER
                + location.getZ()
                + DELIMITER
                + location.getYaw()
                + DELIMITER
                + location.getPitch();
    }

    /**
     * Parses a persisted custom-spawn string into a {@link Location} in the given world.
     *
     * @param world The world the location belongs to, or {@code null} if it is not currently loaded
     * @param raw The stored {@code x;y;z;yaw;pitch} string; a blank string means "no custom spawn"
     * @return The parsed location, or {@code null} when {@code raw} is blank or cannot be parsed
     */
    public static @Nullable Location parse(@Nullable World world, String raw) {
        if (raw.isBlank()) {
            return null;
        }

        String[] parts = raw.split(DELIMITER);
        if (parts.length != EXPECTED_PARTS) {
            LOGGER.warning("Ignoring malformed custom spawn '" + raw + "': expected " + EXPECTED_PARTS
                    + " ';'-separated values but found " + parts.length);
            return null;
        }

        try {
            return new Location(
                    world,
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Float.parseFloat(parts[3]),
                    Float.parseFloat(parts[4]));
        } catch (NumberFormatException e) {
            LOGGER.warning("Ignoring malformed custom spawn '" + raw + "': " + e.getMessage());
            return null;
        }
    }
}
