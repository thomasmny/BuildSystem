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
package de.eintosti.buildsystem.world.backup.storage;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.backup.BackupImpl;
import de.eintosti.buildsystem.world.backup.BackupService;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.signature.BuiltinSignatures;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SftpBackupStorage implements BackupStorage {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(5);
    private static final int BUFFER_SIZE = 8192;

    private final BuildSystemPlugin plugin;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String remoteBasePath;
    private final Path tmpDownloadPath;

    @Nullable
    private volatile SshClient sshClient;
    @Nullable
    private volatile ClientSession clientSession;
    @Nullable
    private volatile SftpClient sftpClient;

    public SftpBackupStorage(BuildSystemPlugin plugin, String host, int port, String username, String password, String remoteBasePath) {
        this.plugin = plugin;

        this.host = host;
        this.port = validatePort(port);
        this.username = username;
        this.password = password;
        this.remoteBasePath = normalizeBasePath(remoteBasePath);
        this.tmpDownloadPath = FileUtils.resolve(plugin.getDataFolder().toPath(), ".tmp_backup_downloads");

        Security.addProvider(new BouncyCastleProvider());
        establishConnection();
    }

    private static int validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        return port;
    }

    private static String normalizeBasePath(@Nullable String basePath) {
        if (basePath == null || basePath.trim().isEmpty()) {
            return "/";
        }
        String normalized = basePath.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private synchronized void establishConnection() {
        if (this.sshClient == null) {
            initializeSshClient();
        }

        try {
            this.clientSession = this.sshClient.connect(this.username, this.host, this.port)
                    .verify(CONNECTION_TIMEOUT.toMillis())
                    .getSession();

            if (this.clientSession == null) {
                throw new IllegalStateException();
            }

            this.clientSession.addPasswordIdentity(this.password);
            this.clientSession.auth().verify(AUTH_TIMEOUT.toMillis());

            this.sftpClient = SftpClientFactory.instance().createSftpClient(this.clientSession);
            plugin.getLogger().info("SFTP connection established successfully.");
        } catch (Exception e) {
            disconnectAll();
            plugin.getLogger().log(Level.SEVERE, "Failed to establish SFTP connection to " + this.host + ":" + this.port, e);
        }
    }

    private void initializeSshClient() {
        this.sshClient = SshClient.setUpDefaultClient();
        this.sshClient.setSignatureFactories(Arrays.asList(
                BuiltinSignatures.rsa,
                BuiltinSignatures.ed25519
        ));
        this.sshClient.start();
    }

    @Nullable
    private SftpClient getSftpClient() {
        if (sftpClient == null || !sftpClient.isOpen()) {
            synchronized (this) {
                if (sftpClient == null || !sftpClient.isOpen()) {
                    establishConnection();
                }
            }
        }
        return sftpClient;
    }

    private String getBackupDirectory(BuildWorld buildWorld) {
        return this.remoteBasePath + buildWorld.getUniqueId() + "/";
    }

    @Override
    public synchronized CompletableFuture<List<Backup>> listBackups(BuildWorld buildWorld) {
        return CompletableFuture.supplyAsync(() -> {
            List<Backup> backups = new ArrayList<>(Config.World.Backup.maxBackupsPerWorld);
            String backupDirectory = getBackupDirectory(buildWorld);

            try {
                SftpClient sftp = getSftpClient();
                createDirectoryIfNotExists(sftp, backupDirectory);

                Iterable<DirEntry> files = sftp.readDir(backupDirectory);
                for (SftpClient.DirEntry file : files) {
                    if (!file.getFilename().endsWith(".zip")) {
                        continue;
                    }

                    Attributes attributes = file.getAttributes();
                    long timestamp = Optional.ofNullable(attributes.getCreateTime())
                            .orElse(attributes.getModifyTime())
                            .toMillis();

                    backups.add(new BackupImpl(
                            plugin.getBackupService().getProfile(buildWorld),
                            timestamp,
                            backupDirectory + file.getFilename()
                    ));
                }
            } catch (IOException e) {
                disconnectAll();
                throw new RuntimeException("Failed to list SFTP backups", e);
            }

            backups.sort(Comparator.comparingLong(Backup::creationTime).reversed());
            return backups;
        }, BackupService.BACKUP_EXECUTOR);
    }

    @Override
    public synchronized CompletableFuture<Backup> storeBackup(BuildWorld buildWorld) {
        return CompletableFuture.supplyAsync(() -> {
            long timestamp = System.currentTimeMillis();
            String backupName = getBackupName(timestamp);
            String backupDirectory = getBackupDirectory(buildWorld);
            String remotePath = backupDirectory + backupName;

            byte[] zipBytes;
            try {
                zipBytes = FileUtils.zipWorldToMemory(buildWorld);
            } catch (IOException e) {
                throw new RuntimeException("Failed to zip world: " + buildWorld.getName());
            }

            try {
                SftpClient sftp = getSftpClient();
                createDirectoryIfNotExists(sftp, backupDirectory);

                try (
                        OutputStream out = sftp.write(remotePath);
                        BufferedOutputStream bufferedOut = new BufferedOutputStream(out, BUFFER_SIZE)
                ) {
                    bufferedOut.write(zipBytes);
                    bufferedOut.flush();
                }

                plugin.getLogger().info(String.format("Backed up world '%s'. Took %sms", buildWorld.getName(), (System.currentTimeMillis() - timestamp)));
                return new BackupImpl(plugin.getBackupService().getProfile(buildWorld), timestamp, remotePath);
            } catch (IOException e) {
                disconnectAll();
                throw new RuntimeException("Failed to upload SFTP backup", e);
            }
        }, BackupService.BACKUP_EXECUTOR);
    }

    private void createDirectoryIfNotExists(SftpClient sftp, String path) throws IOException {
        try {
            if (!directoryExists(sftp, path)) {
                createDirectoriesRecursively(sftp, path);
            }
        } catch (IOException e) {
            throw new IOException("Failed to create backup directory: " + path, e);
        }
    }

    private void createDirectoriesRecursively(SftpClient sftp, @Nullable String path) throws IOException {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return;
        }

        String normalizedPath = path.replace("\\", "/");
        if (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        if (directoryExists(sftp, normalizedPath)) {
            return;
        }

        int lastSeparator = normalizedPath.lastIndexOf('/');
        if (lastSeparator > 0) {
            String parentPath = normalizedPath.substring(0, lastSeparator);
            createDirectoriesRecursively(sftp, parentPath);
        }

        sftp.mkdir(normalizedPath);
    }

    private boolean directoryExists(SftpClient sftp, String path) {
        try {
            return sftp.stat(path).isDirectory();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public synchronized CompletableFuture<File> downloadBackup(Backup backup) {
        return CompletableFuture.supplyAsync(() -> {
            SftpClient sftp = getSftpClient();
            Path target = tmpDownloadPath.resolve(UUID.randomUUID() + ".zip");

            try (
                    InputStream in = sftp.read(backup.key());
                    BufferedInputStream bufferedIn = new BufferedInputStream(in, BUFFER_SIZE);
                    OutputStream out = Files.newOutputStream(target);
                    BufferedOutputStream bufferedOut = new BufferedOutputStream(out, BUFFER_SIZE)
            ) {
                bufferedIn.transferTo(bufferedOut);
                return target.toFile();
            } catch (IOException e) {
                disconnectAll();
                throw new RuntimeException("Failed to download SFTP backup: " + backup.key(), e);
            }
        }, BackupService.BACKUP_EXECUTOR);
    }

    @Override
    public synchronized CompletableFuture<Void> deleteBackup(Backup backup) {
        return CompletableFuture.runAsync(() -> {
            try {
                SftpClient sftp = getSftpClient();
                sftp.remove(backup.key());
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete SFTP backup: " + backup.key(), e);
            }
        }, BackupService.BACKUP_EXECUTOR);
    }

    @Override
    public void close() {
        disconnectAll();
        FileUtils.deleteDirectory(this.tmpDownloadPath);
    }

    private void disconnectAll() {
        close(this.sshClient);
        this.sshClient = null;

        close(this.sftpClient);
        this.sftpClient = null;

        close(this.clientSession);
        this.clientSession = null;
    }

    private void close(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close resource", e);
        }
    }
}