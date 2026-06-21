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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
    void deleteDirectory_failedDeleteIsReportedNotSwallowed() throws IOException {
        File world = createWorldLikeDirectory("locked");
        Path region = world.toPath().resolve("region");

        // Clear write permission on the directory so its contents cannot be deleted. Skip where the platform or user
        // does not enforce it (e.g. running as root, or a filesystem ignoring the bit) so the test stays deterministic.
        assumeTrue(region.toFile().setWritable(false, false), "could not make directory read-only");
        try {
            assumeTrue(!Files.isWritable(region), "directory write permission is not enforced here");
            assertThrows(IOException.class, () -> FileUtils.deleteDirectory(world));
        } finally {
            region.toFile().setWritable(true, false);
        }
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

    private Map<String, String> readZipEntries(byte[] zipped) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipped))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes()));
            }
        }
        return entries;
    }

    @Test
    void zipDirectoryToMemory_archivesEveryFileWithForwardSlashPaths() throws IOException {
        File world = createWorldLikeDirectory("source");

        byte[] zipped = FileUtils.zipDirectoryToMemory(world.toPath());

        Map<String, String> entries = readZipEntries(zipped);
        assertEquals("level", entries.get("level.dat"));
        assertEquals("region-data", entries.get("region/r.0.0.mca"));
        assertEquals(4, entries.size(), "Every regular file should be archived");
    }

    @Test
    void zipDirectoryToMemory_missingDirectoryThrows() {
        Path missing = tempDir.resolve("missing");

        // Failure must surface instead of producing a silently-truncated archive.
        assertThrows(IOException.class, () -> FileUtils.zipDirectoryToMemory(missing));
    }
}
