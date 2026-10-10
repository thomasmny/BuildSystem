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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.eintosti.buildsystem.world.backup.storage.s3.S3Client.S3Object;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs the hand-written S3 client against Versity's S3 gateway, which checks the signatures and parses nothing of
 * ours, so a wrong canonical request or a misread response fails here. Needs Docker: run with {@code ./gradlew dockerTest}.
 */
@NullMarked
@Tag("docker")
class S3ClientDockerTest {

    private static final String ACCESS_KEY = "buildsystem";
    private static final String SECRET_KEY = "buildsystem-secret";
    private static final String BUCKET = "backups";
    private static final int PAGED_KEYS = 1001;

    // Serves the directories under /mnt as buckets and the files in them as objects.
    private static final GenericContainer<?> GATEWAY = new GenericContainer<>(DockerImageName.parse(
                    "versity/versitygw:v1.8.0@sha256:30292fc2eeacc67a36993b01f7a7a5e3361a19cced0e80c1d71cfa2a4b0a2499"))
            .withCommand("--access", ACCESS_KEY, "--secret", SECRET_KEY, "posix", "/mnt")
            .withExposedPorts(7070)
            .waitingFor(Wait.forListeningPort());

    @TempDir
    Path tempDir;

    private S3Client client;

    @BeforeAll
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    static void startGateway() throws Exception {
        GATEWAY.start();
        // The bucket, and more keys than one listing page holds, written as files rather than uploaded one at a time.
        String paged = "/mnt/" + BUCKET + "/paged";
        ExecResult result = GATEWAY.execInContainer(
                "sh",
                "-c",
                "mkdir -p " + paged + " && i=1; while [ $i -le " + PAGED_KEYS + " ]; do echo $i > " + paged
                        + "/$i; i=$((i+1)); done");
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Creating the bucket failed: " + result.getStderr());
        }
    }

    @AfterAll
    static void stopGateway() {
        GATEWAY.stop();
    }

    private static S3Client client(String secretKey) {
        URI endpoint = URI.create("http://" + GATEWAY.getHost() + ":" + GATEWAY.getMappedPort(7070));
        return new S3Client(ACCESS_KEY, secretKey, "us-east-1", BUCKET, endpoint);
    }

    @BeforeEach
    void setUp() {
        client = client(SECRET_KEY);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    private Path file(String name, byte[] content) throws IOException {
        return Files.write(tempDir.resolve(name), content);
    }

    private List<String> keys(String prefix) throws IOException {
        return client.list(prefix).stream().map(S3Object::key).toList();
    }

    @Test
    void uploadedObject_isListedDownloadedAndDeleted() throws IOException {
        byte[] content = "a world backup".getBytes(StandardCharsets.UTF_8);
        client.putFile("roundtrip/1000.zip", file("upload.zip", content), uploaded -> {});

        assertEquals(List.of("roundtrip/1000.zip"), keys("roundtrip/"));
        Path downloaded = tempDir.resolve("download.zip");
        client.get("roundtrip/1000.zip", downloaded);
        assertArrayEquals(content, Files.readAllBytes(downloaded));

        client.delete("roundtrip/1000.zip");
        assertEquals(List.of(), keys("roundtrip/"));
    }

    @Test
    void largeFile_isUploadedInParts() throws IOException {
        byte[] content = new byte[40 * 1024 * 1024];
        new Random(42).nextBytes(content);
        List<Long> progress = new ArrayList<>();

        client.putFile("multipart/large.zip", file("large.zip", content), progress::add);

        // Two full 16 MiB parts and the 8 MiB rest, each reported once S3 has accepted it.
        assertEquals(List.of(16L << 20, 32L << 20, 40L << 20), progress);
        Path downloaded = tempDir.resolve("large-download.zip");
        client.get("multipart/large.zip", downloaded);
        assertEquals(-1L, Files.mismatch(file("expected.zip", content), downloaded));
    }

    @Test
    void listing_followsContinuationTokens() throws IOException {
        assertEquals(PAGED_KEYS, client.list("paged/").size());
    }

    @Test
    void keyWithSpacesPlusAndNonAscii_roundTrips() throws IOException {
        String key = "encoded/my world+1/été.zip";
        byte[] content = "encoded".getBytes(StandardCharsets.UTF_8);
        client.putFile(key, file("encoded.zip", content), uploaded -> {});

        assertEquals(List.of(key), keys("encoded/"));
        Path downloaded = tempDir.resolve("encoded-download.zip");
        client.get(key, downloaded);
        assertArrayEquals(content, Files.readAllBytes(downloaded));
    }

    @Test
    void presignedUrl_servesTheObjectUntilItExpires() throws Exception {
        byte[] content = "shared".getBytes(StandardCharsets.UTF_8);
        client.putFile("presigned/share.zip", file("share.zip", content), uploaded -> {});
        String valid = client.presignedGetUrl("presigned/share.zip", Duration.ofMinutes(5));
        String expiring = client.presignedGetUrl("presigned/share.zip", Duration.ofSeconds(1));

        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<byte[]> response =
                    http.send(HttpRequest.newBuilder(URI.create(valid)).build(), BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode());
            assertArrayEquals(content, response.body());

            // Well past the one-second expiry, so a small clock difference with the container does not matter.
            Thread.sleep(5_000);
            int expired = http.send(HttpRequest.newBuilder(URI.create(expiring)).build(), BodyHandlers.discarding())
                    .statusCode();
            assertEquals(403, expired);
        }
    }

    @Test
    void wrongSecret_failsWithS3sErrorMessage() {
        try (S3Client wrong = client("not-the-secret")) {
            IOException failure = assertThrows(IOException.class, () -> wrong.list("roundtrip/"));
            assertTrue(failure.getMessage().contains("SignatureDoesNotMatch"), failure.getMessage());
        }
    }
}
