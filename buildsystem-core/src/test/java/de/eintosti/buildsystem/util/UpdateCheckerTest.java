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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.util.UpdateChecker.ReleaseRequest;
import de.eintosti.buildsystem.util.UpdateChecker.UpdateReason;
import de.eintosti.buildsystem.util.UpdateChecker.UpdateResult;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@NullMarked
class UpdateCheckerTest {

    private final List<Runnable> submitted = new ArrayList<>();
    private int requests;

    @ParameterizedTest(name = "{0} against {1}: {2}")
    @CsvSource({
        "4.0.0, 4.1.0, NEW_UPDATE",
        "4.0.0, 4.0.0, UP_TO_DATE",
        "4.0.0, v4.0.0, UP_TO_DATE",
        "4.0, 4.0.0, UP_TO_DATE",
        "4.1.0-SNAPSHOT, 4.1.0, NEW_UPDATE",
        "4.1.0, 4.0.0, UNRELEASED_VERSION",
        "4.1.0, 4.1.0-rc1, UNRELEASED_VERSION",
        "4.0.0, 4.0.1-hotfix, NEW_UPDATE",
        "dev, 4.0.0, UNSUPPORTED_VERSION_SCHEME"
    })
    void installedVersion_isComparedWithTheNewestTag(String installed, String tag, UpdateReason expected) {
        UpdateChecker checker = checker(installed, response(200, releases(release(tag, false, false)), Map.of()));

        assertEquals(expected, check(checker).reason());
    }

    @Test
    void newestStableRelease_isPicked_whateverItsPositionInTheList() {
        UpdateChecker checker = checker(
                "4.0.0",
                response(
                        200,
                        releases(
                                release("3.9.9", false, false),
                                release("4.1.0", false, false),
                                release("5.0.0", true, false),
                                release("4.2.0", false, true),
                                release("4.0.5", false, false),
                                release("nightly", false, false)),
                        Map.of()));

        UpdateResult result = check(checker);

        assertEquals(UpdateReason.NEW_UPDATE, result.reason());
        assertEquals(new UpdateChecker.Release("4.1.0", url("4.1.0")), result.newerRelease());
    }

    @Test
    void upToDate_hasNoNewerRelease() {
        UpdateChecker checker = checker("4.0.0", response(200, releases(release("4.0.0", false, false)), Map.of()));

        assertNull(check(checker).newerRelease());
    }

    @Test
    void answer_isReusedForAnHour() {
        UpdateChecker checker = checker("4.0.0", response(200, releases(release("4.0.0", false, false)), Map.of()));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertSame(first, checker.requestUpdateCheck());
        assertEquals(1, requests);
    }

    @Test
    void rateLimit_isNotRetriedBeforeTheReset() {
        String reset = String.valueOf(Instant.now().plusSeconds(3600).getEpochSecond());
        UpdateChecker checker = checker("4.0.0", response(403, "{}", Map.of("x-ratelimit-reset", List.of(reset))));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.RATE_LIMITED, first.join().reason());
        assertSame(first, checker.requestUpdateCheck());
    }

    @Test
    void rateLimit_withAPassedRetryAfter_isRetried() {
        UpdateChecker checker = checker("4.0.0", response(429, "{}", Map.of("retry-after", List.of("0"))));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.RATE_LIMITED, first.join().reason());
        assertNotSame(first, checker.requestUpdateCheck());
    }

    @Test
    void malformedJson_isAFailureThatIsRetried() {
        UpdateChecker checker = checker("4.0.0", response(200, "[{\"tag_name\": ", Map.of()));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.INVALID_JSON, first.join().reason());
        assertNotSame(first, checker.requestUpdateCheck());
    }

    @Test
    void unreachableGitHub_isAFailureThatIsRetried() {
        UpdateChecker checker = new UpdateChecker(
                plugin("4.0.0"),
                () -> {
                    throw new IOException("offline");
                },
                submitted::add);

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.COULD_NOT_CONNECT, first.join().reason());
        assertNotSame(first, checker.requestUpdateCheck());
    }

    private UpdateChecker checker(String installed, HttpResponse<String> response) {
        ReleaseRequest request = () -> {
            requests++;
            return response;
        };
        return new UpdateChecker(plugin(installed), request, submitted::add);
    }

    private static JavaPlugin plugin(String version) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("BuildSystem", version, "Main"));
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

    private static String url(String tag) {
        return "https://github.com/thomasmny/BuildSystem/releases/tag/" + tag;
    }

    private static String release(String tag, boolean draft, boolean prerelease) {
        return "{\"tag_name\": \"%s\", \"html_url\": \"%s\", \"draft\": %b, \"prerelease\": %b}"
                .formatted(tag, url(tag), draft, prerelease);
    }

    private static String releases(String... releases) {
        return Arrays.stream(releases).collect(Collectors.joining(", ", "[", "]"));
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body, Map<String, List<String>> headers) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        return response;
    }
}
