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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.NullMarked;

/**
 * Packs a server-side world into an archive that can be dropped into a client's {@code saves} directory.
 *
 * <p>Since Paper 26.1 a server world is a dimension of the main level ({@code <level>/dimensions/minecraft/<name>}) and
 * has no {@code level.dat} of its own, so its folder alone is not a save the client can open. The export rebuilds the
 * missing scaffolding: the dimension becomes the save's overworld and the main level's {@code level.dat} is copied in
 * under the exported world's name. Worlds still stored in the pre-26.1 flat layout already are a save and are packed
 * as they are.
 */
@NullMarked
public final class WorldExporter {

    private static final Set<String> EXCLUDED_FILES = Set.of("session.lock", "uid.dat", "paper-world.yml");
    private static final Set<String> VANILLA_DIMENSIONS = Set.of("overworld", "the_nether", "the_end");
    private static final Pattern UNSAFE_NAME_CHARACTERS = Pattern.compile("[^A-Za-z0-9._-]");

    /**
     * The {@code TAG_String("LevelName")} header: tag id, then the 2-byte length and bytes of the tag name.
     */
    private static final byte[] LEVEL_NAME_TAG = {8, 0, 9, 'L', 'e', 'v', 'e', 'l', 'N', 'a', 'm', 'e'};

    private WorldExporter() {}

    /**
     * Writes {@code worldFolder} to {@code target} as a zipped single-player save.
     *
     * @param worldName The world's name, used for the archive's root directory and the save's displayed name
     * @param worldFolder The world's folder on disk
     * @param defaultLevelFolder The main level's folder, the source of the {@code level.dat} a dimension world lacks
     * @param target The archive to write
     * @throws IOException If the world cannot be read or the archive cannot be written
     */
    public static void export(String worldName, File worldFolder, File defaultLevelFolder, Path target)
            throws IOException {
        Path source = worldFolder.toPath();
        if (!Files.isDirectory(source)) {
            throw new IOException("World folder does not exist: " + worldFolder.getAbsolutePath());
        }

        String rootDirectory = fileName(worldName);
        Files.createDirectories(target.getParent());

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            if (Files.isRegularFile(source.resolve("level.dat"))) {
                // Already a level folder: the pre-26.1 flat layout, or a world that is itself the main level. Nested
                // dimensions belong to other worlds and are left behind.
                copyTree(zip, source, rootDirectory + "/", true);
            } else {
                writeEntry(zip, rootDirectory + "/level.dat", levelDat(defaultLevelFolder, worldName));
                copyTree(zip, source, rootDirectory + "/dimensions/minecraft/overworld/", false);
            }
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    /**
     * {@return {@code name} reduced to characters that are safe in an archive entry and an HTTP header}
     */
    public static String fileName(String name) {
        String sanitized = UNSAFE_NAME_CHARACTERS.matcher(name).replaceAll("_");
        return sanitized.isBlank() ? "world" : sanitized;
    }

    private static void copyTree(ZipOutputStream zip, Path root, String prefix, boolean skipNestedDimensions)
            throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                Path relative = root.relativize(file);
                if (isExcluded(relative, skipNestedDimensions)) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(prefix + relative.toString().replace(File.separatorChar, '/')));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private static boolean isExcluded(Path relative, boolean skipNestedDimensions) {
        if (EXCLUDED_FILES.contains(relative.getFileName().toString())) {
            return true;
        }
        if (!skipNestedDimensions || relative.getNameCount() < 3) {
            return false;
        }
        return relative.getName(0).toString().equals("dimensions")
                && relative.getName(1).toString().equals("minecraft")
                && !VANILLA_DIMENSIONS.contains(relative.getName(2).toString().toLowerCase(Locale.ROOT));
    }

    private static void writeEntry(ZipOutputStream zip, String entryName, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(content);
        zip.closeEntry();
    }

    /**
     * Reads the main level's {@code level.dat} and renames the save to the exported world, so a player who exports
     * several worlds does not end up with a list of identically named saves.
     */
    private static byte[] levelDat(File defaultLevelFolder, String worldName) throws IOException {
        Path levelDat = defaultLevelFolder.toPath().resolve("level.dat");
        if (!Files.isRegularFile(levelDat)) {
            throw new IOException("Main level has no level.dat to copy: " + levelDat);
        }
        return gzip(withLevelName(gunzip(Files.readAllBytes(levelDat)), worldName));
    }

    /**
     * Replaces the value of the {@code LevelName} tag in uncompressed NBT, returning the data unchanged when the tag
     * is not found.
     *
     * <p>ponytail: a byte splice rather than an NBT parser — the tag header is self-delimiting and this is the only
     * field the export rewrites. Parse properly if a second field ever needs changing.
     */
    private static byte[] withLevelName(byte[] nbt, String worldName) {
        int header = indexOf(nbt, LEVEL_NAME_TAG);
        if (header < 0) {
            return nbt;
        }

        int lengthAt = header + LEVEL_NAME_TAG.length;
        if (lengthAt + 2 > nbt.length) {
            return nbt;
        }

        int oldLength = ((nbt[lengthAt] & 0xFF) << 8) | (nbt[lengthAt + 1] & 0xFF);
        int valueEnd = lengthAt + 2 + oldLength;
        if (valueEnd > nbt.length) {
            return nbt;
        }

        byte[] value = worldName.getBytes(StandardCharsets.UTF_8);
        if (value.length > 0xFFFF) {
            return nbt;
        }

        byte[] patched = new byte[nbt.length - oldLength + value.length];
        System.arraycopy(nbt, 0, patched, 0, lengthAt);
        patched[lengthAt] = (byte) (value.length >> 8);
        patched[lengthAt + 1] = (byte) value.length;
        System.arraycopy(value, 0, patched, lengthAt + 2, value.length);
        System.arraycopy(nbt, valueEnd, patched, lengthAt + 2 + value.length, nbt.length - valueEnd);
        return patched;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            if (Arrays.equals(haystack, i, i + needle.length, needle, 0, needle.length)) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] gunzip(byte[] compressed) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return in.readAllBytes();
        }
    }

    private static byte[] gzip(byte[] raw) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(raw.length);
        try (OutputStream out = new GZIPOutputStream(bytes)) {
            out.write(raw);
        }
        return bytes.toByteArray();
    }
}
