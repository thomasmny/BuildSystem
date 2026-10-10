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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.config.ConfigService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.SftpEventListener;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * Runs the SFTP backup storage against an in-process SSH server whose file system is a temporary directory.
 */
@NullMarked
class SftpBackupStorageTest {

    private static final String USER = "backup";
    private static final String PASSWORD = "secret";

    @TempDir
    Path tempDir;

    private Path remoteRoot;
    private Path dataFolder;
    private BuildWorld world;
    private MockedStatic<Bukkit> bukkit;
    private @Nullable SshServer server;
    // The storage a test opened last, closed after it.
    private @Nullable SftpBackupStorage opened;
    // Read on the server's threads.
    private volatile @Nullable SftpEventListener serverListener;

    @BeforeEach
    void setUp() throws IOException {
        remoteRoot = Files.createDirectories(tempDir.resolve("remote"));
        dataFolder = Files.createDirectories(tempDir.resolve("plugin"));
        Path worldContainer = Files.createDirectories(tempDir.resolve("server"));
        Files.writeString(
                Files.createDirectories(worldContainer.resolve("lobby")).resolve("level.dat"), "level");

        world = mock(BuildWorld.class);
        when(world.getName()).thenReturn("lobby");
        when(world.getUniqueId()).thenReturn(UUID.randomUUID());

        // Mockito's static mock only applies to this thread, so the storage runs its work on it (Runnable::run).
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getWorldContainer).thenReturn(worldContainer.toFile());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (opened != null) {
            opened.close();
        }
        if (server != null) {
            server.stop(true);
        }
        bukkit.close();
    }

    private SshServer startServer(int port, String hostKey) throws IOException {
        SimpleGeneratorHostKeyProvider keys = new SimpleGeneratorHostKeyProvider(tempDir.resolve(hostKey));
        keys.setAlgorithm(KeyUtils.RSA_ALGORITHM);
        keys.setKeySize(2048);
        SftpSubsystemFactory sftp = new SftpSubsystemFactory();
        sftp.addSftpEventListener(new SftpEventListener() {
            @Override
            public void moving(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts)
                    throws IOException {
                if (serverListener != null) {
                    serverListener.moving(session, srcPath, dstPath, opts);
                }
            }
        });

        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setHost("127.0.0.1");
        sshServer.setPort(port);
        sshServer.setKeyPairProvider(keys);
        sshServer.setPasswordAuthenticator((user, password, session) -> USER.equals(user) && PASSWORD.equals(password));
        sshServer.setSubsystemFactories(List.of(sftp));
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(remoteRoot));
        sshServer.start();
        server = sshServer;
        return sshServer;
    }

    private SftpBackupStorage connect(String basePath) {
        BackupProfile profile = mock(BackupProfile.class);
        opened = new SftpBackupStorage(
                Logger.getLogger("test"),
                Runnable::run,
                dataFolder.toFile(),
                mock(ConfigService.class, RETURNS_DEEP_STUBS),
                buildWorld -> profile,
                "127.0.0.1",
                server.getPort(),
                USER,
                PASSWORD,
                basePath);
        return opened;
    }

    private List<Path> remoteFiles() throws IOException {
        try (Stream<Path> files = Files.walk(remoteRoot)) {
            return files.filter(Files::isRegularFile).toList();
        }
    }

    @Test
    void storedBackup_isListedAndDownloadsByteForByte() throws IOException {
        startServer(0, "host.key");
        SftpBackupStorage storage = connect("backups");

        Backup stored = storage.storeBackup(world).join();
        List<Backup> listed = storage.listBackups(world).join();
        File downloaded = storage.downloadBackup(listed.getFirst()).join();

        String expectedKey = "backups/" + world.getUniqueId() + "/" + stored.creationTime() + ".zip";
        assertEquals(expectedKey, stored.key());
        assertEquals(1, listed.size());
        assertEquals(stored.key(), listed.getFirst().key());
        assertArrayEquals(Files.readAllBytes(remoteRoot.resolve(expectedKey)), Files.readAllBytes(downloaded.toPath()));
        assertEquals("level", levelDatIn(downloaded.toPath()));
    }

    private static @Nullable String levelDatIn(Path zip) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (entry.getName().endsWith("level.dat")) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    @Test
    void missingBasePath_isCreatedRecursively() throws IOException {
        startServer(0, "host.key");
        SftpBackupStorage storage = connect("a/b/c");

        storage.storeBackup(world).join();

        Path worldDirectory = remoteRoot.resolve("a/b/c/" + world.getUniqueId());
        assertTrue(Files.isDirectory(worldDirectory));
        assertEquals(1, remoteFiles().size());
    }

    @Test
    void failedUpload_leavesNoPartFile() throws IOException {
        startServer(0, "host.key");
        serverListener = new SftpEventListener() {
            @Override
            public void moving(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts)
                    throws IOException {
                throw new IOException("rename refused");
            }
        };
        SftpBackupStorage storage = connect("backups");

        assertThrows(CompletionException.class, () -> storage.storeBackup(world).join());

        serverListener = null;
        assertEquals(List.of(), remoteFiles());
        assertEquals(List.of(), storage.listBackups(world).join());
    }

    @Test
    void hostKey_isRecordedOnFirstUse_andAChangedKeyIsRefused() throws IOException {
        int port = startServer(0, "host.key").getPort();
        SftpBackupStorage first = connect("backups");
        first.listBackups(world).join();
        Path knownHosts = dataFolder.resolve(".sftp_known_hosts");
        assertTrue(Files.readString(knownHosts).contains("127.0.0.1"));

        first.close();
        server.stop(true);
        startServer(port, "other-host.key");
        SftpBackupStorage storage = connect("backups");

        assertThrows(CompletionException.class, () -> storage.storeBackup(world).join());
        assertEquals(List.of(), remoteFiles());
    }

    @Test
    void serverRestart_isReconnectedTo() throws IOException {
        int port = startServer(0, "host.key").getPort();
        SftpBackupStorage storage = connect("backups");
        storage.storeBackup(world).join();

        server.stop(true);
        startServer(port, "host.key");

        // The first call may still use the dropped connection and fail with a CompletionException wrapping the
        // IOException. That failure closes the connection, so the next call must reconnect.
        try {
            storage.listBackups(world).join();
        } catch (CompletionException expectedOnTheDroppedConnection) {
        }
        assertEquals(1, storage.listBackups(world).join().size());
    }

    @Test
    void deleteBackup_removesOnlyThatFile() throws IOException {
        startServer(0, "host.key");
        SftpBackupStorage storage = connect("backups");
        Backup backup = storage.storeBackup(world).join();
        Path other =
                Files.writeString(remoteRoot.resolve("backups/" + world.getUniqueId() + "/1000.zip"), "older backup");

        storage.deleteBackup(backup).join();

        assertEquals(List.of(other), remoteFiles());
        assertFalse(
                storage.listBackups(world).join().stream().anyMatch(b -> b.key().equals(backup.key())));
    }
}
