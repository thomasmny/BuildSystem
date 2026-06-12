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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@NullMarked
public class S3BackupStorage extends AbstractBackupStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String pathPrefix;
    private final Path tmpDownloadDirectory;

    public S3BackupStorage(BuildSystemPlugin plugin, Executor executor, @Nullable String url, String accessKey, String secretKey, String region, String bucket, String pathPrefix) {
        super(plugin, executor);

        this.bucket = bucket;
        this.pathPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
        this.tmpDownloadDirectory = FileUtils.resolve(plugin.getDataFolder(), ".tmp_backup_downloads");

        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region));

        if (url != null && !url.isEmpty()) {
            builder = builder.region(Region.of(region)).endpointOverride(URI.create(url));
        }

        this.s3Client = builder.build();
    }

    private String getBackupDirectory(BuildWorld buildWorld) {
        return pathPrefix + buildWorld.getUniqueId() + "/";
    }

    @Override
    protected List<Backup> doListBackups(BuildWorld buildWorld) {
        List<Backup> backups = new ArrayList<>(plugin.getConfigService().current().world().backup().maxBackupsPerWorld());
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(getBackupDirectory(buildWorld))
                    .build());

            backups.addAll(response.contents().stream()
                    .filter(object -> object.key().endsWith(".zip"))
                    .map(object -> new BackupImpl(
                            plugin.getBackupService().getProfile(buildWorld),
                            object.lastModified().toEpochMilli(),
                            object.key()
                    ))
                    .toList());
        } catch (S3Exception | SdkClientException e) {
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
                s3Client.putObject(
                        PutObjectRequest.builder().bucket(bucket).key(key).build(),
                        RequestBody.fromBytes(zipBytes)
                );
            } catch (S3Exception | SdkClientException e) {
                throw new IOException("Failed to upload S3 backup for " + buildWorld.getName(), e);
            }

            logDuration(buildWorld, timestamp);
            return new BackupImpl(plugin.getBackupService().getProfile(buildWorld), timestamp, key);
        });
    }

    @Override
    public CompletableFuture<File> downloadBackup(Backup backup) {
        return supply("download S3 backup " + backup.key(), () -> {
            try {
                Path target = tmpDownloadDirectory.resolve(UUID.randomUUID() + ".zip");
                s3Client.getObject(
                        GetObjectRequest.builder().bucket(bucket).key(backup.key()).build(),
                        target
                );
                return target.toFile();
            } catch (S3Exception | SdkClientException e) {
                throw new IOException("Failed to download S3 backup: " + backup.key(), e);
            }
        });
    }

    @Override
    protected void doDeleteBackup(Backup backup) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(backup.key()).build());
        } catch (S3Exception | SdkClientException e) {
            throw new RuntimeException("Unable to delete S3 backup " + backup.key(), e);
        }
    }

    @Override
    public void close() {
        try {
            s3Client.close();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error while closing S3 client", e);
        }
        try {
            FileUtils.deleteDirectory(tmpDownloadDirectory);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete temporary download directory", e);
        }
    }
}
