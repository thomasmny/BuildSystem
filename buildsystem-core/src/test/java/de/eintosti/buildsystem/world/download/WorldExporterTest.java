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
package de.eintosti.buildsystem.world.download;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the layout the client expects: a Paper 26.1+ dimension world has to come back out as a save with a
 * {@code level.dat} and its data under the overworld.
 */
@NullMarked
class WorldExporterTest {

    private static final long MAX_BYTES = 64L * 1024 * 1024;

    @TempDir
    Path tempDir;

    @Test
    void exportsDimensionWorldAsSinglePlayerSave() throws IOException {
        Path dimension = tempDir.resolve("dimensions/minecraft/lobby");
        Files.createDirectories(dimension.resolve("region"));
        Files.writeString(dimension.resolve("region/r.0.0.mca"), "region-data");
        Files.createDirectories(dimension.resolve("data/minecraft"));
        Files.writeString(dimension.resolve("data/minecraft/world_gen_settings.dat"), "gen");
        Files.writeString(dimension.resolve("session.lock"), "lock");
        Files.writeString(dimension.resolve("paper-world.yml"), "paper: config");

        File level = levelFolder("MainLevel");
        Path archive = tempDir.resolve("out.zip");
        WorldExporter.export(
                "lobby", dimension.toFile(), level, archive, MAX_BYTES, WorldExporter.ExportProgress.IGNORED);

        Map<String, byte[]> entries = read(archive);
        assertEquals("region-data", new String(entries.get("lobby/dimensions/minecraft/overworld/region/r.0.0.mca")));
        assertTrue(entries.containsKey("lobby/dimensions/minecraft/overworld/data/minecraft/world_gen_settings.dat"));
        assertEquals("lobby", levelName(entries.get("lobby/level.dat")));
        assertFalse(entries.containsKey("lobby/dimensions/minecraft/overworld/session.lock"));
        assertFalse(entries.containsKey("lobby/dimensions/minecraft/overworld/paper-world.yml"));
    }

    @Test
    void keepsFlatWorldLayoutAndDropsForeignDimensions() throws IOException {
        Path world = tempDir.resolve("legacy");
        Files.createDirectories(world.resolve("region"));
        Files.write(world.resolve("level.dat"), gzip(levelDat("legacy")));
        Files.writeString(world.resolve("region/r.0.0.mca"), "region-data");
        Files.createDirectories(world.resolve("dimensions/minecraft/other"));
        Files.writeString(world.resolve("dimensions/minecraft/other/marker"), "another world");
        Files.createDirectories(world.resolve("dimensions/minecraft/the_nether"));
        Files.writeString(world.resolve("dimensions/minecraft/the_nether/marker"), "own nether");

        Path archive = tempDir.resolve("legacy.zip");
        WorldExporter.export(
                "legacy",
                world.toFile(),
                levelFolder("MainLevel"),
                archive,
                MAX_BYTES,
                WorldExporter.ExportProgress.IGNORED);

        Map<String, byte[]> entries = read(archive);
        assertTrue(entries.containsKey("legacy/region/r.0.0.mca"));
        assertTrue(entries.containsKey("legacy/dimensions/minecraft/the_nether/marker"));
        assertFalse(entries.containsKey("legacy/dimensions/minecraft/other/marker"));
    }

    @Test
    void findsTheLevelDatWhenPaperReportsTheOverworldDimensionFolder() throws IOException {
        Path level = tempDir.resolve("level-Main");
        Files.createDirectories(level);
        Files.write(level.resolve("level.dat"), gzip(levelDat("Main")));
        Path overworld = level.resolve("dimensions/minecraft/overworld");
        Files.createDirectories(overworld);

        Path dimension = level.resolve("dimensions/minecraft/lobby");
        Files.createDirectories(dimension.resolve("region"));
        Files.writeString(dimension.resolve("region/r.0.0.mca"), "region-data");

        Path archive = tempDir.resolve("reported-dimension.zip");
        WorldExporter.export(
                "lobby",
                dimension.toFile(),
                overworld.toFile(),
                archive,
                MAX_BYTES,
                WorldExporter.ExportProgress.IGNORED);

        assertEquals("lobby", levelName(read(archive).get("lobby/level.dat")));
    }

    @Test
    void reportsProgressUpToTheTotal() throws IOException {
        Path dimension = tempDir.resolve("dimensions/minecraft/progress");
        Files.createDirectories(dimension.resolve("region"));
        for (int region = 0; region < 4; region++) {
            Files.writeString(dimension.resolve("region/r." + region + ".0.mca"), "x".repeat(1000));
        }

        List<Long> packed = new ArrayList<>();
        AtomicLong total = new AtomicLong();
        WorldExporter.export(
                "progress",
                dimension.toFile(),
                levelFolder("MainLevel"),
                tempDir.resolve("progress.zip"),
                MAX_BYTES,
                (packedBytes, totalBytes) -> {
                    packed.add(packedBytes);
                    total.set(totalBytes);
                });

        assertEquals(4000L, total.get());
        assertEquals(List.of(0L, 1000L, 2000L, 3000L, 4000L), packed);
    }

    @Test
    void barFillsAndAnimatesWithinTheFilledPart() {
        assertEquals(0, ExportProgressBar.percent(0));
        assertEquals(50, ExportProgressBar.percent(0.5));
        assertEquals(100, ExportProgressBar.percent(1.5), "an over-full fraction is clamped");
        assertEquals(0, ExportProgressBar.percent(Double.NaN), "an unknown total reads as zero, not NaN");

        assertFalse(ExportProgressBar.bar(0, 0).contains("&f"), "an empty bar has nothing to highlight");
        assertTrue(ExportProgressBar.bar(0.5, 0).contains("&f"), "a half-full bar carries the sweep");
        assertNotEquals(
                ExportProgressBar.bar(0.5, 0),
                ExportProgressBar.bar(0.5, 1),
                "the sweep moves between frames at the same progress");
        assertNotEquals(ExportProgressBar.spinner(0), ExportProgressBar.spinner(1));
        assertEquals(
                3,
                ExportProgressBar.percentText(0.07).length(),
                "the percentage is padded so the centered line does not jump a digit's width");
        assertEquals(
                ExportProgressBar.percentText(0.07).length(),
                ExportProgressBar.percentText(1).length());
    }

    private File levelFolder(String levelName) throws IOException {
        Path level = tempDir.resolve("level-" + levelName);
        Files.createDirectories(level);
        Files.write(level.resolve("level.dat"), gzip(levelDat(levelName)));
        return level.toFile();
    }

    /**
     * A minimal {@code level.dat} body: enough NBT around the {@code LevelName} tag for the rename to have to find it.
     */
    private static byte[] levelDat(String levelName) {
        byte[] name = levelName.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {10, 0, 0, 10, 0, 4, 'D', 'a', 't', 'a'});
        out.writeBytes(new byte[] {8, 0, 9, 'L', 'e', 'v', 'e', 'l', 'N', 'a', 'm', 'e'});
        out.write(name.length >> 8);
        out.write(name.length);
        out.writeBytes(name);
        out.writeBytes(new byte[] {3, 0, 4, 'T', 'i', 'm', 'e', 0, 0, 0, 7, 0, 0});
        return out.toByteArray();
    }

    private static String levelName(byte[] compressed) throws IOException {
        byte[] nbt;
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            nbt = in.readAllBytes();
        }
        String text = new String(nbt, StandardCharsets.ISO_8859_1);
        int header = text.indexOf("LevelName") + "LevelName".length();
        int length = ((nbt[header] & 0xFF) << 8) | (nbt[header + 1] & 0xFF);
        return new String(nbt, header + 2, length, StandardCharsets.UTF_8);
    }

    private static byte[] gzip(byte[] raw) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream out = new GZIPOutputStream(bytes)) {
            out.write(raw);
        }
        return bytes.toByteArray();
    }

    private static Map<String, byte[]> read(Path archive) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), zip.readAllBytes());
            }
        }
        return entries;
    }
}
