/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.util.UUIDTypeAdapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class UUIDFetcher {

    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final String NAME_URL = "https://api.mojang.com/user/profiles/%s/names";

    private static final Map<String, UUID> UUID_CACHE = new HashMap<>();
    private static final Map<UUID, String> NAME_CACHE = new HashMap<>();

    /**
     * Fetches the uuid which belongs to the player with the give name synchronously and returns it.
     *
     * @param name The name of the player whose uuid is to be fetched
     * @return The uuid which belongs to the player
     */
    public static UUID getUUID(String name) {
        String lowerCase = name.toLowerCase();
        if (UUID_CACHE.containsKey(lowerCase)) {
            return UUID_CACHE.get(lowerCase);
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(UUID_URL, name)).openConnection();
            connection.setReadTimeout(5000);

            JsonObject jsonObject;
            try {
                // Support older versions of JSON used by Minecraft versions <1.18
                jsonObject = new JsonParser().parse(new BufferedReader(new InputStreamReader(connection.getInputStream()))).getAsJsonObject();
            } catch (IllegalStateException ignored) {
                return null;
            }

            UUID uuid = UUIDTypeAdapter.fromString(jsonObject.get("id").getAsString());
            UUID_CACHE.put(lowerCase, uuid);
            NAME_CACHE.put(uuid, name);

            return uuid;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Fetches the name which belongs to the player with the give uuid synchronously and returns it.
     *
     * @param uuid The uuid of the player whose name is to be fetched
     * @return The name which belongs to the player
     */
    public static String getName(UUID uuid) {
        if (NAME_CACHE.containsKey(uuid)) {
            return NAME_CACHE.get(uuid);
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(NAME_URL, UUIDTypeAdapter.fromUUID(uuid))).openConnection();
            connection.setReadTimeout(5000);
            JsonArray nameHistory;
            try {
                // Support older versions of JSON used by Minecraft versions <1.18
                nameHistory = new JsonParser().parse(new BufferedReader(new InputStreamReader(connection.getInputStream()))).getAsJsonArray();
            } catch (IllegalStateException ignored) {
                return null;
            }
            JsonObject currentNameData = nameHistory.get(nameHistory.size() - 1).getAsJsonObject();

            String name = currentNameData.get("name").getAsString();
            UUID_CACHE.put(name, uuid);
            NAME_CACHE.put(uuid, name);

            return name;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}