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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@NullMarked
public final class StringCleaner {

    public static final String INVALID_NAME_CHARACTERS = "[^A-Za-z\\d/_-]";
    public static final String DEFAULT_INVALID_CHARACTERS = "^\b$";

    private StringCleaner() {}

    public static boolean hasInvalidNameCharacters(String input, String configuredPattern) {
        return Arrays.stream(input.split(""))
                .anyMatch(c -> c.matches(INVALID_NAME_CHARACTERS) || c.matches(configuredPattern));
    }

    @Nullable public static String firstInvalidChar(String input, String configuredPattern) {
        return Arrays.stream(input.split(""))
                .filter(c -> c.matches(INVALID_NAME_CHARACTERS) || c.matches(configuredPattern))
                .findFirst()
                .orElse(null);
    }

    public static String sanitize(String input, String configuredPattern) {
        return input.replaceAll(INVALID_NAME_CHARACTERS, "")
                .replaceAll(configuredPattern, "")
                .replace(" ", "_")
                .trim();
    }

    /**
     * Checks whether a resolved file escapes a given base directory.
     *
     * @param base the expected parent directory
     * @param resolved the file to check (must already be constructed from base + user input)
     * @return {@code true} if the resolved file is NOT under base (i.e., an escape attempt)
     */
    public static boolean isPathEscape(File base, File resolved) {
        try {
            return !resolved.getCanonicalPath().startsWith(base.getCanonicalPath() + File.separator)
                    && !resolved.getCanonicalFile().equals(base.getCanonicalFile());
        } catch (IOException e) {
            // If we can't resolve canonical paths, assume escape
            return true;
        }
    }
}
