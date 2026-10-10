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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the signer against AWS's published "Signature Calculation Examples" for S3, so a change to the canonical form
 * fails here rather than as an opaque {@code SignatureDoesNotMatch} against a real bucket.
 */
class AwsV4SignerTest {

    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final Instant SIGNING_TIME =
            ZonedDateTime.of(2013, 5, 24, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

    @Test
    @DisplayName("The documented GET Object example produces the documented signature")
    void getObjectExampleMatchesPublishedSignature() {
        AwsV4Signer signer = new AwsV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1");

        Map<String, String> signed = signer.sign(
                "GET",
                "examplebucket.s3.amazonaws.com",
                "/test.txt",
                "",
                Map.of("range", "bytes=0-9"),
                new byte[0],
                SIGNING_TIME);

        assertEquals(
                "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request, "
                        + "SignedHeaders=host;range;x-amz-content-sha256;x-amz-date, "
                        + "Signature=f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41",
                signed.get("Authorization"));
    }

    @Test
    @DisplayName("The documented pre-signed URL example produces the documented signature")
    void presignedUrlExampleMatchesPublishedSignature() {
        AwsV4Signer signer = new AwsV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1");

        String query = signer.presignGet(
                "examplebucket.s3.amazonaws.com", "/test.txt", Duration.ofSeconds(86400), SIGNING_TIME);

        assertEquals(
                "X-Amz-Algorithm=AWS4-HMAC-SHA256"
                        + "&X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20130524%2Fus-east-1%2Fs3%2Faws4_request"
                        + "&X-Amz-Date=20130524T000000Z"
                        + "&X-Amz-Expires=86400"
                        + "&X-Amz-SignedHeaders=host"
                        + "&X-Amz-Signature=aeeed9bbccd4d02ee5c0109b86d86835f995330da4c265957d157751f604d404",
                query);
    }

    @Test
    @DisplayName("A pre-signed URL cannot outlive the seven days S3 allows")
    void presignedExpiryIsCapped() {
        AwsV4Signer signer = new AwsV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1");

        String query = signer.presignGet("b.s3.amazonaws.com", "/k.zip", Duration.ofDays(30), SIGNING_TIME);

        assertTrue(query.contains("X-Amz-Expires=604800"), query);
    }

    @Test
    @DisplayName("An empty payload hashes to the documented constant")
    void emptyPayloadHash() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                AwsV4Signer.hex(AwsV4Signer.sha256(new byte[0])));
    }

    // The expected values below were computed once with botocore 1.43.111 (S3SigV4Auth), an independent SigV4
    // implementation, for the same keys, time and requests. Each request is built the way S3Client builds it.

    private static final BucketEndpoint BUCKET = BucketEndpoint.forAws("examplebucket", "us-east-1");
    private static final String KEY = "backups/1000.zip";

    private static String signature(String method, String key, Map<String, String> query, String payload) {
        AwsV4Signer signer = new AwsV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1");
        String authorization = signer.sign(
                        method,
                        BUCKET.host(),
                        BUCKET.pathOf(key),
                        PercentEncoding.query(query),
                        Map.of(),
                        payload.getBytes(StandardCharsets.UTF_8),
                        SIGNING_TIME)
                .get("Authorization");
        return authorization.substring(authorization.indexOf("Signature=") + "Signature=".length());
    }

    @Test
    @DisplayName("A PUT signs the hash of its payload")
    void putWithPayloadMatchesBotocore() {
        assertEquals(
                "d7e7582c4503f7922b4aa459c0d2570d4b6dfaa13b164582ce0f2b9bb0f464ed",
                signature("PUT", KEY, Map.of(), "Welcome to Amazon S3."));
    }

    @Test
    @DisplayName("The multipart upload requests sign their query parameters")
    void multipartRequestsMatchBotocore() {
        Map<String, String> part = Map.of("partNumber", "2", "uploadId", "abc/def+ghi=");
        assertEquals("uploads=", PercentEncoding.query(Map.of("uploads", "")));
        assertEquals("partNumber=2&uploadId=abc%2Fdef%2Bghi%3D", PercentEncoding.query(part));

        assertEquals(
                "1ecd4811f99f8558a34527535a2f839849ed8b2cdf101f54f21974fc818f64e8",
                signature("POST", KEY, Map.of("uploads", ""), ""));
        assertEquals(
                "7b7613606f414f01213c0eec89153638cdbb0b3395644b419d6d92006b0ca7a5",
                signature("PUT", KEY, part, "part two"));
        assertEquals(
                "7072033aeafdd0e02304d6733d431543e18be00dff019dfe0f61f671b051ccca",
                signature(
                        "POST",
                        KEY,
                        Map.of("uploadId", "abc/def+ghi="),
                        "<CompleteMultipartUpload><Part><PartNumber>1</PartNumber><ETag>\"e1\"</ETag></Part>"
                                + "</CompleteMultipartUpload>"));
    }

    @Test
    @DisplayName("A key with a space, a plus and non-ASCII characters is encoded and signed like botocore")
    void encodedKeyMatchesBotocore() {
        String key = "backups/my world+1/\u00e9t\u00e9.zip";

        assertEquals("/backups/my%20world%2B1/%C3%A9t%C3%A9.zip", BUCKET.pathOf(key));
        assertEquals(
                "66d111f58b667fc8d98a191f3ad6a30b83001b83a8016aedfe988179e6cdd893",
                signature("GET", key, Map.of(), ""));
    }
}
