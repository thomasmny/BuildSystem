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
import de.eintosti.buildsystem.util.ServerModeChecker;
import de.eintosti.buildsystem.util.ServerModeChecker.ServerMode;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
import java.util.logging.Level;

/**
 * Resolves player names to UUIDs and back. Lookups are cached and never block the main thread: the async variants schedule the network call on Bukkit's async pool, while the
 * blocking variants are reserved for code that already runs off the main thread (e.g. world deserialization).
 */
@NullMarked
public final class PlayerLookupService {

    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private final Executor asyncExecutor;

    private final Map<String, UUID> uuidCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> nameCache = new ConcurrentHashMap<>();

    public PlayerLookupService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.asyncExecutor = runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    /**
     * Records a known name/uuid pair so later lookups resolve without a network call.
     *
     * @param uuid The player's uuid
     * @param name The player's name
     */
    public void cacheUser(UUID uuid, String name) {
        uuidCache.put(name.toLowerCase(Locale.ROOT), uuid);
        nameCache.put(uuid, name);
    }

    /**
     * Asynchronously resolves the uuid for the given name. Completes immediately for cached names; otherwise the lookup runs on the async pool.
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
     * Asynchronously resolves the current name for the given uuid.
     *
     * @param uuid The player uuid
     * @return A future completing with the name, or {@code null} if it cannot be resolved
     */
    public CompletableFuture<@Nullable String> lookupName(UUID uuid) {
        String cached = nameCache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> lookupNameBlocking(uuid), asyncExecutor);
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

        JsonObject json = requestJson(UUID_URL.formatted(name));
        if (json == null || !json.has("id")) {
            return null;
        }
        UUID uuid = fromUndashed(json.get("id").getAsString());
        cacheUser(uuid, name);
        return uuid;
    }

    /**
     * Blocking name resolution. Never call on the main thread.
     *
     * @param uuid The player uuid
     * @return The name, or {@code null} if it cannot be resolved
     */
    public @Nullable String lookupNameBlocking(UUID uuid) {
        String cached = nameCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        if (ServerModeChecker.getServerMode() == ServerMode.OFFLINE) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name != null) {
                cacheUser(uuid, name);
            }
            return name;
        }

        JsonObject json = requestJson(PROFILE_URL.formatted(toUndashed(uuid)));
        if (json == null || !json.has("name")) {
            return null;
        }
        String name = json.get("name").getAsString();
        cacheUser(uuid, name);
        return name;
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

    static String toUndashed(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    static UUID fromUndashed(String undashed) {
        return UUID.fromString(undashed.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
