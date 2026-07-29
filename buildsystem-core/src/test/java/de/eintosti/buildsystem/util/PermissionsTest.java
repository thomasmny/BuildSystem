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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the single-source-of-truth property of {@link Permissions}: permission nodes belong there, not inline at the
 * call site, so a typo fails to compile instead of silently never matching.
 *
 * <p>Covers both ways of writing one inline — the whole node as a literal, and a literal suffix concatenated onto
 * {@code getArgument().getPermission()}. The second form spells a real node without ever writing
 * {@code "buildsystem.…"}, so it slipped past the literal check.
 */
class PermissionsTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");
    private static final Path PERMISSIONS = SOURCE_ROOT.resolve("de/eintosti/buildsystem/util/Permissions.java");
    private static final Pattern NODE = Pattern.compile("\"buildsystem\\.[a-z.%]*\"");
    private static final Pattern SUFFIXED_NODE = Pattern.compile("getPermission\\(\\)\\s*\\+\\s*\"[^\"]*\"");

    @Test
    @DisplayName("No permission node is written inline outside Permissions")
    void permissionNodesAreCentralized() throws IOException {
        try (Stream<Path> sources = Files.walk(SOURCE_ROOT)) {
            List<String> offenders = sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.equals(PERMISSIONS))
                    .flatMap(PermissionsTest::nodesIn)
                    .toList();

            assertTrue(
                    offenders.isEmpty(),
                    "Permission nodes must be declared in Permissions, not inline. Found: " + offenders);
        }
    }

    private static Stream<String> nodesIn(Path path) {
        String source;
        try {
            source = Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }

        Stream.Builder<String> found = Stream.builder();
        collect(NODE, source, path, found);
        collect(SUFFIXED_NODE, source, path, found);
        return found.build();
    }

    private static void collect(Pattern pattern, String source, Path path, Stream.Builder<String> found) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            found.add(path.getFileName() + ": " + matcher.group());
        }
    }
}
