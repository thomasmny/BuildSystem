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
package de.eintosti.buildsystem.world.backup.storage.s3;

import java.net.URI;
import org.jspecify.annotations.NullMarked;

/**
 * Where a bucket lives, and how it is addressed. AWS expects the bucket in the hostname
 * ({@code bucket.s3.region.amazonaws.com/key}); S3-compatible providers such as MinIO, Backblaze B2 and Cloudflare R2
 * expect it in the path ({@code endpoint/bucket/key}).
 *
 * <p>That choice is made once, here. The host is signed and the path is signed, so deciding it in more than one place
 * risks signing something other than the URL actually requested — which surfaces as an opaque
 * {@code SignatureDoesNotMatch}.
 *
 * @param scheme The URL scheme
 * @param host The host, including a non-default port, exactly as it must appear in the {@code Host} header
 * @param basePath The already-encoded path prefix every key sits under, empty when the bucket is in the hostname
 */
@NullMarked
record BucketEndpoint(String scheme, String host, String basePath) {

    /**
     * {@return an endpoint addressing an AWS bucket virtual-hosted style}
     *
     * @param bucket The bucket name
     * @param region The bucket's region
     */
    static BucketEndpoint forAws(String bucket, String region) {
        return new BucketEndpoint("https", bucket + ".s3." + region + ".amazonaws.com", "");
    }

    /**
     * {@return an endpoint addressing a bucket on an S3-compatible service path style}
     *
     * @param endpoint The service endpoint
     * @param bucket The bucket name
     */
    static BucketEndpoint forCustomService(URI endpoint, String bucket) {
        String host = endpoint.getPort() == -1 ? endpoint.getHost() : endpoint.getHost() + ":" + endpoint.getPort();
        return new BucketEndpoint(endpoint.getScheme(), host, "/" + PercentEncoding.encode(bucket));
    }

    /**
     * {@return the canonical path for a key} This is both what gets signed and what gets requested.
     *
     * @param key The object key, empty to address the bucket itself
     */
    String pathOf(String key) {
        return basePath + "/" + PercentEncoding.encodePath(key);
    }

    /**
     * {@return the absolute URL for a key}
     *
     * @param key The object key, empty to address the bucket itself
     * @param query The already-encoded query string, or empty
     */
    String urlOf(String key, String query) {
        return scheme + "://" + host + pathOf(key) + (query.isEmpty() ? "" : "?" + query);
    }
}
