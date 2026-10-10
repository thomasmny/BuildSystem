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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Checks the latest GitHub release of BuildSystem against the installed version.
 *
 * @author Parker Hawke - Choco
 */
@NullMarked
public final class UpdateChecker {

    private static final URI LATEST_RELEASE =
            URI.create("https://api.github.com/repos/thomasmny/BuildSystem/releases/latest");
    private static final String RELEASES_PAGE = "https://github.com/thomasmny/BuildSystem/releases";
    private static final Pattern DECIMAL_SCHEME_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CACHE_FOR = Duration.ofHours(1);

    private final JavaPlugin plugin;
    private final ReleaseRequest request;
    private final Executor executor;
    private final Duration cacheFor;

    private @Nullable CompletableFuture<UpdateResult> lastCheck;
    private long lastCheckAt;

    /**
     * The last release GitHub sent, kept so a {@code 304 Not Modified} can reuse it.
     */
    private volatile @Nullable Release lastRelease;

    public UpdateChecker(JavaPlugin plugin, Executor executor) {
        this(plugin, githubRequest(plugin.getDescription().getVersion()), executor, CACHE_FOR);
    }

    UpdateChecker(JavaPlugin plugin, ReleaseRequest request, Executor executor, Duration cacheFor) {
        this.plugin = plugin;
        this.request = request;
        this.executor = executor;
        this.cacheFor = cacheFor;
    }

    private static ReleaseRequest githubRequest(String pluginVersion) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        return etag -> {
            HttpRequest.Builder builder = HttpRequest.newBuilder(LATEST_RELEASE)
                    .timeout(TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "BuildSystem/" + pluginVersion)
                    .GET();
            if (etag != null) {
                builder.header("If-None-Match", etag);
            }
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        };
    }

    /**
     * Fetches the latest release, sending the given ETag as {@code If-None-Match} when there is one.
     */
    @FunctionalInterface
    interface ReleaseRequest {
        HttpResponse<String> send(@Nullable String etag) throws IOException, InterruptedException;
    }

    private record Release(@Nullable String etag, String version, String url) {}

    /**
     * {@return the plugin's current version} Exposed so callers can render it (e.g. in an update-available message)
     * without depending on the plugin instance.
     */
    public String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * {@return the higher of two dotted version numbers, or {@code null} if either has no number in it}
     */
    private static @Nullable String compareVersions(String first, String second) {
        String[] firstSplit = splitVersionInfo(first), secondSplit = splitVersionInfo(second);
        if (firstSplit == null || secondSplit == null) {
            return null;
        }

        for (int i = 0; i < Math.min(firstSplit.length, secondSplit.length); i++) {
            int currentValue = NumberUtils.toInt(firstSplit[i]), newestValue = NumberUtils.toInt(secondSplit[i]);
            if (newestValue > currentValue) {
                return second;
            } else if (newestValue < currentValue) {
                return first;
            }
        }

        return (secondSplit.length > firstSplit.length) ? second : first;
    }

    private static String @Nullable [] splitVersionInfo(String version) {
        Matcher matcher = DECIMAL_SCHEME_PATTERN.matcher(version);
        return matcher.find() ? matcher.group().split("\\.") : null;
    }

    /**
     * Requests an update check from GitHub. The request runs on the executor given to the constructor. An answer is
     * reused for an hour, so a player joining does not cost a request each time; a failed check is retried on the next
     * call.
     *
     * @return a future update result
     */
    public synchronized CompletableFuture<UpdateResult> requestUpdateCheck() {
        long now = System.nanoTime();
        boolean reusable = lastCheck != null
                && now - lastCheckAt < cacheFor.toNanos()
                && (!lastCheck.isDone() || lastCheck.join().getReason().isAnswer());
        if (!reusable) {
            lastCheck = CompletableFuture.supplyAsync(this::check, executor);
            lastCheckAt = now;
        }
        return lastCheck;
    }

    /**
     * Never throws, so a cached check can always be read.
     */
    private UpdateResult check() {
        Release cached = lastRelease;
        try {
            HttpResponse<String> response = request.send(cached == null ? null : cached.etag());
            int status = response.statusCode();
            // A 304 confirms the cached release and does not count against GitHub's rate limit.
            Release release = status == 200 ? parse(response) : status == 304 ? cached : null;
            if (release == null) {
                return new UpdateResult(
                        switch (status) {
                            case 401 -> UpdateReason.UNAUTHORIZED_QUERY;
                            case 403, 429 -> UpdateReason.RATE_LIMITED;
                            default -> UpdateReason.UNKNOWN_ERROR;
                        });
            }
            lastRelease = release;
            return compare(release);
        } catch (IOException e) {
            return new UpdateResult(UpdateReason.COULD_NOT_CONNECT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new UpdateResult(UpdateReason.COULD_NOT_CONNECT);
        } catch (RuntimeException e) {
            // A body that is not JSON or lacks the expected fields.
            return new UpdateResult(UpdateReason.INVALID_JSON);
        }
    }

    private static Release parse(HttpResponse<String> response) {
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String tag = json.get("tag_name").getAsString();
        return new Release(
                response.headers().firstValue("ETag").orElse(null),
                tag.startsWith("v") ? tag.substring(1) : tag,
                json.get("html_url").getAsString());
    }

    private UpdateResult compare(Release release) {
        String pluginVersion = getCurrentVersion();
        String latest = compareVersions(pluginVersion, release.version());
        if (latest == null) {
            return new UpdateResult(UpdateReason.UNSUPPORTED_VERSION_SCHEME);
        }
        if (latest.equals(pluginVersion)) {
            return new UpdateResult(
                    pluginVersion.equals(release.version())
                            ? UpdateReason.UP_TO_DATE
                            : UpdateReason.UNRELEASED_VERSION);
        }
        return new UpdateResult(UpdateReason.NEW_UPDATE, release.version(), release.url());
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
         * GitHub's rate limit was reached. The check is retried on the next request.
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

    /**
     * Represents a result for an update query performed by {@link UpdateChecker#requestUpdateCheck()}.
     */
    public final class UpdateResult {

        private final UpdateReason reason;
        private final String newestVersion;
        private final String releaseUrl;

        private UpdateResult(UpdateReason reason, String newestVersion, String releaseUrl) {
            this.reason = reason;
            this.newestVersion = newestVersion;
            this.releaseUrl = releaseUrl;
        }

        private UpdateResult(UpdateReason reason) {
            Preconditions.checkArgument(
                    reason != UpdateReason.NEW_UPDATE,
                    "Reasons that require updates must also provide the latest version String");

            this.reason = reason;
            this.newestVersion = plugin.getDescription().getVersion();
            this.releaseUrl = RELEASES_PAGE;
        }

        /**
         * Get the constant reason of this result.
         *
         * @return the reason
         */
        public UpdateReason getReason() {
            return reason;
        }

        /**
         * Check whether this result requires the user to update.
         *
         * @return {@code true} if requires update, {@code false} otherwise
         */
        public boolean requiresUpdate() {
            return reason == UpdateReason.NEW_UPDATE;
        }

        /**
         * Get the latest version of the plugin. This may be the currently installed version, it may not be. This
         * depends entirely on the result of the update.
         *
         * @return the newest version of the plugin
         */
        public String getNewestVersion() {
            return newestVersion;
        }

        /**
         * {@return the GitHub page of the newest release, or of all releases unless an update is available}
         */
        public String getReleaseUrl() {
            return releaseUrl;
        }
    }
}
