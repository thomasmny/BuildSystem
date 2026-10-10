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
package de.eintosti.buildsystem.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.util.ServerModeChecker;
import de.eintosti.buildsystem.util.ServerModeChecker.ServerMode;
import de.eintosti.buildsystem.util.TaskScheduler;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Resolves player names to UUIDs. Lookups are cached and never block the main thread: the async variants
 * schedule the network call on Bukkit's async pool, while the blocking variants are reserved for code that already runs
 * off the main thread (e.g. world deserialization).
 */
@NullMarked
public final class PlayerLookupService {

    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    /**
     * The names a Mojang account can have. Anything else cannot be looked up and would not even form a valid URL.
     */
    private static final Pattern MOJANG_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private final Executor asyncExecutor;
    private final Executor mainThread;

    private final Map<String, UUID> uuidCache = new ConcurrentHashMap<>();

    /**
     * @param plugin The plugin, used for logging
     * @param asyncExecutor Runs the Mojang lookups off the main thread; in production this is
     *     {@link TaskScheduler#background()}
     * @param mainThread Runs the {@link #resolve} callbacks; in production this is {@link TaskScheduler#mainThread()}
     */
    public PlayerLookupService(JavaPlugin plugin, Executor asyncExecutor, Executor mainThread) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.asyncExecutor = asyncExecutor;
        this.mainThread = mainThread;
    }

    /**
     * Records a known name/uuid pair so later lookups resolve without a network call.
     *
     * @param uuid The player's uuid
     * @param name The player's name
     */
    public void cacheUser(UUID uuid, String name) {
        uuidCache.put(name.toLowerCase(Locale.ROOT), uuid);
    }

    /**
     * Asynchronously resolves the uuid for the given name. Completes immediately for cached names; otherwise the lookup
     * runs on the async pool.
     *
     * @param name The player name
     * @return A future completing with the uuid, or {@code null} if the name has no account
     */
    public CompletableFuture<@Nullable UUID> lookupUniqueId(String name) {
        UUID cached = uuidCache.get(name.toLowerCase(Locale.ROOT));
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> lookupUniqueIdBlocking(name), asyncExecutor);
    }

    /**
     * Resolves a player name to a {@link Builder}, using the online player when there is one and otherwise looking the
     * name up off the main thread. Call it on the main thread: an online player reaches {@code onFound} straight away,
     * and a looked-up answer is handed back to the main thread.
     *
     * @param name The player name
     * @param onFound Receives the builder
     * @param onNotFound Runs when no account has that name
     */
    public void resolve(String name, Consumer<Builder> onFound, Runnable onNotFound) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            onFound.accept(Builder.of(online));
            return;
        }

        lookupUniqueId(name)
                .thenAcceptAsync(
                        uuid -> {
                            if (uuid == null) {
                                onNotFound.run();
                            } else {
                                onFound.accept(Builder.of(uuid, name));
                            }
                        },
                        mainThread);
    }

    /**
     * Blocking uuid resolution. Never call on the main thread.
     *
     * @param name The player name
     * @return The uuid, or {@code null} if the name has no account
     */
    public @Nullable UUID lookupUniqueIdBlocking(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        UUID cached = uuidCache.get(key);
        if (cached != null) {
            return cached;
        }

        if (ServerModeChecker.getServerMode() == ServerMode.OFFLINE) {
            UUID uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
            cacheUser(uuid, name);
            return uuid;
        }

        if (!MOJANG_NAME.matcher(name).matches()) {
            return null;
        }

        JsonObject json = requestJson(UUID_URL.formatted(name));
        if (json == null || !json.has("id")) {
            return null;
        }
        UUID uuid = fromUndashed(json.get("id").getAsString());
        cacheUser(uuid, json.has("name") ? json.get("name").getAsString() : name);
        return uuid;
    }

    private @Nullable JsonObject requestJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().isEmpty()) {
                return null;
            }
            JsonElement element = JsonParser.parseString(response.body());
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed Mojang lookup: " + url, e);
            return null;
        }
    }

    static UUID fromUndashed(String undashed) {
        return UUID.fromString(undashed.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
