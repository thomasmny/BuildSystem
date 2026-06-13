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
package de.eintosti.buildsystem.world;

import static org.junit.jupiter.api.Assertions.*;

import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins the export primitive that {@code /worlds saveTemplate} relies on: copying a live world directory into
 * {@code templates/} via {@link FileUtils#copy} must drop {@code uid.dat}/{@code session.lock}, recurse
 * subdirectories, and the path-escape guard must reject names that climb out of the templates directory.
 */
@NullMarked
class TemplateExportTest {

    @TempDir
    Path tempDir;

    @Test
    void copyExcludesUidAndSessionLock() throws Exception {
        Path source = Files.createDirectory(tempDir.resolve("world"));
        Files.createDirectory(source.resolve("region"));
        Files.writeString(source.resolve("region").resolve("r.0.0.mca"), "region-data");
        Files.writeString(source.resolve("level.dat"), "level-data");
        Files.writeString(source.resolve("uid.dat"), "uid");
        Files.writeString(source.resolve("session.lock"), "lock");

        Path target = tempDir.resolve("templates").resolve("my-template");
        FileUtils.copy(source.toFile(), target.toFile());

        assertTrue(Files.exists(target.resolve("level.dat")), "level.dat should be copied");
        assertTrue(Files.exists(target.resolve("region").resolve("r.0.0.mca")), "region file should be copied");
        assertFalse(Files.exists(target.resolve("uid.dat")), "uid.dat should be excluded");
        assertFalse(Files.exists(target.resolve("session.lock")), "session.lock should be excluded");
    }

    @Test
    void copyRecursesNestedSubdirectories() throws Exception {
        Path source = Files.createDirectory(tempDir.resolve("world"));
        Path nested = source.resolve("data").resolve("playerdata");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("player.dat"), "player");

        Path target = tempDir.resolve("templates").resolve("nested-template");
        FileUtils.copy(source.toFile(), target.toFile());

        assertTrue(
                Files.exists(target.resolve("data").resolve("playerdata").resolve("player.dat")),
                "nested file should be copied recursively");
    }

    @Test
    void pathEscapeGuardRejectsTraversal() {
        File templatesDir = tempDir.resolve("templates").toFile();
        assertTrue(StringCleaner.isPathEscape(templatesDir, new File(templatesDir, "../evil")));
    }

    @Test
    void pathEscapeGuardAllowsDirectChild() {
        File templatesDir = tempDir.resolve("templates").toFile();
        assertFalse(StringCleaner.isPathEscape(templatesDir, new File(templatesDir, "safe-template")));
    }
}
