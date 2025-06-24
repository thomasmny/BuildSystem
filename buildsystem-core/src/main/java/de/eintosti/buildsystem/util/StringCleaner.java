/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
import de.eintosti.buildsystem.config.ConfigValues;
import java.util.Arrays;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class StringCleaner {

    private static final BuildSystemPlugin PLUGIN = JavaPlugin.getPlugin(BuildSystemPlugin.class);

    public static final String INVALID_NAME_CHARACTERS = "[^A-Za-z\\d/_-]";

    private StringCleaner() {
    }

    private static String getConfigInvalidNameCharacters() {
        return PLUGIN.getConfigValues().getInvalidNameCharacters();
    }

    /**
     * Checks if the input string contains any invalid characters as defined by {@link #INVALID_NAME_CHARACTERS} and {@link ConfigValues#getInvalidNameCharacters()}.
     *
     * @param input The input string to check
     * @return {@code true} if the input contains invalid characters, {@code false} otherwise
     */
    public static boolean hasInvalidNameCharacters(String input) {
        return Arrays.stream(input.split("")).anyMatch(c -> c.matches(INVALID_NAME_CHARACTERS) || c.matches(getConfigInvalidNameCharacters()));
    }

    /**
     * Finds the first invalid character in the input string based on the defined invalid characters.
     *
     * @param input The input string to check for invalid characters
     * @return The first invalid character found, or {@code null} if no invalid characters are present
     */
    @Nullable
    public static String firstInvalidChar(String input) {
        return Arrays.stream(input.split(""))
                .filter(c -> c.matches(INVALID_NAME_CHARACTERS) || c.matches(getConfigInvalidNameCharacters()))
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
        return input
                .replaceAll(INVALID_NAME_CHARACTERS, "")
                .replaceAll(getConfigInvalidNameCharacters(), "")
                .replace(" ", "_")
                .trim();
    }
}
