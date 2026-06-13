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
package de.eintosti.buildsystem.i18n;

import de.eintosti.buildsystem.BuildSystemPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Loads, merges and looks up messages from {@code messages.yml}. Owns the raw message map and the bundled-default
 * merge/copy-back; rendering and sending are handled by {@link Messages}.
 */
@NullMarked
final class MessageStore {

    private final BuildSystemPlugin plugin;
    private volatile Map<String, String> messages = Map.of();

    MessageStore(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        // 1. If user file doesn't exist, save the bundled default
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // 2. Load user file and bundled defaults
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults =
                    YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            // 3. Copy missing keys from bundled defaults to user file
            boolean changed = false;
            for (String key : defaults.getKeys(false)) {
                if (!userConfig.contains(key)) {
                    userConfig.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                try {
                    userConfig.save(file);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
                }
            }
        }

        // 4. Build messages map
        Map<String, String> map = new HashMap<>();
        ConfigurationSection section = userConfig.getConfigurationSection("");
        if (section != null) {
            section.getKeys(false).forEach(key -> {
                if (!userConfig.contains(key)) {
                    Bukkit.getConsoleSender()
                            .sendMessage(ChatColor.RED + "[BuildSystem] Could not find message with key: " + key);
                    return;
                }
                if (userConfig.isList(key)) {
                    map.put(key, String.join("\n", userConfig.getStringList(key)));
                } else {
                    map.put(
                            key,
                            Objects.requireNonNull(
                                    userConfig.getString(key), "Message key '%s' is null".formatted(key)));
                }
            });
        }
        this.messages = Map.copyOf(map);
    }

    void reload() {
        load();
    }

    String getPrefix() {
        return messages.getOrDefault("prefix", "");
    }

    private void checkIfKeyPresent(String key) {
        if (!messages.containsKey(key)) {
            plugin.getLogger().warning("Could not find message with key: " + key);
        }
    }

    /**
     * Returns the raw message for the given key with the {@code %prefix%} token substituted, warning when the key is
     * missing. The returned text has not yet had placeholders, the external resolver, or colors applied.
     *
     * @param key The message key
     * @return The raw, prefix-substituted message, or an empty string when the key is missing
     */
    String getRaw(String key) {
        checkIfKeyPresent(key);
        return messages.getOrDefault(key, "").replace("%prefix%", getPrefix());
    }

    /**
     * Returns the raw message for the given key with the {@code %prefix%} token substituted, without emitting a
     * missing-key warning. The returned text has not yet had placeholders, the external resolver, or colors applied.
     *
     * @param key The message key
     * @return The raw, prefix-substituted message, or an empty string when the key is missing
     */
    String getRawUnchecked(String key) {
        return messages.getOrDefault(key, "").replace("%prefix%", getPrefix());
    }
}
