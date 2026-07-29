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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A minimal S3 client covering the four operations the backup storage needs: list, put, get and delete. Written
 * against the S3 REST API on the JDK's {@link HttpClient} rather than pulling in the AWS SDK, which costs ~25 MB in
 * the shaded jar and ships the generated model for every S3 operation to serve these four.
 *
 * <p>Requests are signed with {@link AwsV4Signer} and addressed through a {@link BucketEndpoint}, which decides
 * virtual-hosted versus path-style addressing once so the signed host and path always match the request.
 */
@NullMarked
public final class S3Client implements AutoCloseable {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);

    private final HttpClient http;
    private final AwsV4Signer signer;
    private final BucketEndpoint endpoint;

    /**
     * An object listed in a bucket.
     *
     * @param key The full object key
     * @param lastModified When the object was last written
     */
    public record S3Object(String key, Instant lastModified) {}

    /**
     * Creates a client.
     *
     * @param accessKey The access key id
     * @param secretKey The secret access key
     * @param region The bucket's region
     * @param bucket The bucket name
     * @param serviceEndpoint An S3-compatible service endpoint, or {@code null} to address AWS directly
     */
    public S3Client(String accessKey, String secretKey, String region, String bucket, @Nullable URI serviceEndpoint) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                // The signature is bound to the host, so a redirect could never be satisfied by replaying this
                // request, and following one would hand the Authorization header to a different origin.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.signer = new AwsV4Signer(accessKey, secretKey, region);
        this.endpoint = serviceEndpoint == null
                ? BucketEndpoint.forAws(bucket, region)
                : BucketEndpoint.forCustomService(serviceEndpoint, bucket);
    }

    /**
     * Lists every object under a prefix, following continuation tokens so a bucket holding more than one page is not
     * silently truncated.
     *
     * @param prefix The key prefix to list under
     * @return The objects found
     * @throws IOException If the request fails or S3 returns an error
     */
    public List<S3Object> list(String prefix) throws IOException {
        List<S3Object> objects = new ArrayList<>();
        String continuationToken = null;

        do {
            Map<String, String> query = new TreeMap<>();
            query.put("list-type", "2");
            query.put("prefix", prefix);
            if (continuationToken != null) {
                query.put("continuation-token", continuationToken);
            }

            byte[] xml = body(request("GET", "", query, Payload.empty(), BodyHandlers.ofByteArray()), "list " + prefix);
            Listing listing = Listing.parse(xml);
            objects.addAll(listing.objects());
            continuationToken = listing.nextContinuationToken();
        } while (continuationToken != null);

        return objects;
    }

    /**
     * Uploads an object, replacing anything already at the key.
     *
     * @param key The object key
     * @param content The object body
     * @throws IOException If the request fails or S3 returns an error
     */
    public void put(String key, byte[] content) throws IOException {
        body(request("PUT", key, Map.of(), Payload.of(content), BodyHandlers.ofByteArray()), "upload " + key);
    }

    /**
     * Downloads an object to a file, streaming it rather than buffering: a world backup can be far larger than the
     * heap the server has spare.
     *
     * @param key The object key
     * @param target The file to write
     * @throws IOException If the request fails or S3 returns an error
     */
    public void get(String key, Path target) throws IOException {
        HttpResponse<Path> response = request("GET", key, Map.of(), Payload.empty(), BodyHandlers.ofFile(target));
        if (!isSuccess(response)) {
            // The handler has already written the error document to the target; it is not a backup.
            Files.deleteIfExists(target);
            throw new IOException(failureMessage("download " + key, response.statusCode(), new byte[0]));
        }
    }

    /**
     * Deletes an object. S3 treats deleting a key that is not there as success, and so does this.
     *
     * @param key The object key
     * @throws IOException If the request fails or S3 returns an error
     */
    public void delete(String key) throws IOException {
        body(request("DELETE", key, Map.of(), Payload.empty(), BodyHandlers.ofByteArray()), "delete " + key);
    }

    private <T> HttpResponse<T> request(
            String method, String key, Map<String, String> query, Payload payload, BodyHandler<T> handler)
            throws IOException {
        String canonicalQuery = canonicalQuery(query);
        Map<String, String> headers = signer.sign(
                method,
                endpoint.host(),
                endpoint.pathOf(key),
                canonicalQuery,
                Map.of(),
                payload.bytes(),
                Instant.now());

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(endpoint.urlOf(key, canonicalQuery)))
                .timeout(REQUEST_TIMEOUT)
                .method(method, payload.publisher());
        headers.forEach(request::header);

        try {
            return http.send(request.build(), handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling S3", e);
        }
    }

    /**
     * {@return the response body, once the status is known to be a success}
     *
     * @param response The response to check
     * @param action What was attempted, for the error message
     * @throws IOException If S3 returned an error status
     */
    private static byte[] body(HttpResponse<byte[]> response, String action) throws IOException {
        if (!isSuccess(response)) {
            throw new IOException(failureMessage(action, response.statusCode(), response.body()));
        }
        return response.body();
    }

    private static boolean isSuccess(HttpResponse<?> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /**
     * {@return a message describing a failed request} S3 explains itself in an XML error document, which is far more
     * useful than the status alone, so it is included when there is one.
     */
    private static String failureMessage(String action, int status, byte[] errorDocument) {
        String detail =
                errorDocument.length == 0 ? "" : " - " + new String(errorDocument, StandardCharsets.UTF_8).trim();
        return "S3 request failed to " + action + " (HTTP " + status + ")" + detail;
    }

    /**
     * {@return the query string in the sorted, encoded form SigV4 requires}
     */
    private static String canonicalQuery(Map<String, String> query) {
        StringBuilder canonical = new StringBuilder();
        new TreeMap<>(query).forEach((name, value) -> {
            if (!canonical.isEmpty()) {
                canonical.append('&');
            }
            canonical.append(PercentEncoding.encode(name)).append('=').append(PercentEncoding.encode(value));
        });
        return canonical.toString();
    }

    @Override
    public void close() {
        http.close();
    }

    /**
     * A request body, pairing the bytes that get signed with the publisher that sends them so the two cannot drift.
     */
    private record Payload(byte[] bytes) {

        static Payload empty() {
            return new Payload(new byte[0]);
        }

        static Payload of(byte[] bytes) {
            return new Payload(bytes);
        }

        HttpRequest.BodyPublisher publisher() {
            return bytes.length == 0
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(bytes);
        }
    }
}
