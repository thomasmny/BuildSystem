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
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * Loads {@code messages.yml} from disk, merging in any keys missing from the bundled defaults, and provides raw
 * (unrendered) lookups of the resulting message strings.
 */
@NullMarked
final class MessageStore {

    private final BuildSystemPlugin plugin;
    private volatile Map<String, String> messages = Map.of();

    MessageStore(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults =
                    YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));

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

    void checkIfKeyPresent(String key) {
        if (!messages.containsKey(key)) {
            plugin.getLogger().warning("Could not find message with key: " + key);
        }
    }

    /**
     * Returns the raw, unrendered value stored for the given key, or an empty string when the key is absent.
     *
     * @param key The message key
     * @return The raw stored message
     */
    String getRaw(String key) {
        return messages.getOrDefault(key, "");
    }

    /**
     * Returns the raw stored value for the given key only if the loaded {@code messages.yml} actually contains it,
     * without logging a "missing key" warning. Used to read legacy keys during a one-time migration.
     *
     * @param key The message key
     * @return The raw stored value, or {@link Optional#empty()} when the key is absent
     */
    Optional<String> find(String key) {
        return Optional.ofNullable(messages.get(key));
    }
}
