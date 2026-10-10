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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

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
    void copy_unreadableSourceThrowsRatherThanReportingSuccess() throws IOException {
        File source = createWorldLikeDirectory("source");
        File target = tempDir.resolve("target").toFile();
        // A file that cannot be read stands in for any mid-copy IO failure.
        Path unreadable = source.toPath().resolve("region").resolve("r.0.0.mca");
        assumeTrue(unreadable.toFile().setReadable(false), "filesystem must support clearing the read bit");

        try {
            // A silent partial copy is what made /worlds rename destructive: it deleted the source afterwards.
            assertThrows(IOException.class, () -> FileUtils.copy(source, target));
        } finally {
            unreadable.toFile().setReadable(true);
        }
    }

    @Test
    void copy_missingSourceDirectoryThrows() {
        File missing = tempDir.resolve("missing").toFile();
        File target = tempDir.resolve("target").toFile();

        assertThrows(IOException.class, () -> FileUtils.copy(missing, target));
    }

    @Test
    void moveDirectory_movesEverythingIncludingServerFiles() throws IOException {
        File source = createWorldLikeDirectory("old");
        File target = tempDir.resolve("dimensions")
                .resolve("minecraft")
                .resolve("new")
                .toFile();

        FileUtils.moveDirectory(source, target);

        assertFalse(source.exists());
        assertEquals(
                "region-data",
                Files.readString(target.toPath().resolve("region").resolve("r.0.0.mca")));
        assertTrue(new File(target, "uid.dat").exists(), "a move keeps the world's uid");
    }

    @Test
    void moveDirectory_neverMergesIntoAnExistingDirectory() throws IOException {
        File source = createWorldLikeDirectory("old");
        File target = createWorldLikeDirectory("taken");
        Files.writeString(target.toPath().resolve("level.dat"), "other world");

        assertThrows(IOException.class, () -> FileUtils.moveDirectory(source, target));

        assertEquals("other world", Files.readString(target.toPath().resolve("level.dat")));
        assertTrue(new File(source, "level.dat").exists(), "the source must be left alone");
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

    /**
     * A running server with {@code level-name=world} whose container is the temp dir.
     */
    private MockedStatic<Bukkit> mockServer() {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(tempDir.toFile());
        World main = mock(World.class);
        when(main.getName()).thenReturn("world");
        when(main.getKey()).thenReturn(NamespacedKey.minecraft("overworld"));
        bukkit.when(Bukkit::getWorlds).thenReturn(List.of(main));
        bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);
        return bukkit;
    }

    private File dimensions() {
        return dimensions(NamespacedKey.MINECRAFT);
    }

    private File dimensions(String namespace) {
        return new File(tempDir.toFile(), "world" + File.separator + "dimensions" + File.separator + namespace);
    }

    @Test
    void worldFolder_loadedWorld_usesItsReportedFolder() {
        File reported = tempDir.resolve("wherever").resolve("custom").toFile();
        World loaded = mock(World.class);
        when(loaded.getWorldFolder()).thenReturn(reported);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("Loaded")).thenReturn(loaded);
            assertEquals(reported, FileUtils.worldFolder("Loaded"));
        }
    }

    @Test
    void worldFolder_unloadedWorld_resolvesLowercasedDimensionFolder() throws IOException {
        File dimension = new File(dimensions(), "myworld");
        Files.createDirectories(dimension.toPath());
        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertEquals(dimension, FileUtils.worldFolder("MyWorld"));
        }
    }

    @Test
    void worldFolder_notYetCreated_returnsLowercasedDimensionTarget() {
        // Template/rename destinations: nothing exists yet, so the derived dimension path is returned for the copy.
        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertEquals(new File(dimensions(), "fresh"), FileUtils.worldFolder("Fresh"));
        }
    }

    @Test
    void worldFolder_legacyFlatFolder_isUsedWhenNotMigrated() throws IOException {
        File legacy = tempDir.resolve("Old").toFile();
        Files.createDirectories(legacy.toPath());
        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertEquals(legacy, FileUtils.worldFolder("Old"));
        }
    }

    @Test
    void worldFolder_namespacedWorld_resolvesItsNamespaceFolder() {
        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertEquals(new File(dimensions("maps"), "lobby"), FileUtils.worldFolder("maps:Lobby"));
        }
    }

    @Test
    void worldFolder_namespacedWorld_ignoresFlatFolderOfTheSameName() throws IOException {
        Files.createDirectories(tempDir.resolve("Lobby"));
        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertEquals(new File(dimensions("maps"), "lobby"), FileUtils.worldFolder("maps:Lobby"));
        }
    }

    @Test
    void dimensionWorldNames_listsWorldsOfEveryValidNamespace() throws IOException {
        for (File folder : List.of(
                new File(dimensions(), "arena"),
                new File(dimensions(), "overworld"),
                new File(dimensions("maps"), "lobby"),
                new File(dimensions("maps"), "the_end"),
                new File(dimensions("Not Valid"), "hidden"))) {
            Files.createDirectories(folder.toPath().resolve("region"));
        }
        Files.createDirectories(new File(dimensions("maps"), "empty").toPath());

        try (MockedStatic<Bukkit> bukkit = mockServer()) {
            assertEquals(Set.of("arena", "maps:lobby", "maps:the_end"), new HashSet<>(FileUtils.dimensionWorldNames()));
        }
    }

    @Test
    void isWorldDirectory_acceptsDataFolder_rejectsVanillaAndEmpty() throws IOException {
        File arena = new File(dimensions(), "arena");
        Files.createDirectories(arena.toPath().resolve("region"));
        assertTrue(FileUtils.isWorldDirectory(arena));

        File overworld = new File(dimensions(), "overworld");
        Files.createDirectories(overworld.toPath().resolve("region"));
        assertFalse(FileUtils.isWorldDirectory(overworld), "vanilla dimensions must not be importable");

        File empty = new File(dimensions(), "empty");
        Files.createDirectories(empty.toPath());
        assertFalse(FileUtils.isWorldDirectory(empty), "a folder without world data is not a world");
    }
}
