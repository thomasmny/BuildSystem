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
    @DisplayName("An empty payload hashes to the documented constant")
    void emptyPayloadHash() {
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                AwsV4Signer.hex(AwsV4Signer.sha256(new byte[0])));
    }

    @Test
    @DisplayName("The signature covers the payload, so a changed body changes the signature")
    void signatureCoversPayload() {
        AwsV4Signer signer = new AwsV4Signer(ACCESS_KEY, SECRET_KEY, "eu-central-1");

        String first = signer.sign(
                        "PUT",
                        "b.s3.eu-central-1.amazonaws.com",
                        "/k.zip",
                        "",
                        Map.of(),
                        "one".getBytes(StandardCharsets.UTF_8),
                        SIGNING_TIME)
                .get("Authorization");
        String second = signer.sign(
                        "PUT",
                        "b.s3.eu-central-1.amazonaws.com",
                        "/k.zip",
                        "",
                        Map.of(),
                        "two".getBytes(StandardCharsets.UTF_8),
                        SIGNING_TIME)
                .get("Authorization");

        assertNotEquals(first, second, "payload must be part of what is signed");
    }
}
