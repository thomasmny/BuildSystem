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

import de.eintosti.buildsystem.BuildSystemPlugin;
import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class StringCleaner {

    public static final String INVALID_NAME_CHARACTERS = "[^A-Za-z\\d/_-]";
    private static final String DEFAULT_INVALID_CHARACTERS = "^\b$";

    private StringCleaner() {
    }

    /**
     * Gets the configured invalid characters pattern, falling back to the default if the plugin is not available (e.g. in tests).
     *
     * @return The invalid characters regex pattern
     */
    private static String getInvalidCharacters() {
        try {
            return BuildSystemPlugin.get().getConfigService().current().world().invalidCharacters();
        } catch (Exception e) {
            return DEFAULT_INVALID_CHARACTERS;
        }
    }

    /**
     * Checks if the input string contains any invalid characters as defined by {@link #INVALID_NAME_CHARACTERS} and the configured invalid-characters pattern.
     *
     * @param input The input string to check
     * @return {@code true} if the input contains invalid characters, {@code false} otherwise
     */
    public static boolean hasInvalidNameCharacters(String input) {
        String invalidChars = getInvalidCharacters();
        return Arrays.stream(input.split("")).anyMatch(c -> c.matches(INVALID_NAME_CHARACTERS) || c.matches(invalidChars));
    }

    /**
     * Finds the first invalid character in the input string based on the defined invalid characters.
     *
     * @param input The input string to check for invalid characters
     * @return The first invalid character found, or {@code null} if no invalid characters are present
     */
    @Nullable
    public static String firstInvalidChar(String input) {
        String invalidChars = getInvalidCharacters();
        return Arrays.stream(input.split(""))
                .filter(c -> c.matches(INVALID_NAME_CHARACTERS) || c.matches(invalidChars))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sanitizes the input string by removing invalid characters, replacing spaces with underscores, and trimming whitespace.
     *
     * @param input The input string to sanitize
     * @return A sanitized version of the input string
     */
    public static String sanitize(String input) {
        String invalidChars = getInvalidCharacters();
        return input
                .replaceAll(INVALID_NAME_CHARACTERS, "")
                .replaceAll(invalidChars, "")
                .replace(" ", "_")
                .trim();
    }
}
