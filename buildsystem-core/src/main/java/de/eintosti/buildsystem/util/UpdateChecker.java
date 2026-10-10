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

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Checks the newest stable GitHub release of BuildSystem against the installed version.
 *
 * @author Parker Hawke - Choco
 */
@NullMarked
public final class UpdateChecker {

    /**
     * The release list by repository id, which keeps working if the repository is renamed or transferred. The
     * {@code /latest} endpoint is not used because it returns whichever release was marked latest, which may be a patch
     * for an older major version.
     */
    private static final URI RELEASES =
            URI.create("https://api.github.com/repositories/303399172/releases?per_page=20");

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CACHE_FOR = Duration.ofHours(1);

    private final JavaPlugin plugin;
    private final ReleaseRequest request;
    private final Executor executor;

    private @Nullable CompletableFuture<UpdateResult> lastCheck;
    private long lastCheckAt;

    /**
     * When GitHub allows the next request after a rate-limited one, or {@code null} if it did not say.
     */
    private volatile @Nullable Instant retryAt;

    public UpdateChecker(JavaPlugin plugin, Executor executor) {
        this(plugin, githubRequest(plugin.getDescription().getVersion()), executor);
    }

    UpdateChecker(JavaPlugin plugin, ReleaseRequest request, Executor executor) {
        this.plugin = plugin;
        this.request = request;
        this.executor = executor;
    }

    private static ReleaseRequest githubRequest(String pluginVersion) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(RELEASES)
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "BuildSystem/" + pluginVersion)
                .GET()
                .build();
        return () -> httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Fetches the release list.
     */
    @FunctionalInterface
    interface ReleaseRequest {
        HttpResponse<String> send() throws IOException, InterruptedException;
    }

    /**
     * {@return the plugin's current version} Exposed so callers can render it (e.g. in an update-available message)
     * without depending on the plugin instance.
     */
    public String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Requests an update check from GitHub on the executor given to the constructor. An answer is reused for an hour,
     * so a player joining does not cost a request each time. A failed check is retried on the next call, or once the
     * time GitHub gave for a rate limit has passed.
     *
     * @return a future update result
     */
    public synchronized CompletableFuture<UpdateResult> requestUpdateCheck() {
        long now = System.nanoTime();
        if (!isReusable(now)) {
            lastCheck = CompletableFuture.supplyAsync(this::check, executor);
            lastCheckAt = now;
        }
        return lastCheck;
    }

    private boolean isReusable(long now) {
        if (lastCheck == null) {
            return false;
        }
        boolean fresh = now - lastCheckAt < CACHE_FOR.toNanos();
        // A check still running is only waited for inside the cache window, so one that hangs cannot block the next.
        if (!lastCheck.isDone() || lastCheck.join().reason().isAnswer()) {
            return fresh;
        }
        Instant until = retryAt;
        return until != null && Instant.now().isBefore(until);
    }

    /**
     * Never throws, so a cached check can always be read.
     */
    private UpdateResult check() {
        HttpResponse<String> response;
        try {
            response = request.send();
        } catch (IOException e) {
            return new UpdateResult(UpdateReason.COULD_NOT_CONNECT, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new UpdateResult(UpdateReason.COULD_NOT_CONNECT, null);
        }

        int status = response.statusCode();
        retryAt = status == 403 || status == 429 ? retryAt(response.headers(), Instant.now()) : null;
        if (status != 200) {
            return new UpdateResult(
                    switch (status) {
                        case 401 -> UpdateReason.UNAUTHORIZED_QUERY;
                        case 403, 429 -> UpdateReason.RATE_LIMITED;
                        default -> UpdateReason.UNKNOWN_ERROR;
                    },
                    null);
        }

        List<Candidate> releases;
        try {
            releases = parse(response.body());
        } catch (RuntimeException e) {
            return new UpdateResult(UpdateReason.INVALID_JSON, null);
        }

        try {
            return compare(releases);
        } catch (RuntimeException e) {
            return new UpdateResult(UpdateReason.UNKNOWN_ERROR, null);
        }
    }

    /**
     * {@return when GitHub allows the next request, at most an hour away} {@code Retry-After} counts seconds from now,
     * and {@code x-ratelimit-reset} is a Unix time, so a skewed clock could otherwise pause checks for long.
     */
    private static @Nullable Instant retryAt(HttpHeaders headers, Instant now) {
        try {
            OptionalLong retryAfter = headers.firstValueAsLong("retry-after");
            OptionalLong reset = headers.firstValueAsLong("x-ratelimit-reset");
            Instant at = retryAfter.isPresent()
                    ? now.plusSeconds(retryAfter.getAsLong())
                    : reset.isPresent() ? Instant.ofEpochSecond(reset.getAsLong()) : null;
            Instant latest = now.plus(CACHE_FOR);
            return at == null || at.isBefore(latest) ? at : latest;
        } catch (NumberFormatException e) {
            // Retry-After may also be an HTTP date; the next request then simply tries again.
            return null;
        }
    }

    /**
     * {@return the stable releases in the list} Drafts, pre-releases and tags that are not version numbers are left
     * out, and a leading {@code v} is stripped from the tag.
     */
    private static List<Candidate> parse(String body) {
        List<Candidate> releases = new ArrayList<>();
        for (JsonElement element : JsonParser.parseString(body).getAsJsonArray()) {
            JsonObject release = element.getAsJsonObject();
            if (release.get("draft").getAsBoolean() || release.get("prerelease").getAsBoolean()) {
                continue;
            }
            String tag = release.get("tag_name").getAsString();
            String version = tag.startsWith("v") ? tag.substring(1) : tag;
            Version parsed = Version.parse(version);
            if (parsed != null) {
                releases.add(new Candidate(
                        parsed, new Release(version, release.get("html_url").getAsString())));
            }
        }
        return releases;
    }

    private UpdateResult compare(List<Candidate> releases) {
        Version installed = Version.parse(getCurrentVersion());
        if (installed == null) {
            return new UpdateResult(UpdateReason.UNSUPPORTED_VERSION_SCHEME, null);
        }

        Candidate newest =
                releases.stream().max(Comparator.comparing(Candidate::version)).orElse(null);
        if (newest == null) {
            return new UpdateResult(UpdateReason.UP_TO_DATE, null);
        }

        int comparison = newest.version().compareTo(installed);
        UpdateReason reason = comparison > 0
                ? UpdateReason.NEW_UPDATE
                : comparison == 0 ? UpdateReason.UP_TO_DATE : UpdateReason.UNRELEASED_VERSION;
        return new UpdateResult(reason, newest.release());
    }

    private record Candidate(Version version, Release release) {}

    /**
     * A dotted version number. Missing parts count as zero, and a version with a suffix such as {@code -SNAPSHOT} is
     * older than the same numbers without one.
     */
    private record Version(List<Integer> numbers, boolean suffixed) implements Comparable<Version> {

        private static final Pattern NUMBERS = Pattern.compile("\\d+(?:\\.\\d+)*");

        static @Nullable Version parse(String version) {
            Matcher matcher = NUMBERS.matcher(version);
            if (!matcher.lookingAt()) {
                return null;
            }
            List<Integer> numbers = Arrays.stream(matcher.group().split("\\."))
                    .map(NumberUtils::toInt)
                    .toList();
            return new Version(numbers, matcher.end() < version.length());
        }

        @Override
        public int compareTo(Version other) {
            for (int i = 0; i < Math.max(numbers.size(), other.numbers.size()); i++) {
                int comparison = Integer.compare(part(i), other.part(i));
                if (comparison != 0) {
                    return comparison;
                }
            }
            return Boolean.compare(other.suffixed, suffixed);
        }

        private int part(int index) {
            return index < numbers.size() ? numbers.get(index) : 0;
        }
    }

    /**
     * A stable GitHub release.
     *
     * @param version The tag without a leading {@code v}
     * @param url The release page
     */
    public record Release(String version, String url) {}

    /**
     * The outcome of an update check.
     *
     * @param reason Why the check ended the way it did
     * @param release The newest stable release, or {@code null} if the check failed or there is none
     */
    public record UpdateResult(
            UpdateReason reason, @Nullable Release release) {

        public UpdateResult {
            Preconditions.checkArgument(
                    reason != UpdateReason.NEW_UPDATE || release != null, "A new update needs a release");
        }

        /**
         * {@return the release to update to, or {@code null} unless it is newer than the installed version}
         */
        public @Nullable Release newerRelease() {
            return reason == UpdateReason.NEW_UPDATE ? release : null;
        }
    }

    /**
     * A constant reason for the result of {@link UpdateResult}.
     */
    public enum UpdateReason {

        /**
         * A newer release is available on GitHub.
         */
        NEW_UPDATE, // The only reason that requires an update

        /**
         * A connection to GitHub could not be established.
         */
        COULD_NOT_CONNECT,

        /**
         * The JSON retrieved from GitHub was invalid or malformed.
         */
        INVALID_JSON,

        /**
         * A 401 error was returned by GitHub.
         */
        UNAUTHORIZED_QUERY,

        /**
         * GitHub's rate limit was reached. The check is retried once GitHub allows it.
         */
        RATE_LIMITED,

        /**
         * The version of the plugin installed on the server is greater than the latest release.
         */
        UNRELEASED_VERSION,

        /**
         * An unknown error occurred.
         */
        UNKNOWN_ERROR,

        /**
         * The plugin uses an unsupported version scheme, therefore a proper comparison between versions could not be
         * made.
         */
        UNSUPPORTED_VERSION_SCHEME,

        /**
         * The plugin is up-to-date with the latest release.
         */
        UP_TO_DATE;

        /**
         * {@return whether GitHub answered the check, as opposed to the request failing}
         */
        boolean isAnswer() {
            return switch (this) {
                case NEW_UPDATE, UNRELEASED_VERSION, UNSUPPORTED_VERSION_SCHEME, UP_TO_DATE -> true;
                case COULD_NOT_CONNECT, INVALID_JSON, UNAUTHORIZED_QUERY, RATE_LIMITED, UNKNOWN_ERROR -> false;
            };
        }
    }
}
