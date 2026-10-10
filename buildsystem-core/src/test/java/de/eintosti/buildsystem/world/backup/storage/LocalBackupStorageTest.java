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
package de.eintosti.buildsystem.world.backup.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@NullMarked
class LocalBackupStorageTest {

    @TempDir
    Path backupRoot;

    private LocalBackupStorage storage;
    private BuildWorld world;
    private UUID worldId;

    @BeforeEach
    void setUp() {
        BackupProfile profile = mock(BackupProfile.class);
        world = mock(BuildWorld.class);
        worldId = UUID.randomUUID();
        when(world.getUniqueId()).thenReturn(worldId);
        when(world.getName()).thenReturn("test-world");

        // Synchronous executor so futures complete immediately in tests
        storage = new LocalBackupStorage(Logger.getLogger("test"), Runnable::run, backupRoot, bw -> profile);
    }

    private Path worldBackupDir() throws Exception {
        Path dir = backupRoot.resolve(worldId.toString());
        Files.createDirectories(dir);
        return dir;
    }

    private Path createZip(Path dir, String name) throws Exception {
        Path zip = dir.resolve(name);
        Files.writeString(zip, "fake-zip");
        return zip;
    }

    @Test
    void backupNameFormat() {
        long ts = 1234567890123L;
        assertEquals("1234567890123.zip", AbstractBackupStorage.backupName(ts));
    }

    @Test
    void listReturnsZipFilesOnly() throws Exception {
        Path dir = worldBackupDir();
        createZip(dir, "1000.zip");
        createZip(dir, "2000.zip");
        Files.writeString(dir.resolve("notes.txt"), "not a backup");

        List<Backup> backups = storage.listBackups(world).get();
        assertEquals(2, backups.size());
        assertTrue(backups.stream().allMatch(b -> b.key().endsWith(".zip")));
    }

    private static void setTimes(Path file, long millis) throws Exception {
        FileTime time = FileTime.fromMillis(millis);
        Files.getFileAttributeView(file, BasicFileAttributeView.class).setTimes(time, null, time);
    }

    private static long creationTime(Path file) throws Exception {
        return Files.readAttributes(file, BasicFileAttributes.class)
                .creationTime()
                .toMillis();
    }

    @Test
    void listOrdersByTheTimestampInTheFileName() throws Exception {
        Path dir = worldBackupDir();
        // A copy or rsync of the backup folder: the newer backup's file now carries the older times. Created in this
        // order as well, because Linux keeps the real creation time whatever setTimes asks for.
        Path newer = createZip(dir, "2000.zip");
        Thread.sleep(20);
        Path older = createZip(dir, "1000.zip");
        setTimes(newer, 1_000L);
        setTimes(older, 9_000L);
        assertTrue(creationTime(older) > creationTime(newer), "the file times must disagree with the names");

        List<Backup> backups = storage.listBackups(world).get();

        assertEquals(
                List.of(2000L, 1000L),
                backups.stream().map(Backup::creationTime).toList());
        assertEquals(newer.toAbsolutePath().toString(), backups.getFirst().key());
    }

    @Test
    void backupNamedOtherwise_isDatedByItsCreationTime() throws Exception {
        Path copy = createZip(worldBackupDir(), "manual copy.zip");
        setTimes(copy, 5_000L);

        List<Backup> backups = storage.listBackups(world).get();

        assertEquals(
                List.of(creationTime(copy)),
                backups.stream().map(Backup::creationTime).toList());
    }

    @Test
    void listEmptyWhenDirectoryAbsent() throws Exception {
        List<Backup> backups = storage.listBackups(world).get();
        assertTrue(backups.isEmpty());
    }

    @Test
    void deleteRemovesExactFile() throws Exception {
        Path dir = worldBackupDir();
        Path zip = createZip(dir, "delete-me.zip");
        assertTrue(Files.exists(zip));

        Backup backup = mock(Backup.class);
        when(backup.key()).thenReturn(zip.toAbsolutePath().toString());

        storage.deleteBackup(backup).get();
        assertFalse(Files.exists(zip));
    }

    @Test
    void deleteIsNoOpForMissingFile() throws Exception {
        Backup backup = mock(Backup.class);
        when(backup.key())
                .thenReturn(
                        backupRoot.resolve("nonexistent.zip").toAbsolutePath().toString());
        // Should complete without throwing
        storage.deleteBackup(backup).get();
    }
}
