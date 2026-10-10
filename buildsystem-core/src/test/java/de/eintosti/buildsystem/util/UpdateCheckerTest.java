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
package de.eintosti.buildsystem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.util.UpdateChecker.ReleaseRequest;
import de.eintosti.buildsystem.util.UpdateChecker.UpdateReason;
import de.eintosti.buildsystem.util.UpdateChecker.UpdateResult;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
class UpdateCheckerTest {

    private static final String RELEASE_URL = "https://github.com/thomasmny/BuildSystem/releases/tag/4.1.0";

    private final List<Runnable> submitted = new ArrayList<>();
    private final Deque<HttpResponse<String>> responses = new ArrayDeque<>();
    private final List<@Nullable String> sentEtags = new ArrayList<>();

    @Test
    void newerTag_isAnUpdateWithItsReleasePage() {
        responses.add(release("4.1.0"));

        UpdateResult result = check(checker(Duration.ofHours(1)));

        assertEquals(UpdateReason.NEW_UPDATE, result.getReason());
        assertEquals("4.1.0", result.getNewestVersion());
        assertEquals(RELEASE_URL, result.getReleaseUrl());
    }

    @Test
    void sameTag_isUpToDate() {
        responses.add(release("4.0.0"));

        assertEquals(
                UpdateReason.UP_TO_DATE, check(checker(Duration.ofHours(1))).getReason());
    }

    @Test
    void leadingV_isIgnored() {
        responses.add(release("v4.0.0"));

        assertEquals(
                UpdateReason.UP_TO_DATE, check(checker(Duration.ofHours(1))).getReason());
    }

    @Test
    void answer_isReusedForAnHour() {
        responses.add(release("4.0.0"));
        UpdateChecker checker = checker(Duration.ofHours(1));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertSame(first, checker.requestUpdateCheck());
        assertEquals(1, sentEtags.size());
    }

    @Test
    void notModified_reusesTheCachedReleaseAndSendsItsEtag() {
        responses.add(release("4.1.0"));
        responses.add(response(304, "", null));
        UpdateChecker checker = checker(Duration.ZERO);

        check(checker);
        UpdateResult result = check(checker);

        assertEquals(UpdateReason.NEW_UPDATE, result.getReason());
        assertEquals(RELEASE_URL, result.getReleaseUrl());
        assertEquals(Arrays.asList(null, "\"etag\""), sentEtags);
    }

    @Test
    void rateLimit_isAFailureThatIsRetried() {
        responses.add(response(403, "{\"message\": \"API rate limit exceeded\"}", null));
        UpdateChecker checker = checker(Duration.ofHours(1));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.RATE_LIMITED, first.join().getReason());
        assertNotSame(first, checker.requestUpdateCheck());
    }

    @Test
    void malformedJson_isAFailureThatIsRetried() {
        responses.add(response(200, "{\"tag_name\": ", null));
        UpdateChecker checker = checker(Duration.ofHours(1));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.INVALID_JSON, first.join().getReason());
        assertNotSame(first, checker.requestUpdateCheck());
    }

    @Test
    void unreachableGitHub_isAFailureThatIsRetried() {
        UpdateChecker checker = new UpdateChecker(
                plugin(),
                etag -> {
                    throw new IOException("offline");
                },
                submitted::add,
                Duration.ofHours(1));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.COULD_NOT_CONNECT, first.join().getReason());
        assertNotSame(first, checker.requestUpdateCheck());
    }

    private UpdateChecker checker(Duration cacheFor) {
        ReleaseRequest request = etag -> {
            sentEtags.add(etag);
            return responses.removeFirst();
        };
        return new UpdateChecker(plugin(), request, submitted::add, cacheFor);
    }

    private static JavaPlugin plugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("BuildSystem", "4.0.0", "Main"));
        return plugin;
    }

    private UpdateResult check(UpdateChecker checker) {
        CompletableFuture<UpdateResult> future = checker.requestUpdateCheck();
        runSubmitted();
        return future.join();
    }

    private void runSubmitted() {
        List<Runnable> tasks = List.copyOf(submitted);
        submitted.clear();
        tasks.forEach(Runnable::run);
    }

    private static HttpResponse<String> release(String tag) {
        return response(200, "{\"tag_name\": \"%s\", \"html_url\": \"%s\"}".formatted(tag, RELEASE_URL), "\"etag\"");
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body, @Nullable String etag) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers())
                .thenReturn(
                        HttpHeaders.of(etag == null ? Map.of() : Map.of("ETag", List.of(etag)), (name, value) -> true));
        return response;
    }
}
