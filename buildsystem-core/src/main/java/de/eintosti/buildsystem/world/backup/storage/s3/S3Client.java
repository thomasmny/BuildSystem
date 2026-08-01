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

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
import java.util.function.LongConsumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

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

    /**
     * Uploading a world archive is bounded by the server's uplink rather than by S3, so it gets its own budget: a
     * gigabyte over a slow connection legitimately takes longer than the timeout the small requests use.
     */
    private static final Duration UPLOAD_TIMEOUT = Duration.ofHours(2);

    /**
     * The part size used until a file is large enough to need bigger ones. Comfortably above the five megabytes S3
     * requires of every part but the last, and small enough that a failed part is cheap to lose.
     */
    private static final long MIN_PART_SIZE = 16L * 1024 * 1024;

    /**
     * The most parts S3 accepts in one upload.
     */
    private static final long MAX_PARTS = 10_000L;

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
     * Uploads a file as a multipart upload, streaming each part off disk so neither the heap nor the five-gigabyte
     * ceiling on a single {@code PUT} bounds how large the file may be.
     *
     * <p>A failed upload is aborted rather than abandoned. An incomplete multipart upload does not show up in a
     * listing but is still billed for.
     *
     * @param key The object key
     * @param file The file to upload
     * @param uploaded Notified with the running total of bytes accepted by S3, for reporting progress
     * @throws IOException If the request fails or S3 returns an error
     */
    public void putFile(String key, Path file, LongConsumer uploaded) throws IOException {
        long size = Files.size(file);
        long partSize = partSize(size);
        String uploadId = createMultipartUpload(key);

        try {
            List<String> etags = new ArrayList<>();
            long offset = 0L;
            while (offset < size || etags.isEmpty()) {
                long length = Math.min(partSize, size - offset);
                etags.add(uploadPart(key, uploadId, etags.size() + 1, file, offset, length));
                offset += length;
                uploaded.accept(offset);
            }
            completeMultipartUpload(key, uploadId, etags);
        } catch (IOException | RuntimeException e) {
            abortMultipartUpload(key, uploadId, e);
            throw e;
        }
    }

    /**
     * {@return the part size to split a file of {@code size} into} Grown when the file would otherwise need more than
     * the ten thousand parts S3 allows, so the part count is bounded rather than the file size.
     */
    static long partSize(long size) {
        long needed = (size + MAX_PARTS - 1) / MAX_PARTS;
        return Math.max(MIN_PART_SIZE, needed);
    }

    private String createMultipartUpload(String key) throws IOException {
        byte[] xml = body(
                request("POST", key, Map.of("uploads", ""), Payload.empty(), BodyHandlers.ofByteArray()),
                "start a multipart upload of " + key);
        return S3Xml.required(S3Xml.parse(xml, "multipart upload").getDocumentElement(), "UploadId");
    }

    /**
     * {@return the part's ETag, which the completion request must quote back}
     */
    private String uploadPart(String key, String uploadId, int partNumber, Path file, long offset, long length)
            throws IOException {
        Map<String, String> query = Map.of("partNumber", Integer.toString(partNumber), "uploadId", uploadId);
        HttpResponse<byte[]> response = request(
                "PUT",
                key,
                query,
                Payload.ofFileRange(file, offset, length),
                BodyHandlers.ofByteArray(),
                UPLOAD_TIMEOUT);
        body(response, "upload part " + partNumber + " of " + key);

        return response.headers()
                .firstValue("ETag")
                .orElseThrow(() -> new IOException("S3 did not return an ETag for part " + partNumber + " of " + key));
    }

    private void completeMultipartUpload(String key, String uploadId, List<String> etags) throws IOException {
        StringBuilder xml = new StringBuilder("<CompleteMultipartUpload>");
        for (int i = 0; i < etags.size(); i++) {
            xml.append("<Part><PartNumber>")
                    .append(i + 1)
                    .append("</PartNumber><ETag>")
                    .append(S3Xml.escape(etags.get(i)))
                    .append("</ETag></Part>");
        }
        xml.append("</CompleteMultipartUpload>");

        byte[] response = body(
                request(
                        "POST",
                        key,
                        Map.of("uploadId", uploadId),
                        Payload.of(xml.toString().getBytes(StandardCharsets.UTF_8)),
                        BodyHandlers.ofByteArray()),
                "complete the multipart upload of " + key);

        // S3 answers 200 and only then reports a failure inside the document, so the status alone proves nothing.
        Element root = S3Xml.parse(response, "multipart completion").getDocumentElement();
        if ("Error".equals(root.getTagName())) {
            throw new IOException("S3 failed to complete the multipart upload of " + key + " - "
                    + new String(response, StandardCharsets.UTF_8).trim());
        }
    }

    /**
     * Discards a half-finished upload, keeping the original failure as the one that surfaces.
     */
    private void abortMultipartUpload(String key, String uploadId, Throwable cause) {
        try {
            request("DELETE", key, Map.of("uploadId", uploadId), Payload.empty(), BodyHandlers.ofByteArray());
        } catch (IOException | RuntimeException e) {
            cause.addSuppressed(new IOException("Failed to abort the multipart upload of " + key, e));
        }
    }

    /**
     * {@return a URL that grants anyone holding it a single object for a limited time} Nothing is sent to S3 to make
     * one: the URL carries its own signature, so it can be handed out and followed by a plain browser.
     *
     * @param key The object key
     * @param expiry How long the URL stays valid, capped at the seven days S3 allows
     */
    public String presignedGetUrl(String key, Duration expiry) {
        String query = signer.presignGet(endpoint.host(), endpoint.pathOf(key), expiry, Instant.now());
        return endpoint.urlOf(key, query);
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
        HttpResponse<Path> response;
        try {
            response = request("GET", key, Map.of(), Payload.empty(), BodyHandlers.ofFile(target));
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }

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
        return request(method, key, query, payload, handler, REQUEST_TIMEOUT);
    }

    private <T> HttpResponse<T> request(
            String method,
            String key,
            Map<String, String> query,
            Payload payload,
            BodyHandler<T> handler,
            Duration timeout)
            throws IOException {
        String canonicalQuery = PercentEncoding.query(query);
        Map<String, String> headers = signer.sign(
                method, endpoint.host(), endpoint.pathOf(key), canonicalQuery, Map.of(), payload.hash(), Instant.now());

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(endpoint.urlOf(key, canonicalQuery)))
                .timeout(timeout)
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

    @Override
    public void close() {
        http.close();
    }

    /**
     * A request body, pairing the hash that gets signed with the publisher that sends it so the two cannot drift.
     */
    private record Payload(String hash, HttpRequest.BodyPublisher publisher) {

        static Payload empty() {
            return new Payload(AwsV4Signer.hex(AwsV4Signer.sha256(new byte[0])), HttpRequest.BodyPublishers.noBody());
        }

        static Payload of(byte[] bytes) {
            return new Payload(
                    AwsV4Signer.hex(AwsV4Signer.sha256(bytes)), HttpRequest.BodyPublishers.ofByteArray(bytes));
        }

        /**
         * {@return a payload streamed from a slice of a file} S3 rejects a chunked body, so the length is declared up
         * front and the bytes are read on demand, keeping the part out of memory.
         *
         * <p>Sent unsigned: hashing the body first would mean reading the archive a second time. The signature covers
         * the request but not its contents, which TLS protects in transit.
         *
         * @param file The file to read from
         * @param offset Where the slice starts
         * @param length How many bytes the slice holds
         */
        static Payload ofFileRange(Path file, long offset, long length) {
            HttpRequest.BodyPublisher slice = HttpRequest.BodyPublishers.ofInputStream(() -> {
                try {
                    InputStream in = Files.newInputStream(file);
                    in.skipNBytes(offset);
                    return ByteStreams.limit(in, length);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            return new Payload(AwsV4Signer.UNSIGNED_PAYLOAD, HttpRequest.BodyPublishers.fromPublisher(slice, length));
        }
    }
}
