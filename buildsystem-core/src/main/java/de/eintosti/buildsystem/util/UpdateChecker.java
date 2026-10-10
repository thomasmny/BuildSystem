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
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
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
 * A utility class to assist in checking for updates for plugins uploaded to <a
 * href="https://spigotmc.org/resources/">SpigotMC</a>.
 *
 * @author Parker Hawke - Choco
 */
@NullMarked
public final class UpdateChecker {

    private static final String USER_AGENT = "CHOCO-update-checker";
    private static final String UPDATE_URL = "https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=%d";
    private static final Pattern DECIMAL_SCHEME_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)*");
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CACHE_FOR = Duration.ofHours(1);

    private final JavaPlugin plugin;
    private final int pluginID;
    private final HttpClient httpClient;
    private final Executor executor;

    private @Nullable CompletableFuture<UpdateResult> lastCheck;
    private long lastCheckAt;

    public UpdateChecker(JavaPlugin plugin, int pluginID, Executor executor) {
        Preconditions.checkArgument(pluginID > 0, "Plugin ID must be greater than 0");
        this.plugin = plugin;
        this.pluginID = pluginID;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.executor = executor;
    }

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
     * Request an update check to Spigot. This request is asynchronous and may not complete immediately as an HTTP GET
     * request is published to the Spigot API. An answer from Spigot is reused for an hour, so a player joining does not
     * cost a request each time; a failed check is retried on the next call.
     *
     * @return a future update result
     */
    public synchronized CompletableFuture<UpdateResult> requestUpdateCheck() {
        long now = System.nanoTime();
        boolean reusable = lastCheck != null
                && now - lastCheckAt <= CACHE_FOR.toNanos()
                && (!lastCheck.isDone() || lastCheck.join().getReason().isAnswer());
        if (!reusable) {
            lastCheck = CompletableFuture.supplyAsync(this::check, executor);
            lastCheckAt = now;
        }
        return lastCheck;
    }

    private UpdateResult check() {
        int responseCode;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(UPDATE_URL.formatted(pluginID)))
                    .timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseCode = response.statusCode();

            JsonElement json;
            try (JsonReader reader = new JsonReader(new StringReader(response.body()))) {
                json = JsonParser.parseReader(reader);
            }

            if (!json.isJsonObject()) {
                return new UpdateResult(UpdateReason.INVALID_JSON);
            }

            String currentVersion =
                    json.getAsJsonObject().get("current_version").getAsString();
            String pluginVersion = plugin.getDescription().getVersion();
            String latest = compareVersions(pluginVersion, currentVersion);

            if (latest == null) {
                return new UpdateResult(UpdateReason.UNSUPPORTED_VERSION_SCHEME);
            } else if (latest.equals(pluginVersion)) {
                return new UpdateResult(
                        pluginVersion.equals(currentVersion)
                                ? UpdateReason.UP_TO_DATE
                                : UpdateReason.UNRELEASED_VERSION);
            } else if (latest.equals(currentVersion)) {
                return new UpdateResult(UpdateReason.NEW_UPDATE, latest);
            }
        } catch (IOException e) {
            return new UpdateResult(UpdateReason.COULD_NOT_CONNECT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new UpdateResult(UpdateReason.COULD_NOT_CONNECT);
        }

        return new UpdateResult(responseCode == 401 ? UpdateReason.UNAUTHORIZED_QUERY : UpdateReason.UNKNOWN_ERROR);
    }

    /**
     * A constant reason for the result of {@link UpdateResult}.
     */
    public enum UpdateReason {

        /**
         * A new update is available for download on SpigotMC.
         */
        NEW_UPDATE, // The only reason that requires an update

        /**
         * A successful connection to the Spigot API could not be established.
         */
        COULD_NOT_CONNECT,

        /**
         * The JSON retrieved from Spigot was invalid or malformed.
         */
        INVALID_JSON,

        /**
         * A 401 error was returned by the Spigot API.
         */
        UNAUTHORIZED_QUERY,

        /**
         * The version of the plugin installed on the server is greater than the one uploaded to SpigotMC's resources
         * section.
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
         * The plugin is up-to-date with the version released on SpigotMC's resources section.
         */
        UP_TO_DATE;

        /**
         * {@return whether Spigot answered the check, as opposed to the request failing}
         */
        boolean isAnswer() {
            return switch (this) {
                case NEW_UPDATE, UNRELEASED_VERSION, UNSUPPORTED_VERSION_SCHEME, UP_TO_DATE -> true;
                case COULD_NOT_CONNECT, INVALID_JSON, UNAUTHORIZED_QUERY, UNKNOWN_ERROR -> false;
            };
        }
    }

    /**
     * Represents a result for an update query performed by {@link UpdateChecker#requestUpdateCheck()}.
     */
    public final class UpdateResult {

        private final UpdateReason reason;
        private final String newestVersion;

        private UpdateResult(UpdateReason reason, String newestVersion) {
            this.reason = reason;
            this.newestVersion = newestVersion;
        }

        private UpdateResult(UpdateReason reason) {
            Preconditions.checkArgument(
                    reason != UpdateReason.NEW_UPDATE,
                    "Reasons that require updates must also provide the latest version String");

            this.reason = reason;
            this.newestVersion = plugin.getDescription().getVersion();
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
    }
}
