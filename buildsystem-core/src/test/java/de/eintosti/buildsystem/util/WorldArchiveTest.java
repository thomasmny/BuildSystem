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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Backups must stay restorable across versions: the archives in {@code backups/} were made by the zip4j code this
 * replaced, one by the local storage (which put the world folder at the archive root) and one by S3/SFTP (which did
 * not).
 */
@NullMarked
class WorldArchiveTest {

    @TempDir
    Path tempDir;

    private Path resource(String name) throws IOException {
        Path copy = tempDir.resolve(name);
        try (InputStream in = Objects.requireNonNull(getClass().getResourceAsStream("/backups/" + name), name)) {
            Files.copy(in, copy);
        }
        return copy;
    }

    private Path world(String name) throws IOException {
        Path world = tempDir.resolve(name);
        Files.createDirectories(world.resolve("region"));
        Files.writeString(world.resolve("level.dat"), "level-data");
        Files.writeString(world.resolve("region").resolve("r.0.0.mca"), "region-data");
        Files.writeString(world.resolve("uid.dat"), "uid");
        Files.writeString(world.resolve("session.lock"), "lock");
        return world;
    }

    private static void assertRestored(Path target) throws IOException {
        assertEquals("level-data", Files.readString(target.resolve("level.dat")));
        assertEquals("region-data", Files.readString(target.resolve("region").resolve("r.0.0.mca")));
    }

    @Test
    void writingIntoAMissingTempDirectory_createsIt() throws IOException {
        Path archive = tempDir.resolve(".tmp_backup_downloads").resolve("backup.zip");

        WorldArchive.write(world("arena"), null, archive);

        assertTrue(Files.isRegularFile(archive));
    }

    @Test
    void zip4jLocalBackup_restoresIntoTheWorldFolderItself() throws IOException {
        Path target = tempDir.resolve("restored");
        File archive = resource("zip4j-local.zip").toFile();

        WorldArchive.validate(archive, target.toFile());
        WorldArchive.extract(archive, target.toFile());

        assertRestored(target);
        assertFalse(Files.exists(target.resolve("legacy")), "the old archive root must not become a subfolder");
    }

    @Test
    void zip4jRemoteBackup_restores() throws IOException {
        Path target = tempDir.resolve("restored");
        File archive = resource("zip4j-remote.zip").toFile();

        WorldArchive.validate(archive, target.toFile());
        WorldArchive.extract(archive, target.toFile());

        assertRestored(target);
    }

    @Test
    void writtenArchive_restoresAndLeavesOutTheSessionLock() throws IOException {
        Path archive = tempDir.resolve("backup.zip");
        WorldArchive.write(world("source"), null, archive);
        Path target = tempDir.resolve("restored");

        WorldArchive.validate(archive.toFile(), target.toFile());
        WorldArchive.extract(archive.toFile(), target.toFile());

        assertRestored(target);
        assertTrue(Files.exists(target.resolve("uid.dat")));
        assertFalse(Files.exists(target.resolve("session.lock")));
    }

    @Test
    void writtenArchive_namesEntriesWithForwardSlashes() throws IOException {
        Path archive = tempDir.resolve("backup.zip");
        WorldArchive.write(world("source"), null, archive);

        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertTrue(zip.stream().map(ZipEntry::getName).anyMatch("region/r.0.0.mca"::equals));
        }
    }

    @Test
    void writingAMissingWorldFolder_fails() {
        // A silently empty archive would be worse than no backup.
        assertThrows(
                IOException.class,
                () -> WorldArchive.write(tempDir.resolve("missing"), null, tempDir.resolve("backup.zip")));
    }

    @Test
    void writtenArchive_leavesOutTheExcludedSubtree() throws IOException {
        Path source = world("main");
        Path nested = source.resolve("dimensions").resolve("minecraft").resolve("other");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("level.dat"), "other world");
        Path archive = tempDir.resolve("backup.zip");

        WorldArchive.write(source, source.resolve("dimensions"), archive);
        Path target = tempDir.resolve("restored");
        WorldArchive.extract(archive.toFile(), target.toFile());

        assertRestored(target);
        assertFalse(Files.exists(target.resolve("dimensions")));
    }

    @Test
    void entryEscapingTheWorldFolder_isRefusedBeforeAnythingIsWritten() throws IOException {
        Path archive = tempDir.resolve("evil.zip");
        try (OutputStream out = Files.newOutputStream(archive);
                ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("../escaped.txt"));
            zip.write("x".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Path target = tempDir.resolve("restored");

        assertThrows(IOException.class, () -> WorldArchive.validate(archive.toFile(), target.toFile()));
        assertThrows(IOException.class, () -> WorldArchive.extract(archive.toFile(), target.toFile()));
        assertFalse(Files.exists(tempDir.resolve("escaped.txt")));
    }

    @Test
    void emptyOrTruncatedArchive_isRefused() throws IOException {
        Path empty = tempDir.resolve("empty.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(empty))) {
            zip.finish();
        }
        Path truncated = tempDir.resolve("truncated.zip");
        byte[] whole = Files.readAllBytes(resource("zip4j-remote.zip"));
        Files.write(truncated, Arrays.copyOf(whole, whole.length / 2));

        assertThrows(IOException.class, () -> WorldArchive.validate(empty.toFile(), tempDir.toFile()));
        assertThrows(IOException.class, () -> WorldArchive.validate(truncated.toFile(), tempDir.toFile()));
    }
}
