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
import de.eintosti.buildsystem.api.world.data.BuildWorldStatus;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.util.Placeholders;
import de.eintosti.buildsystem.util.color.ColorAPI;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class Messages {

    private final BuildSystemPlugin plugin;
    private final ConfigService configService;
    private volatile Map<String, String> messages = Map.of();

    public Messages(BuildSystemPlugin plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
    }

    public void load() {
        // 1. If user file doesn't exist, save the bundled default
        File file = new java.io.File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // 2. Load user file and bundled defaults
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        java.io.InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
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
                } catch (java.io.IOException e) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save messages.yml", e);
                }
            }
        }

        // 4. Build messages map
        Map<String, String> map = new HashMap<>();
        ConfigurationSection section = userConfig.getConfigurationSection("");
        if (section != null) {
            section.getKeys(false).forEach(key -> {
                if (!userConfig.contains(key)) {
                    Bukkit.getConsoleSender().sendMessage(
                            ChatColor.RED + "[BuildSystem] Could not find message with key: " + key);
                    return;
                }
                if (userConfig.isList(key)) {
                    map.put(key, String.join("\n", userConfig.getStringList(key)));
                } else {
                    map.put(key, Objects.requireNonNull(userConfig.getString(key),
                            "Message key '%s' is null".formatted(key)));
                }
            });
        }
        this.messages = Map.copyOf(map);
    }

    public void reload() {
        load();
    }

    private String getPrefix() {
        return messages.getOrDefault("prefix", "");
    }

    private void checkIfKeyPresent(String key) {
        if (!messages.containsKey(key)) {
            plugin.getLogger().warning("Could not find message with key: " + key);
            load();
        }
    }

    public void sendPermissionError(CommandSender sender) {
        sendMessage(sender, "no_permissions");
    }

    @SafeVarargs
    public final void sendMessage(CommandSender sender, String key, Entry<String, Object>... placeholders) {
        String message = getString(key, sender, placeholders);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    @SafeVarargs
    public final String getString(String key, CommandSender sender, Entry<String, Object>... placeholders) {
        checkIfKeyPresent(key);
        String message = messages.getOrDefault(key, "").replace("%prefix%", getPrefix());
        String result = Placeholders.apply(message, placeholders);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && sender instanceof Player player) {
            result = PlaceholderAPI.setPlaceholders(player, result);
        }
        return ColorAPI.process(result);
    }

    @SafeVarargs
    public final List<String> getStringList(String key, @Nullable Player player, Entry<String, Object>... placeholders) {
        return getStringList(key, player, (line) -> placeholders);
    }

    @Unmodifiable
    public final List<String> getStringList(String key, @Nullable Player player, Function<String, Entry<String, Object>[]> placeholders) {
        String message = messages.getOrDefault(key, "").replace("%prefix%", getPrefix());
        boolean papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        return Arrays.stream(message.split("\n"))
                .map(line -> Placeholders.apply(line, placeholders.apply(line)))
                .map(line -> papiEnabled && player != null
                        ? PlaceholderAPI.setPlaceholders(player, line)
                        : line)
                .map(ColorAPI::process)
                .toList();
    }

    public String formatDate(long millis) {
        return millis > 0
                ? new SimpleDateFormat(configService.current().messages().dateFormat()).format(millis)
                : "-";
    }

    @Nullable
    public static String getMessageKey(BuildWorldStatus status) {
        return switch (status) {
            case NOT_STARTED -> "status_not_started";
            case IN_PROGRESS -> "status_in_progress";
            case ALMOST_FINISHED -> "status_almost_finished";
            case FINISHED -> "status_finished";
            case ARCHIVE -> "status_archive";
            case HIDDEN -> "status_hidden";
        };
    }

    @Nullable
    public static String getMessageKey(BuildWorldType type) {
        return switch (type) {
            case NORMAL -> "type_normal";
            case FLAT -> "type_flat";
            case NETHER -> "type_nether";
            case END -> "type_end";
            case VOID -> "type_void";
            case TEMPLATE -> "type_template";
            case PRIVATE -> "type_private";
            case IMPORTED -> "type_imported";
            case CUSTOM -> "type_custom";
            case UNKNOWN -> null;
        };
    }
}
