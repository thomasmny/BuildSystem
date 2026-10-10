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
package de.eintosti.buildsystem.world.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Both ways of restoring, by renaming the world folder and inside it, must leave a whole world behind whatever step
 * fails, and a server stop in the middle must be undone at the next start.
 */
@NullMarked
class WorldRestoreTest {

    private static final Logger LOGGER = Logger.getLogger("WorldRestoreTest");

    @TempDir
    Path tempDir;

    private Path world() throws IOException {
        Path world = Files.createDirectories(tempDir.resolve("arena").resolve("region"))
                .getParent();
        Files.writeString(world.resolve("level.dat"), "current");
        Files.writeString(world.resolve("region").resolve("r.0.0.mca"), "current-region");
        Files.writeString(world.resolve("uid.dat"), "current-uid");
        return world;
    }

    private File backup() throws IOException {
        Path archive = tempDir.resolve("backup.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (String name : new String[] {"level.dat", "uid.dat", "datapacks/pack.zip"}) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(("restored-" + name).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return archive.toFile();
    }

    private static void assertCurrent(Path world) throws IOException {
        assertEquals("current", Files.readString(world.resolve("level.dat")));
        assertEquals("current-region", Files.readString(world.resolve("region").resolve("r.0.0.mca")));
        assertFalse(Files.exists(world.resolve("datapacks")), "nothing from the backup is left in the world");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void swap_putsTheWholeBackupInPlace(boolean inPlace) throws IOException {
        Path world = world();
        WorldRestore restore = new WorldRestore(world.toFile());
        restore.stage(backup(), inPlace);

        restore.swap();
        restore.cleanUp();

        assertEquals("restored-level.dat", Files.readString(world.resolve("level.dat")));
        assertEquals("restored-uid.dat", Files.readString(world.resolve("uid.dat")));
        assertTrue(Files.exists(world.resolve("datapacks").resolve("pack.zip")));
        assertFalse(Files.exists(world.resolve("region")), "the old world's entries are gone");
        assertFalse(Files.exists(world.resolve(WorldRestore.FOLDER)));
        assertFalse(Files.exists(tempDir.resolve(WorldRestore.FOLDER)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void swapThatCannotMoveTheBackupIn_putsTheOldWorldBack(boolean inPlace) throws IOException {
        Path world = world();
        WorldRestore restore = new WorldRestore(world.toFile());
        restore.stage(backup(), inPlace);
        FileUtils.deleteDirectory(restore.staged());

        assertThrows(IOException.class, restore::swap);

        assertCurrent(world);
        assertTrue(restore.worldIsBack());
        assertFalse(Files.exists(restore.replaced()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void leftoverFromAnEarlierRestore_isRefusedBeforeStaging(boolean inPlace) throws IOException {
        Path world = world();
        Path leftover = inPlace
                ? world.resolve(WorldRestore.FOLDER).resolve("replaced")
                : tempDir.resolve(WorldRestore.FOLDER).resolve("arena.replaced");
        Files.createDirectories(leftover);
        WorldRestore restore = new WorldRestore(world.toFile());

        assertThrows(IOException.class, () -> restore.stage(backup(), false));

        assertTrue(Files.exists(leftover));
        assertCurrent(world);
    }

    @Test
    void symlinkedWorldFolder_isRestoredInPlace() throws IOException {
        Path link = Files.createSymbolicLink(tempDir.resolve("linked"), world());
        WorldRestore restore = new WorldRestore(link.toFile());

        restore.stage(backup());

        assertTrue(restore.staged().startsWith(link.resolve(WorldRestore.FOLDER)));
    }

    @Test
    void recover_afterAStopBetweenTheRenames_movesTheWorldBack() throws IOException {
        Path world = world();
        Path replaced =
                Files.createDirectories(tempDir.resolve(WorldRestore.FOLDER)).resolve("arena.replaced");
        Files.move(world, replaced);
        Files.createDirectories(tempDir.resolve(WorldRestore.FOLDER).resolve("arena.1234.staged"));

        WorldRestore.recover(world.toFile(), LOGGER);

        assertCurrent(world);
        assertFalse(Files.exists(tempDir.resolve(WorldRestore.FOLDER)), "the leftovers are deleted");
    }

    @Test
    void recover_withTheWorldInPlace_leavesTheOldCopyAlone() throws IOException {
        Path world = world();
        Path replaced =
                Files.createDirectories(tempDir.resolve(WorldRestore.FOLDER).resolve("arena.replaced"));

        WorldRestore.recover(world.toFile(), LOGGER);

        assertCurrent(world);
        assertTrue(Files.exists(replaced));
    }

    @Test
    void recover_afterAStopWhileMovingTheBackupIn_putsTheOldEntriesBack() throws IOException {
        Path world = world();
        WorldRestore restore = new WorldRestore(world.toFile());
        restore.stage(backup(), true);
        // The state after the old entries are moved aside and the first backup entry is moved in.
        Path replaced = Files.createDirectories(restore.replaced());
        for (String name : new String[] {"level.dat", "region", "uid.dat"}) {
            Files.move(world.resolve(name), replaced.resolve(name));
        }
        Files.createFile(world.resolve(WorldRestore.FOLDER).resolve("moving-in"));
        Files.move(restore.staged().resolve("datapacks"), world.resolve("datapacks"));

        WorldRestore.recover(world.toFile(), LOGGER);

        assertCurrent(world);
        assertEquals("current-uid", Files.readString(world.resolve("uid.dat")));
        assertFalse(Files.exists(world.resolve(WorldRestore.FOLDER)));
    }

    @Test
    void recover_afterAStopWhileMovingTheWorldAside_putsTheMovedEntriesBack() throws IOException {
        Path world = world();
        Path replaced =
                Files.createDirectories(world.resolve(WorldRestore.FOLDER).resolve("replaced"));
        Files.move(world.resolve("region"), replaced.resolve("region"));

        WorldRestore.recover(world.toFile(), LOGGER);

        assertCurrent(world);
        assertFalse(Files.exists(world.resolve(WorldRestore.FOLDER)));
    }
}
