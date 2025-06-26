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
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class S3BackupStorage implements BackupStorage {

    private final BuildSystemPlugin plugin;
    private final S3Client s3Client;
    private final String bucket;
    private final String pathPrefix;

    public S3BackupStorage(BuildSystemPlugin plugin, String accessKey, String secretKey, String region, String bucket, String pathPrefix) {
        this.plugin = plugin;
        this.bucket = bucket;
        this.pathPrefix = pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/";
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    private boolean isValidS3Key(String key) {
        return key.endsWith(".zip");
    }

    private String getBackupDirectory(BuildWorld buildWorld) {
        return this.pathPrefix + buildWorld.getUniqueId() + "/";
    }

    private Path getTmpDownloadDirectory() {
        return FileUtils.resolve(plugin.getDataFolder().toPath(), "backups");
    }

    @Override
    public List<Backup> listBackups(BackupProfile owner, BuildWorld buildWorld) {
        List<Backup> backups = new ArrayList<>(Config.World.Backup.maxBackupsPerWorld);

        try {
            ListObjectsV2Response response = this.s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(this.bucket)
                    .prefix(getBackupDirectory(buildWorld))
                    .build());

            backups.addAll(
                    response.contents().stream()
                            .filter(object -> isValidS3Key(object.key()))
                            .map(object -> new BackupImpl(owner, object.lastModified().toEpochMilli(), object.key()))
                            .toList()
            );
        } catch (S3Exception | SdkClientException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while listing backups", e);
            return Collections.emptyList();
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
            future.completeExceptionally(new RuntimeException("Failed to complete the backup for " + buildWorld.getName()));
            return;
        }

        long currentTime = System.currentTimeMillis();
        String key = getBackupDirectory(buildWorld) + getBackupName(currentTime);

        try {
            this.s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(this.bucket)
                            .key(key)
                            .build(),
                    RequestBody.fromBytes(zipBytes)
            );
            future.complete(new BackupImpl(owner, currentTime, key));
        } catch (S3Exception | SdkClientException e) {
            future.completeExceptionally(new RuntimeException("Failed to upload backup for " + buildWorld.getName(), e));
        }
    }

    @Override
    public File downloadBackup(Backup backup) {
        Path targetPath = getTmpDownloadDirectory().resolve(Paths.get(backup.key()).getFileName().toString());

        try {
            this.s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(this.bucket)
                            .key(backup.key())
                            .build(),
                    targetPath
            );
        } catch (S3Exception | SdkClientException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while downloading backup: " + backup.key(), e);
            return null;
        }

        return targetPath.toFile();
    }

    @Override
    public void deleteBackup(Backup backup) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                this.s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(this.bucket)
                        .key(backup.key())
                        .build());
            });
        } catch (S3Exception | SdkClientException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to delete backup " + backup.key(), e);
        }
    }
}
