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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void listReturnsNewestFirst() throws Exception {
        Path dir = worldBackupDir();
        // Use modification time via Files.setLastModifiedTime to control ordering
        Path older = createZip(dir, "older.zip");
        Path newer = createZip(dir, "newer.zip");
        // Set mtime explicitly: newer > older
        Files.setLastModifiedTime(older, java.nio.file.attribute.FileTime.fromMillis(1000L));
        Files.setLastModifiedTime(newer, java.nio.file.attribute.FileTime.fromMillis(9000L));

        List<Backup> backups = storage.listBackups(world).get();
        assertEquals(2, backups.size());
        assertTrue(backups.get(0).creationTime() >= backups.get(1).creationTime(),
                "First element should have a creation time >= second");
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
        when(backup.key()).thenReturn(backupRoot.resolve("nonexistent.zip").toAbsolutePath().toString());
        // Should complete without throwing
        storage.deleteBackup(backup).get();
    }
}
