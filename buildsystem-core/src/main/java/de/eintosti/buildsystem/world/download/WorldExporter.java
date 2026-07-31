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

import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.collection.ListTag;
import dev.dewy.nbt.tags.primitive.StringTag;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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

    /**
     * Runtime and leftover files. {@code level.dat_old} matters most: it is an untouched copy of the original, so
     * leaving it in would hand back the very metadata the exported {@code level.dat} has been stripped of.
     */
    private static final Set<String> EXCLUDED_FILES =
            Set.of("session.lock", "uid.dat", "paper-world.yml", "level.dat_old", ".DS_Store");

    private static final Set<String> VANILLA_DIMENSIONS = Set.of("overworld", "the_nether", "the_end");
    private static final Pattern UNSAFE_NAME_CHARACTERS = Pattern.compile("[^A-Za-z0-9._-]");

    /**
     * Top-level directories that never leave the server. The main level stores every player's inventory, position and
     * statistics, so exporting it would hand out the whole server's player data; {@code datapacks/bukkit} is the
     * server-side pack a client has no use for.
     */
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of("players", "playerdata", "stats", "advancements");

    /**
     * Datapacks the client cannot resolve, dropped from both the folder and the enabled list in {@code level.dat}.
     */
    private static final Set<String> SERVER_DATAPACKS = Set.of("bukkit", "file/bukkit", "paper", "file/paper");

    /**
     * Server-identifying tags removed from the exported {@code level.dat}.
     */
    private static final Set<String> SERVER_METADATA = Set.of("ServerBrands", "Bukkit.Version", "WasModded");

    private WorldExporter() {}

    /**
     * Writes {@code worldFolder} to {@code target} as a zipped single-player save.
     *
     * @param worldName The world's name, used for the archive's root directory and the save's displayed name
     * @param worldFolder The world's folder on disk
     * @param defaultLevelFolder The main level's folder, the source of the {@code level.dat} a dimension world lacks
     * @param target The archive to write
     * @param maxBytes The size the archive may not exceed
     * @throws WorldTooLargeException If the archive would grow past {@code maxBytes}
     * @throws IOException If the world cannot be read or the archive cannot be written
     */
    public static void export(String worldName, File worldFolder, File defaultLevelFolder, Path target, long maxBytes)
            throws IOException {
        Path source = worldFolder.toPath();
        if (!Files.isDirectory(source)) {
            throw new IOException("World folder does not exist: " + worldFolder.getAbsolutePath());
        }

        String rootDirectory = fileName(worldName);
        Files.createDirectories(target.getParent());

        try (CountingOutputStream counter = new CountingOutputStream(Files.newOutputStream(target), maxBytes);
                ZipOutputStream zip = new ZipOutputStream(counter)) {
            if (Files.isRegularFile(source.resolve("level.dat"))) {
                // Already a level folder: the pre-26.1 flat layout, or a world that is itself the main level. Its own
                // nether and end come along; dimensions belonging to other worlds do not.
                writeEntry(zip, rootDirectory + "/level.dat", levelDat(source, worldName));
                copyTree(zip, source, rootDirectory + "/", true);
            } else {
                writeEntry(zip, rootDirectory + "/level.dat", levelDat(defaultLevelFolder.toPath(), worldName));
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

    private static void copyTree(ZipOutputStream zip, Path root, String prefix, boolean isLevelFolder)
            throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                Path relative = root.relativize(file);
                if (isExcluded(relative, isLevelFolder)) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(prefix + relative.toString().replace(File.separatorChar, '/')));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    /**
     * Whether a file stays on the server. Beyond the runtime files every export drops, a level folder also holds the
     * server's player data and the dimensions of unrelated worlds, and its {@code level.dat} is rewritten separately.
     */
    private static boolean isExcluded(Path relative, boolean isLevelFolder) {
        if (EXCLUDED_FILES.contains(relative.getFileName().toString())) {
            return true;
        }
        if (!isLevelFolder) {
            return false;
        }
        if (relative.getNameCount() == 1 && relative.getName(0).toString().equals("level.dat")) {
            return true;
        }

        String first = relative.getName(0).toString().toLowerCase(Locale.ROOT);
        if (EXCLUDED_DIRECTORIES.contains(first)) {
            return true;
        }
        if (first.equals("datapacks")
                && relative.getNameCount() > 1
                && SERVER_DATAPACKS.contains(relative.getName(1).toString().toLowerCase(Locale.ROOT))) {
            return true;
        }
        return relative.getNameCount() > 2
                && first.equals("dimensions")
                && relative.getName(1).toString().equals("minecraft")
                && !VANILLA_DIMENSIONS.contains(relative.getName(2).toString().toLowerCase(Locale.ROOT));
    }

    private static void writeEntry(ZipOutputStream zip, String entryName, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(content);
        zip.closeEntry();
    }

    /**
     * Reads a {@code level.dat} and prepares it for the client: renamed to the exported world, so several exports do
     * not all show up under the server's level name, and stripped of the tags that only describe this server.
     */
    private static byte[] levelDat(Path levelFolder, String worldName) throws IOException {
        Path levelDat = levelFolder.resolve("level.dat");
        if (!Files.isRegularFile(levelDat)) {
            throw new IOException("No level.dat to export: " + levelDat);
        }

        Nbt nbt = new Nbt();
        CompoundTag root = nbt.fromFile(levelDat.toFile());
        CompoundTag data = root.getCompound("Data");
        if (data == null) {
            throw new IOException("level.dat has no Data compound: " + levelDat);
        }

        data.putString("LevelName", worldName);
        SERVER_METADATA.forEach(data::remove);
        removeServerDatapacks(data.getCompound("DataPacks"));

        // Nbt#toByteArray writes uncompressed; a save's level.dat is gzipped.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            nbt.toStream(root, out);
        }
        return bytes.toByteArray();
    }

    /**
     * Drops the server-side packs from the save's enabled list. A client cannot resolve them and would prompt about
     * missing packs on load.
     */
    private static void removeServerDatapacks(@Nullable CompoundTag dataPacks) {
        if (dataPacks == null) {
            return;
        }
        for (String key : List.of("Enabled", "Disabled")) {
            ListTag<StringTag> packs = dataPacks.getList(key);
            if (packs == null) {
                continue;
            }
            List<StringTag> kept = new ArrayList<>();
            for (StringTag pack : packs) {
                if (!SERVER_DATAPACKS.contains(pack.getValue().toLowerCase(Locale.ROOT))) {
                    kept.add(pack);
                }
            }
            dataPacks.putList(key, kept);
        }
    }

    /**
     * Thrown when an export outgrows the configured limit, so a single world cannot fill the server's disk.
     */
    public static final class WorldTooLargeException extends IOException {

        WorldTooLargeException(long maxBytes) {
            super("World export exceeds the configured limit of " + maxBytes + " bytes");
        }
    }

    /**
     * Counts what is written and aborts the export once it passes the limit, rather than after a full world has
     * already landed on disk.
     */
    private static final class CountingOutputStream extends OutputStream {

        private final OutputStream delegate;
        private final long maxBytes;
        private long written;

        CountingOutputStream(OutputStream delegate, long maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public void write(int b) throws IOException {
            count(1);
            delegate.write(b);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            count(length);
            delegate.write(bytes, offset, length);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        private void count(int bytes) throws IOException {
            written += bytes;
            if (written > maxBytes) {
                throw new WorldTooLargeException(maxBytes);
            }
        }
    }
}
