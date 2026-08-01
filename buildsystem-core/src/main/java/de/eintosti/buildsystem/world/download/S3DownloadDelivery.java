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
package de.eintosti.buildsystem.world.download;

import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.world.backup.storage.s3.S3Client;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Uploads archives to the bucket the backups already use and hands out pre-signed links.
 *
 * <p>No port has to be opened, and the uplink carries the world once rather than once per player. The trade is that a
 * pre-signed URL is a plain bearer token: unlike a local link it cannot be pinned to one client or rate limited, so
 * whoever it is forwarded to can use it until it expires.
 *
 * <p>Credentials, bucket and endpoint come from {@code world.backup.storage.s3}, so the two features cannot drift
 * apart.
 */
@NullMarked
final class S3DownloadDelivery implements DownloadDelivery {

    /**
     * Where uploads live in the bucket, kept apart from the backups so a lifecycle rule can treat them differently.
     */
    private static final String KEY_PREFIX = "downloads/";

    private final S3Client s3;
    private final Logger logger;

    /**
     * The uploaded objects and when their links die. An object outlives the request that made it, so something has to
     * remember it is there; nothing else would ever delete it.
     */
    private final Map<String, Long> uploads = new ConcurrentHashMap<>();

    private S3DownloadDelivery(S3Client s3, Logger logger) {
        this.s3 = s3;
        this.logger = logger;
    }

    /**
     * {@return a delivery uploading to the bucket the backups use, or {@code null} if that is not configured} The
     * reason is logged: downloads silently doing nothing would be worse than downloads that are visibly off.
     *
     * @param configService The live configuration
     * @param logger The plugin logger
     * @param background Where the leftovers of a previous run are cleared, which must not hold up startup
     */
    static @Nullable S3DownloadDelivery open(ConfigService configService, Logger logger, Executor background) {
        if (!(configService.current().world().backup().storage() instanceof PluginConfig.World.Backup.S3 settings)) {
            logger.severe("world.download.storage is 's3' but backups are not stored on S3."
                    + " Set world.backup.storage.type to 's3', or world.download.storage back to 'local'."
                    + " World downloads are disabled.");
            return null;
        }

        String accessKey = settings.resolvedAccessKey();
        String secretKey = settings.resolvedSecretKey();
        if (isBlank(accessKey) || isBlank(secretKey) || isBlank(settings.region()) || isBlank(settings.bucket())) {
            logger.severe("World downloads are disabled:"
                    + " the S3 backup storage is missing its credentials, region or bucket.");
            return null;
        }

        String url = settings.url();
        S3Client s3 = new S3Client(
                accessKey, secretKey, settings.region(), settings.bucket(), isBlank(url) ? null : URI.create(url));
        logger.info("World downloads are served as pre-signed links from bucket '" + settings.bucket() + "'");
        S3DownloadDelivery delivery = new S3DownloadDelivery(s3, logger);
        background.execute(delivery::clearLeftovers);
        return delivery;
    }

    /**
     * Deletes every upload left in the bucket by a previous run. Their links did not survive the restart, so the
     * objects are unreachable and would otherwise be billed forever.
     *
     * <p>Assumes the prefix belongs to this server alone, the same assumption the local delivery makes about its
     * downloads directory.
     */
    private void clearLeftovers() {
        try {
            s3.list(KEY_PREFIX).forEach(object -> delete(object.key()));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to clear leftover world downloads from the bucket", e);
        }
    }

    @Override
    public String publish(Path archive, String fileName, Duration lifetime, ProgressListener progress)
            throws IOException {
        long total = Files.size(archive);
        // The signed URL is the secret, not the key. A directory per upload only keeps two exports of the same world
        // from colliding.
        String key = KEY_PREFIX + UUID.randomUUID() + "/" + fileName;

        try {
            s3.putFile(key, archive, uploaded -> progress.update(uploaded, total));
        } finally {
            // Uploaded or aborted, the local copy has no further use.
            Files.deleteIfExists(archive);
        }

        // Signed only once the bytes are up, so a long upload does not come out of the link's life.
        uploads.put(key, System.currentTimeMillis() + lifetime.toMillis());
        return s3.presignedGetUrl(key, lifetime);
    }

    /**
     * {@return {@link Long#MAX_VALUE}} The bucket has no budget to divide, so nothing is reserved here. The archive is
     * still staged on disk first, which {@link WorldDownloadService} bounds by the free space it finds there.
     */
    @Override
    public long reserve() {
        return Long.MAX_VALUE;
    }

    @Override
    public void release(long reserved) {
        // Nothing was reserved.
    }

    /**
     * {@return true} An upload runs at the speed of the server's uplink, which is slow enough that a player left
     * watching a full bar would read it as a hang.
     */
    @Override
    public boolean reportsPublishProgress() {
        return true;
    }

    @Override
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        uploads.entrySet().removeIf(upload -> {
            if (now <= upload.getValue()) {
                return false;
            }
            delete(upload.getKey());
            return true;
        });
    }

    /**
     * Closes the client without deleting what is still live, since that would hold up shutdown for a round trip per
     * object. The next start clears them instead.
     */
    @Override
    public void close() {
        uploads.clear();
        s3.close();
    }

    private void delete(String key) {
        try {
            s3.delete(key);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete the expired world download " + key, e);
        }
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
