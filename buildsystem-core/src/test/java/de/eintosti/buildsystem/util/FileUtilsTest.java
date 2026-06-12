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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Characterization tests for the file primitives that world deletion, renaming and backups are built on.
 */
@NullMarked
class FileUtilsTest {

    @TempDir
    Path tempDir;

    private File createWorldLikeDirectory(String name) throws IOException {
        Path source = tempDir.resolve(name);
        Files.createDirectories(source.resolve("region"));
        Files.writeString(source.resolve("level.dat"), "level");
        Files.writeString(source.resolve("region").resolve("r.0.0.mca"), "region-data");
        Files.writeString(source.resolve("session.lock"), "lock");
        Files.writeString(source.resolve("uid.dat"), "uid");
        return source.toFile();
    }

    @Test
    void copy_replicatesNestedStructure() throws IOException {
        File source = createWorldLikeDirectory("source");
        File target = tempDir.resolve("target").toFile();

        FileUtils.copy(source, target);

        assertTrue(new File(target, "level.dat").isFile());
        assertTrue(new File(target, "region/r.0.0.mca").isFile());
        assertEquals(
                "region-data",
                Files.readString(target.toPath().resolve("region").resolve("r.0.0.mca")));
    }

    @Test
    void copy_skipsServerInternalFiles() throws IOException {
        File source = createWorldLikeDirectory("source");
        File target = tempDir.resolve("target").toFile();

        FileUtils.copy(source, target);

        assertFalse(new File(target, "session.lock").exists());
        assertFalse(new File(target, "uid.dat").exists());
    }

    @Test
    void deleteDirectory_removesNestedTree() throws IOException {
        File source = createWorldLikeDirectory("doomed");

        FileUtils.deleteDirectory(source);

        assertFalse(source.exists());
    }

    @Test
    void deleteDirectory_missingDirectoryThrows() {
        File missing = tempDir.resolve("missing").toFile();

        assertThrows(IOException.class, () -> FileUtils.deleteDirectory(missing));
    }

    @Test
    void getDirectoryCreation_returnsPlausibleTimestamp() throws IOException {
        File source = createWorldLikeDirectory("created");

        long creation = FileUtils.getDirectoryCreation(source);

        assertTrue(creation > 0);
        assertTrue(creation <= System.currentTimeMillis());
    }

    @Test
    void resolve_createsMissingDirectories() {
        Path resolved = FileUtils.resolve(tempDir.resolve("parent"), "child");

        assertTrue(Files.isDirectory(resolved));
        assertEquals(tempDir.resolve("parent").resolve("child"), resolved);
    }
}
