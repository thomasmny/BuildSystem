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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.backup.BackupImpl;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
public class SftpBackupStorage extends AbstractBackupStorage {

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(5);
    private static final int BUFFER_SIZE = 8192;

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

    public SftpBackupStorage(BuildSystemPlugin plugin, Executor executor, String host, int port, String username, String password, String remoteBasePath) {
        super(plugin, executor);

        this.host = host;
        this.port = validatePort(port);
        this.username = username;
        this.password = password;
        this.remoteBasePath = normalizeBasePath(remoteBasePath);
        this.tmpDownloadPath = FileUtils.resolve(plugin.getDataFolder().toPath(), ".tmp_backup_downloads");

        Security.addProvider(new BouncyCastleProvider());
        establishConnection();
    }

    @Override
    protected void onIoFailure() {
        disconnectAll();
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
        if (sshClient == null) {
            initializeSshClient();
        }

        try {
            clientSession = sshClient.connect(username, host, port)
                    .verify(CONNECTION_TIMEOUT.toMillis())
                    .getSession();

            if (clientSession == null) {
                throw new IllegalStateException();
            }

            clientSession.addPasswordIdentity(password);
            clientSession.auth().verify(AUTH_TIMEOUT.toMillis());

            sftpClient = SftpClientFactory.instance().createSftpClient(clientSession);
            plugin.getLogger().info("SFTP connection established successfully.");
        } catch (Exception e) {
            disconnectAll();
            plugin.getLogger().log(Level.SEVERE, "Failed to establish SFTP connection to " + host + ":" + port, e);
        }
    }

    private void initializeSshClient() {
        sshClient = SshClient.setUpDefaultClient();
        sshClient.setSignatureFactories(Arrays.asList(BuiltinSignatures.rsa, BuiltinSignatures.ed25519));
        sshClient.start();
    }

    private SftpClient getSftpClient() throws IOException {
        if (sftpClient == null || !sftpClient.isOpen()) {
            synchronized (this) {
                if (sftpClient == null || !sftpClient.isOpen()) {
                    establishConnection();
                }
            }
        }
        if (sftpClient == null || !sftpClient.isOpen()) {
            throw new IOException("SFTP connection could not be established");
        }
        return sftpClient;
    }

    private String getBackupDirectory(BuildWorld buildWorld) {
        return remoteBasePath + buildWorld.getUniqueId() + "/";
    }

    @Override
    protected synchronized List<Backup> doListBackups(BuildWorld buildWorld) throws IOException {
        List<Backup> backups = new ArrayList<>(plugin.getConfigService().current().world().backup().maxBackupsPerWorld());
        String backupDirectory = getBackupDirectory(buildWorld);

        SftpClient sftp = getSftpClient();
        createDirectoryIfNotExists(sftp, backupDirectory);

        for (DirEntry file : sftp.readDir(backupDirectory)) {
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

        return backups;
    }

    @Override
    public synchronized CompletableFuture<Backup> storeBackup(BuildWorld buildWorld) {
        return supply("store SFTP backup for " + buildWorld.getName(), () -> {
            long timestamp = System.currentTimeMillis();
            String backupDirectory = getBackupDirectory(buildWorld);
            String remotePath = backupDirectory + backupName(timestamp);

            byte[] zipBytes = FileUtils.zipWorldToMemory(buildWorld);

            SftpClient sftp = getSftpClient();
            createDirectoryIfNotExists(sftp, backupDirectory);

            try (
                    OutputStream out = sftp.write(remotePath);
                    BufferedOutputStream bufferedOut = new BufferedOutputStream(out, BUFFER_SIZE)
            ) {
                bufferedOut.write(zipBytes);
                bufferedOut.flush();
            }

            logDuration(buildWorld, timestamp);
            return new BackupImpl(plugin.getBackupService().getProfile(buildWorld), timestamp, remotePath);
        });
    }

    @Override
    public synchronized CompletableFuture<File> downloadBackup(Backup backup) {
        return supply("download SFTP backup " + backup.key(), () -> {
            SftpClient sftp = getSftpClient();
            Path target = tmpDownloadPath.resolve(UUID.randomUUID() + ".zip");

            try (
                    InputStream in = sftp.read(backup.key());
                    BufferedInputStream bufferedIn = new BufferedInputStream(in, BUFFER_SIZE);
                    OutputStream out = Files.newOutputStream(target);
                    BufferedOutputStream bufferedOut = new BufferedOutputStream(out, BUFFER_SIZE)
            ) {
                bufferedIn.transferTo(bufferedOut);
            }

            return target.toFile();
        });
    }

    @Override
    protected void doDeleteBackup(Backup backup) throws IOException {
        getSftpClient().remove(backup.key());
    }

    @Override
    public void close() {
        disconnectAll();
        try {
            FileUtils.deleteDirectory(tmpDownloadPath);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete temporary download directory", e);
        }
    }

    private void disconnectAll() {
        closeQuietly(sftpClient);
        sftpClient = null;
        closeQuietly(clientSession);
        clientSession = null;
        closeQuietly(sshClient);
        sshClient = null;
    }

    private void closeQuietly(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close resource", e);
        }
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
        String normalized = path.replace("\\", "/");
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (directoryExists(sftp, normalized)) {
            return;
        }
        int lastSeparator = normalized.lastIndexOf('/');
        if (lastSeparator > 0) {
            createDirectoriesRecursively(sftp, normalized.substring(0, lastSeparator));
        }
        sftp.mkdir(normalized);
    }

    private boolean directoryExists(SftpClient sftp, String path) {
        try {
            return sftp.stat(path).isDirectory();
        } catch (IOException e) {
            return false;
        }
    }
}
