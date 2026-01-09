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

import de.eintosti.buildsystem.config.Config.Messages;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * Formats a given time in milliseconds to a human-readable string.
     *
     * @param millis The time in milliseconds to format
     * @return A formatted string representing the date and time
     */
    public static String formatTime(long millis) {
        Date date = new Date(millis);
        DateFormat formatter = new SimpleDateFormat(Messages.dateFormat + " HH:mm:ss");
        return formatter.format(date);
    }
}
