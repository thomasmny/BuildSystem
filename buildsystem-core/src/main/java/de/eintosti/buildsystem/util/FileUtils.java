/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public final class FileUtils {

    private static final Set<String> IGNORE_FILES = Sets.newHashSet("uid.dat", "session.lock");

    private FileUtils() {
    }

    /**
     * Copies a file or directory from the source location to the target location.
     *
     * @param source The source file or directory to be copied
     * @param target The target file or directory where the source will be copied to
     * @throws RuntimeException If an I/O error occurs while copying
     */
    public static void copy(@NotNull File source, @NotNull File target) {
        try {
            if (IGNORE_FILES.contains(source.getName())) {
                return;
            }

            if (source.isDirectory()) {
                copyDirectory(source, target);
            } else {
                copyFile(source, target);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies a directory from source to target.
     *
     * @param source The source directory to be copied
     * @param target The target directory where the source directory will be copied to
     * @throws IOException If an I/O error occurs while copying the directory
     */
    private static void copyDirectory(@NotNull File source, @NotNull File target) throws IOException {
        if (!target.exists() && !target.mkdirs()) {
            throw new IOException("Couldn't create directory: " + target.getName());
        }

        for (String fileName : source.list()) {
            if (IGNORE_FILES.contains(fileName)) {
                continue;
            }

            File sourceFile = new File(source, fileName);
            File targetFile = new File(target, fileName);
            copy(sourceFile, targetFile);
        }
    }

    /**
     * Copies a file from the source to the target location.
     *
     * @param source The source file to be copied
     * @param target The target file where the source file will be copied to
     * @throws IOException If an I/O error occurs while copying the file
     */
    private static void copyFile(@NotNull File source, @NotNull File target) throws IOException {
        try (InputStream inputStream = Files.newInputStream(source.toPath());
                OutputStream outputStream = Files.newOutputStream(target.toPath())) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory Directory to delete
     */
    public static void deleteDirectory(File directory) {
        try (Stream<Path> walk = Files.walk(directory.toPath())) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the creation date of a file.
     *
     * @param file The file to be checked
     * @return The amount of milliseconds that have passed since {@code January 1, 1970 UTC}, until the file was created
     */
    public static long getDirectoryCreation(File file) {
        long creation = System.currentTimeMillis();
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            FileTime time = attrs.creationTime();
            creation = time.toMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return creation;
    }
}