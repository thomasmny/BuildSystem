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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.backup.Backup;
import de.eintosti.buildsystem.api.world.backup.BackupProfile;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.backup.BackupImpl;
import de.eintosti.buildsystem.world.backup.storage.s3.S3Client;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class S3BackupStorage extends AbstractBackupStorage {

    private final ConfigService configService;
    private final Function<BuildWorld, BackupProfile> profileProvider;
    private final S3Client s3Client;
    private final String pathPrefix;
    private final Path tmpDownloadDirectory;

    public S3BackupStorage(
            Logger logger,
            Executor executor,
            File dataFolder,
            ConfigService configService,
            Function<BuildWorld, BackupProfile> profileProvider,
            @Nullable String url,
            String accessKey,
            String secretKey,
            String region,
            String bucket,
            String pathPrefix) {
        super(logger, executor);

        this.configService = configService;
        this.profileProvider = profileProvider;
        this.pathPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
        this.tmpDownloadDirectory = FileUtils.resolve(dataFolder, ".tmp_backup_downloads");

        URI endpoint = url == null || url.isEmpty() ? null : URI.create(url);
        this.s3Client = new S3Client(accessKey, secretKey, region, bucket, endpoint);
    }

    private String getBackupDirectory(BuildWorld buildWorld) {
        return pathPrefix + buildWorld.getUniqueId() + "/";
    }

    @Override
    protected List<Backup> doListBackups(BuildWorld buildWorld) {
        List<Backup> backups =
                new ArrayList<>(configService.current().world().backup().maxBackupsPerWorld());
        try {
            backups.addAll(s3Client.list(getBackupDirectory(buildWorld)).stream()
                    .filter(object -> object.key().endsWith(".zip"))
                    .map(object -> new BackupImpl(
                            profileProvider.apply(buildWorld),
                            object.lastModified().toEpochMilli(),
                            object.key()))
                    .toList());
        } catch (IOException e) {
            throw new RuntimeException("Error while listing S3 backups", e);
        }
        return backups;
    }

    @Override
    public CompletableFuture<Backup> storeBackup(BuildWorld buildWorld) {
        return supply("store S3 backup for " + buildWorld.getName(), () -> {
            long timestamp = System.currentTimeMillis();
            String key = getBackupDirectory(buildWorld) + backupName(timestamp);

            byte[] zipBytes = FileUtils.zipWorldToMemory(buildWorld);

            try {
                s3Client.put(key, zipBytes);
            } catch (IOException e) {
                throw new IOException("Failed to upload S3 backup for " + buildWorld.getName(), e);
            }

            logDuration(buildWorld, timestamp);
            return new BackupImpl(profileProvider.apply(buildWorld), timestamp, key);
        });
    }

    @Override
    public CompletableFuture<File> downloadBackup(Backup backup) {
        return supply("download S3 backup " + backup.key(), () -> {
            try {
                Path target = tmpDownloadDirectory.resolve(UUID.randomUUID() + ".zip");
                s3Client.get(backup.key(), target);
                return target.toFile();
            } catch (IOException e) {
                throw new IOException("Failed to download S3 backup: " + backup.key(), e);
            }
        });
    }

    @Override
    protected void doDeleteBackup(Backup backup) {
        try {
            s3Client.delete(backup.key());
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete S3 backup " + backup.key(), e);
        }
    }

    @Override
    public void close() {
        try {
            s3Client.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while closing S3 client", e);
        }
        try {
            FileUtils.deleteDirectory(tmpDownloadDirectory);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete temporary download directory", e);
        }
    }
}
