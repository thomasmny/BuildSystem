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
package de.eintosti.buildsystem.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StringUtils {

    private StringUtils() {}

    /**
     * Derives a stable, unique lower-case id from a human-entered display name: lower-cased, runs of non-alphanumeric
     * characters collapsed to single underscores, and leading/trailing underscores trimmed, falling back to
     * {@code fallback} when nothing usable remains. A numeric suffix ({@code _2}, {@code _3}, ...) is appended until
     * {@code taken} reports the candidate is free, so two display names can never collapse onto the same id.
     *
     * @param displayName The human-entered name to slugify
     * @param fallback The base id to use when {@code displayName} has no usable characters
     * @param taken Tests whether a candidate id is already in use
     * @return A unique id not reported as taken by {@code taken}
     */
    public static String uniqueId(String displayName, String fallback, Predicate<String> taken) {
        String base = displayName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = fallback;
        }
        String id = base;
        int suffix = 2;
        while (taken.test(id)) {
            id = base + "_" + suffix++;
        }
        return id;
    }

    /**
     * Formats a given time in milliseconds to a human-readable string.
     *
     * @param millis The time in milliseconds to format
     * @return A formatted string representing the date and time
     */
    public static String formatTime(long millis, String dateFormat) {
        Date date = new Date(millis);
        DateFormat formatter = new SimpleDateFormat(dateFormat + " HH:mm:ss");
        return formatter.format(date);
    }
}
