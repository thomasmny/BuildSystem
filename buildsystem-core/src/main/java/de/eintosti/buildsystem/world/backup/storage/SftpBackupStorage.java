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
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.api.world.backup.BackupStorage;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.backup.BackupImpl;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.bukkit.Bukkit;

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

    private volatile SshClient sshClient;

    public SftpBackupStorage(BuildSystemPlugin plugin, String host, int port, String username, String password, String remoteBasePath) {
        this.plugin = plugin;
        this.host = host;
        this.port = validatePort(port);
        this.username = username;
        this.password = password;
        this.remoteBasePath = normalizeBasePath(remoteBasePath);
        this.tmpDownloadPath = FileUtils.resolve(plugin.getDataFolder().toPath(), ".tmp_backup_downloads");

        initializeSshClient();
    }

    private static int validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        return port;
    }

    private static String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.trim().isEmpty()) {
            return "/";
        }
        String normalized = basePath.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private void initializeSshClient() {
        this.sshClient = SshClient.setUpDefaultClient();
        this.sshClient.start();
    }

    private SftpClient connect() throws IOException {
        if (this.sshClient == null || this.sshClient.isClosed()) {
            initializeSshClient();
        }

        try {
            ClientSession session = this.sshClient.connect(this.username, this.host, this.port)
                    .verify(CONNECTION_TIMEOUT.toMillis())
                    .getSession();

            session.addPasswordIdentity(this.password);
            session.auth().verify(AUTH_TIMEOUT.toMillis());

            return SftpClientFactory.instance().createSftpClient(session);
        } catch (Exception e) {
            throw new IOException("Failed to establish SFTP connection to " + this.host + ":" + this.port, e);
        }
    }

    private String getBackupDirectory(BuildWorld buildWorld) {
        return remoteBasePath + buildWorld.getUniqueId() + "/";
    }

    @Override
    public List<Backup> listBackups(BackupProfile owner, BuildWorld buildWorld) {
        List<Backup> backups = new ArrayList<>(Config.World.Backup.maxBackupsPerWorld);
        String backupDirectory = getBackupDirectory(buildWorld);

        try (SftpClient sftp = connect()) {
            if (!directoryExists(sftp, backupDirectory)) {
                plugin.getLogger().warning("SFTP backup directory does not exist: " + backupDirectory);
                return backups;
            }

            Iterable<DirEntry> files = sftp.readDir(backupDirectory);
            for (SftpClient.DirEntry file : files) {
                if (file.getFilename().endsWith(".zip")) {
                    long creationTime = file.getAttributes().getCreateTime().toMillis();
                    String fullPath = backupDirectory + file.getFilename();
                    backups.add(new BackupImpl(owner, creationTime, fullPath));
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to list SFTP backups", e);
        }

        backups.sort(Comparator.comparingLong(Backup::creationTime).reversed());
        return backups;
    }

    @Override
    public void storeBackup(BackupProfile owner, BuildWorld buildWorld, CompletableFuture<Backup> future) {
        byte[] zipBytes;
        try {
            zipBytes = FileUtils.zipWorldToMemory(buildWorld);
        } catch (IOException e) {
            future.completeExceptionally(new RuntimeException("Failed to zip world: " + buildWorld.getName()));
            return;
        }

        long timestamp = System.currentTimeMillis();
        String backupName = getBackupName(timestamp);
        String backupDir = getBackupDirectory(buildWorld);
        String remotePath = backupDir + backupName;

        try (SftpClient sftp = connect()) {
            createDirectoryIfNotExists(sftp, backupDir);

            try (
                    OutputStream out = sftp.write(remotePath);
                    BufferedOutputStream bufferedOut = new BufferedOutputStream(out, BUFFER_SIZE)
            ) {
                bufferedOut.write(zipBytes);
                bufferedOut.flush();
            }
            future.complete(new BackupImpl(owner, timestamp, remotePath));
        } catch (IOException e) {
            future.completeExceptionally(new RuntimeException("Failed to upload SFTP backup", e));
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

    private void createDirectoriesRecursively(SftpClient sftp, String path) throws IOException {
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
    public File downloadBackup(Backup backup) {
        Path target = tmpDownloadPath.resolve(UUID.randomUUID() + ".zip");

        try (
                SftpClient sftp = connect();
                InputStream in = sftp.read(backup.key());
                BufferedInputStream bufferedIn = new BufferedInputStream(in, BUFFER_SIZE);
                OutputStream out = Files.newOutputStream(target);
                BufferedOutputStream bufferedOut = new BufferedOutputStream(out, BUFFER_SIZE)
        ) {
            bufferedIn.transferTo(bufferedOut);
            return target.toFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to download SFTP backup: " + backup.key(), e);
            return null;
        }
    }

    @Override
    public void deleteBackup(Backup backup) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (SftpClient sftp = connect()) {
                    sftp.remove(backup.key());
                } catch (IOException e) {
                    plugin.getLogger().warning("Backup file not found for deletion: " + backup.key());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete SFTP backup: " + backup.key(), e);
        }
    }

    @Override
    public void close() {
        if (this.sshClient != null && !this.sshClient.isClosed()) {
            try {
                this.sshClient.stop();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to properly close SSH client", e);
            }
        }

        FileUtils.deleteDirectory(this.tmpDownloadPath);
    }
}