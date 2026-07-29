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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.NullMarked;

/**
 * AWS Signature Version 4 signing, the only part of talking to S3 that is not a plain HTTP request. Implemented here
 * rather than pulled from the AWS SDK, which costs ~25 MB in the shaded jar to serve four operations.
 *
 * <p>The algorithm is fully specified and deterministic: build a canonical form of the request, hash it, sign the
 * hash with a key derived from the secret plus the date, region and service, and put the result in an
 * {@code Authorization} header. See {@code AwsV4SignerTest} for the published test vector this is pinned against.
 */
@NullMarked
final class AwsV4Signer {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String SERVICE = "s3";
    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter SCOPE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final String accessKey;
    private final String secretKey;
    private final String region;

    AwsV4Signer(String accessKey, String secretKey, String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
    }

    /**
     * Signs a request, returning the headers to send. The returned map contains the caller's headers plus
     * {@code x-amz-date}, {@code x-amz-content-sha256} and {@code Authorization}.
     *
     * @param method The HTTP method
     * @param host The request host, which must match the {@code Host} header actually sent
     * @param canonicalUri The already-encoded path, beginning with {@code /}
     * @param canonicalQuery The already-encoded and sorted query string, or empty
     * @param headers Additional headers to sign
     * @param payload The request body, empty for requests without one
     * @param timestamp The signing time
     * @return The headers to send, including the signature
     */
    Map<String, String> sign(
            String method,
            String host,
            String canonicalUri,
            String canonicalQuery,
            Map<String, String> headers,
            byte[] payload,
            Instant timestamp) {
        String amzDate = AMZ_DATE.format(timestamp);
        String scopeDate = SCOPE_DATE.format(timestamp);
        String payloadHash = hex(sha256(payload));

        SortedMap<String, String> canonicalHeaders = new TreeMap<>();
        headers.forEach((name, value) -> {
            requireHeaderSafe(name);
            requireHeaderSafe(value);
            canonicalHeaders.put(name.toLowerCase(Locale.ROOT), value.trim());
        });
        canonicalHeaders.put("host", host);
        canonicalHeaders.put("x-amz-content-sha256", payloadHash);
        canonicalHeaders.put("x-amz-date", amzDate);

        StringBuilder headerBlock = new StringBuilder();
        canonicalHeaders.forEach((name, value) ->
                headerBlock.append(name).append(':').append(value).append('\n'));
        String signedHeaders = String.join(";", canonicalHeaders.keySet());

        String canonicalRequest = method
                + '\n'
                + canonicalUri
                + '\n'
                + canonicalQuery
                + '\n'
                + headerBlock
                + '\n'
                + signedHeaders
                + '\n'
                + payloadHash;

        String scope = scopeDate + "/" + region + "/" + SERVICE + "/aws4_request";
        String stringToSign = ALGORITHM
                + '\n'
                + amzDate
                + '\n'
                + scope
                + '\n'
                + hex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));

        byte[] signingKey = signingKey(scopeDate);
        String signature = hex(hmac(signingKey, stringToSign));

        Map<String, String> signed = new LinkedHashMap<>(headers);
        signed.put("x-amz-date", amzDate);
        signed.put("x-amz-content-sha256", payloadHash);
        signed.put(
                "Authorization",
                ALGORITHM + " Credential=" + accessKey + "/" + scope + ", SignedHeaders=" + signedHeaders
                        + ", Signature=" + signature);
        return signed;
    }

    /**
     * Rejects a header name or value containing a line break. The canonical request is newline-delimited, so an
     * injected break would let a caller forge the signed content.
     */
    private static void requireHeaderSafe(String value) {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("S3 request headers cannot contain line breaks");
        }
    }

    /**
     * {@return the date/region/service-scoped signing key} Derived rather than stored, so the long-lived secret is
     * never used to sign directly.
     */
    private byte[] signingKey(String scopeDate) {
        byte[] key = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), scopeDate);
        key = hmac(key, region);
        key = hmac(key, SERVICE);
        return hmac(key, "aws4_request");
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 is required to sign S3 requests", e);
        }
    }

    static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to sign S3 requests", e);
        }
    }

    static String hex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
