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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class StringCleaner {

    public static final String INVALID_NAME_CHARACTERS = "[^A-Za-z\\d_-]";
    public static final String DEFAULT_INVALID_CHARACTERS = "^\b$";

    private static final Pattern INVALID_NAME_PATTERN = Pattern.compile(INVALID_NAME_CHARACTERS);

    /** Windows device names that cannot back a directory regardless of extension. */
    private static final Set<String> RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1",
            "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private StringCleaner() {}

    public static boolean hasInvalidNameCharacters(String input, String configuredPattern) {
        Pattern configured = Pattern.compile(configuredPattern);
        return Arrays.stream(input.split(""))
                .anyMatch(c -> INVALID_NAME_PATTERN.matcher(c).matches()
                        || configured.matcher(c).matches());
    }

    public static @Nullable String firstInvalidChar(String input, String configuredPattern) {
        Pattern configured = Pattern.compile(configuredPattern);
        return Arrays.stream(input.split(""))
                .filter(c -> INVALID_NAME_PATTERN.matcher(c).matches()
                        || configured.matcher(c).matches())
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
     * Checks whether a name is reserved or degenerate as a directory name. {@link #isPathEscape} already rejects names
     * that traverse out of the container; this rejects ones that stay inside it but still cannot back a world folder:
     * the current/parent aliases ({@code .}/{@code ..}), the empty name, and the Windows device names (CON, PRN, AUX,
     * NUL, COM1-9, LPT1-9) that are unusable even on a server that later moves between platforms.
     *
     * @param name the candidate world or folder name
     * @return {@code true} if the name must not be used
     */
    public static boolean isReservedName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.equals(".") || trimmed.equals("..")) {
            return true;
        }
        return RESERVED_NAMES.contains(trimmed.toUpperCase(Locale.ROOT));
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
