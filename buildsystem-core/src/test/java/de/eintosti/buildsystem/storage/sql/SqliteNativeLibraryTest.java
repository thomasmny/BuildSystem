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
package de.eintosti.buildsystem.storage.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.storage.StorageBackend;
import de.eintosti.buildsystem.storage.StorageException;
import de.eintosti.buildsystem.storage.StorageType;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteNativeLibraryTest {

    @TempDir
    File dataFolder;

    /**
     * Runs in a JVM of its own, since a JVM loads the native library only once.
     */
    @Test
    void nativeLibraryThatCannotLoad_failsStartupSayingHowToFixIt() throws Exception {
        File notAFolder = new File(dataFolder, "not-a-folder");
        Files.writeString(notAFolder.toPath(), "");
        Process process = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-Dorg.sqlite.tmpdir=" + notAFolder,
                        "-cp",
                        System.getProperty("java.class.path"),
                        Connect.class.getName(),
                        dataFolder.getPath())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();

        assertTrue(output.contains("FAILED SQLite could not load its native library"), output);
        assertTrue(output.contains("-Dorg.sqlite.tmpdir=<a folder that does>"), output);
    }

    static final class Connect {

        public static void main(String[] args) {
            try {
                StorageBackend.connect(
                                StorageType.SQLITE,
                                new PluginConfig.Storage.Database(StorageType.SQLITE, "buildsystem_", Map.of()),
                                new File(args[0]),
                                Logger.getLogger("SqliteNativeLibraryTest"))
                        .close();
                System.out.println("CONNECTED");
            } catch (StorageException e) {
                System.out.println("FAILED " + e.getMessage());
            }
        }
    }
}
