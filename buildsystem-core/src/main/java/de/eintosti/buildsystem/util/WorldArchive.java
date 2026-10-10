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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.world.backup.WorldRestore;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A world folder packed into a zip file for a backup. Archives are streamed to and from disk, so a world of any size
 * needs no more memory than one copy buffer.
 *
 * <p>Entries are paths relative to the world folder. Local backups made before 4.1 instead put the world folder itself
 * at the root of the archive; {@link #extract} recognises those and drops the root.
 */
@NullMarked
public final class WorldArchive {

    /**
     * Held open by the server while the world is loaded, and meaningless in a backup.
     */
    private static final String SESSION_LOCK = "session.lock";

    private WorldArchive() {}

    /**
     * Packs a world's folder into {@code target}. For the server's default world, the other worlds nested inside its
     * folder are left out.
     */
    public static void write(BuildWorld buildWorld, Path target) throws IOException {
        File worldFolder = FileUtils.worldFolder(buildWorld.getName());
        write(worldFolder.toPath(), FileUtils.nestedWorldsRoot(worldFolder), target);
    }

    /**
     * Packs every file under {@code worldFolder} into {@code target}. A file that cannot be read fails the whole
     * archive rather than leaving it silently incomplete, and so does a world folder with no files.
     *
     * @param excludedSubtree A directory inside the world folder to leave out, or {@code null}
     */
    static void write(Path worldFolder, @Nullable Path excludedSubtree, Path target) throws IOException {
        // A world folder that is a symbolic link is archived from where it points.
        Path folder = worldFolder.toRealPath();
        @Nullable Path excluded = realPathIfPresent(excludedSubtree);
        Path restoreFolder = folder.resolve(WorldRestore.FOLDER);
        List<Path> files;
        try (Stream<Path> walk = Files.walk(folder)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(file -> excluded == null || !file.startsWith(excluded))
                    .filter(file -> !file.startsWith(restoreFolder))
                    .filter(file -> !file.equals(folder.resolve(SESSION_LOCK)))
                    .toList();
        }
        if (files.isEmpty()) {
            throw new IOException("World folder has no files to back up: " + worldFolder);
        }

        // The remote storages write into a temp directory that their close() deletes.
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target));
                ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Path file : files) {
                zip.putNextEntry(new ZipEntry(folder.relativize(file).toString().replace(File.separatorChar, '/')));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private static @Nullable Path realPathIfPresent(@Nullable Path path) throws IOException {
        return path != null && Files.exists(path) ? path.toRealPath() : null;
    }

    /**
     * Checks that {@code archive} opens, lists files, and that none of them would land outside {@code targetDirectory}.
     * Only the archive's central directory is read, so a damaged entry still fails later, during {@link #extract}.
     */
    public static void validate(File archive, File targetDirectory) throws IOException {
        try (ZipFile zip = open(archive)) {
            String root = legacyRoot(zip);
            boolean hasFiles = false;
            for (ZipEntry entry : zip.stream().toList()) {
                checkInside(targetDirectory, entry, root);
                hasFiles |= !entry.isDirectory();
            }
            if (!hasFiles) {
                throw new IOException("Refusing to restore backup: archive contains no files: " + archive);
            }
        }
    }

    /**
     * Extracts {@code archive} into {@code targetDirectory}, refusing any entry that would land outside it.
     */
    public static void extract(File archive, File targetDirectory) throws IOException {
        try (ZipFile zip = open(archive)) {
            String root = legacyRoot(zip);
            for (ZipEntry entry : zip.stream().toList()) {
                File destination = checkInside(targetDirectory, entry, root);
                if (entry.isDirectory()) {
                    Files.createDirectories(destination.toPath());
                    continue;
                }
                Files.createDirectories(destination.toPath().getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static ZipFile open(File archive) throws IOException {
        try {
            return new ZipFile(archive);
        } catch (IOException e) {
            throw new IOException("Refusing to restore backup: archive cannot be read: " + archive, e);
        }
    }

    /**
     * {@return the folder name a pre-4.1 local backup put at the root of the archive, with its slash, or {@code ""}}
     * Those archives have a directory entry for the root and every other entry inside it; archives without directory
     * entries, as every other backup was written, never match.
     */
    private static String legacyRoot(ZipFile zip) {
        List<String> names = zip.stream().map(ZipEntry::getName).toList();
        for (String name : names) {
            boolean topLevelDirectory = name.endsWith("/") && name.indexOf('/') == name.length() - 1;
            if (topLevelDirectory && names.stream().allMatch(other -> other.startsWith(name))) {
                return name;
            }
        }
        return "";
    }

    private static File checkInside(File targetDirectory, ZipEntry entry, String root) throws IOException {
        String name = entry.getName().substring(root.length());
        File destination = new File(targetDirectory, name);
        if (StringCleaner.isPathEscape(targetDirectory, destination)) {
            throw new IOException(
                    "Refusing to restore backup: archive entry escapes the world directory: " + entry.getName());
        }
        return destination;
    }
}
