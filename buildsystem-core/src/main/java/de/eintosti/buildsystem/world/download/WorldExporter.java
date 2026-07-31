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
import java.util.function.Function;
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

    /**
     * How far below the level root a dimension folder sits: {@code dimensions/minecraft/<name>}.
     */
    private static final int DIMENSION_FOLDER_DEPTH = 3;

    /**
     * The only saved data a client reads from inside a dimension. Everything else a Paper dimension folder holds is
     * level-scoped and belongs in the save's root {@code data/minecraft}: Paper keeps a per-world copy of those files
     * because each Bukkit world is a level of its own, but a single-player save has one set for the whole world, and a
     * client that finds no {@code world_gen_settings.dat} there cannot build the level at all.
     */
    private static final Set<String> DIMENSION_SCOPED_DATA =
            Set.of("raids.dat", "world_border.dat", "chunk_tickets.dat");

    private static final String MINECRAFT_DATA_PREFIX = "data" + File.separator + "minecraft" + File.separator;

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
     * @param progress Notified as the world is packed, for a caller showing the player how far along it is
     * @throws WorldTooLargeException If the archive would grow past {@code maxBytes}
     * @throws IOException If the world cannot be read or the archive cannot be written
     */
    public static void export(
            String worldName,
            File worldFolder,
            File defaultLevelFolder,
            Path target,
            long maxBytes,
            ExportProgress progress)
            throws IOException {
        Path source = worldFolder.toPath();
        if (!Files.isDirectory(source)) {
            throw new IOException("World folder does not exist: " + worldFolder.getAbsolutePath());
        }

        String rootDirectory = fileName(worldName);
        Files.createDirectories(target.getParent());
        boolean isLevelFolder = Files.isRegularFile(source.resolve("level.dat"));
        List<Path> files = collectFiles(source, isLevelFolder);
        progress.update(0L, totalBytes(files));

        try (CountingOutputStream counter = new CountingOutputStream(Files.newOutputStream(target), maxBytes);
                ZipOutputStream zip = new ZipOutputStream(counter)) {
            if (isLevelFolder) {
                // Already a level folder: the pre-26.1 flat layout, or a world that is itself the main level. Its own
                // nether and end come along; dimensions belonging to other worlds do not.
                writeEntry(zip, rootDirectory + "/level.dat", levelDat(source, worldName));
                copyTree(zip, source, files, progress, relative -> rootDirectory + "/" + slashed(relative));
            } else {
                writeEntry(zip, rootDirectory + "/level.dat", levelDat(defaultLevelFolder.toPath(), worldName));
                copyTree(zip, source, files, progress, relative -> dimensionEntry(rootDirectory, relative));
            }
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    /**
     * Reports how much of a world has been packed. Called from the export thread, once per file.
     */
    @FunctionalInterface
    public interface ExportProgress {

        ExportProgress IGNORED = (packedBytes, totalBytes) -> {};

        /**
         * @param packedBytes Bytes packed so far
         * @param totalBytes Bytes the finished export will have read
         */
        void update(long packedBytes, long totalBytes);
    }

    /**
     * {@return {@code name} reduced to characters that are safe in an archive entry and an HTTP header}
     */
    public static String fileName(String name) {
        String sanitized = UNSAFE_NAME_CHARACTERS.matcher(name).replaceAll("_");
        return sanitized.isBlank() ? "world" : sanitized;
    }

    private static void copyTree(
            ZipOutputStream zip,
            Path root,
            List<Path> files,
            ExportProgress progress,
            Function<Path, String> entryNamer)
            throws IOException {
        long total = totalBytes(files);
        long packed = 0L;
        for (Path file : files) {
            zip.putNextEntry(new ZipEntry(entryNamer.apply(root.relativize(file))));
            Files.copy(file, zip);
            zip.closeEntry();

            packed += sizeOf(file);
            progress.update(packed, total);
        }
    }

    /**
     * Places a file from a Paper dimension folder in the save, splitting its {@code data/minecraft} contents by scope:
     * the client reads generator settings, game rules and the like from the save root, and only a few files from
     * inside the dimension itself.
     */
    private static String dimensionEntry(String rootDirectory, Path relative) {
        String path = relative.toString();
        if (path.startsWith(MINECRAFT_DATA_PREFIX)
                && !DIMENSION_SCOPED_DATA.contains(relative.getFileName().toString())) {
            return rootDirectory + "/data/minecraft/" + relative.getFileName();
        }
        return rootDirectory + "/dimensions/minecraft/overworld/" + slashed(relative);
    }

    private static String slashed(Path relative) {
        return relative.toString().replace(File.separatorChar, '/');
    }

    /**
     * The files the export will write, resolved up front so its size is known before the first byte is packed.
     */
    private static List<Path> collectFiles(Path root, boolean isLevelFolder) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(file -> !isExcluded(root.relativize(file), isLevelFolder))
                    .toList();
        }
    }

    private static long totalBytes(List<Path> files) {
        return files.stream().mapToLong(WorldExporter::sizeOf).sum();
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0L;
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
        Path levelDat = findLevelDat(levelFolder);
        if (levelDat == null) {
            throw new IOException("No level.dat to export at or above: " + levelFolder);
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
     * Finds the {@code level.dat} for a folder, searching upwards.
     *
     * <p>Paper reports a world's folder inconsistently across setups: for the main level it may be the level root or
     * the overworld's dimension folder ({@code <level>/dimensions/minecraft/overworld}). Both lead to the same
     * {@code level.dat}, one directly and one three levels up, so the search walks far enough to cover the deeper
     * shape and no further.
     *
     * @param folder The folder to start at
     * @return The level.dat, or {@code null} if there is none within reach
     */
    private static @Nullable Path findLevelDat(Path folder) {
        Path candidate = folder;
        for (int depth = 0; depth <= DIMENSION_FOLDER_DEPTH && candidate != null; depth++) {
            Path levelDat = candidate.resolve("level.dat");
            if (Files.isRegularFile(levelDat)) {
                return levelDat;
            }
            candidate = candidate.getParent();
        }
        return null;
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
