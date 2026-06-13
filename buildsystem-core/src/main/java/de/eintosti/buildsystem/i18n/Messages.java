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
import de.eintosti.buildsystem.util.color.ColorAPI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class Messages {

    private final ConfigService configService;
    private final MessageStore store;
    private volatile @Nullable TextResolver placeholderResolver;

    public Messages(BuildSystemPlugin plugin, ConfigService configService) {
        this.configService = configService;
        this.store = new MessageStore(plugin);
    }

    /**
     * Registers a resolver applied to every message after BuildSystem's own placeholder substitution, or {@code null}
     * to disable external expansion.
     *
     * @param resolver The resolver, or {@code null} to clear
     */
    public void setPlaceholderResolver(@Nullable TextResolver resolver) {
        this.placeholderResolver = resolver;
    }

    public void load() {
        this.store.load();
    }

    public void reload() {
        this.store.reload();
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
        store.checkIfKeyPresent(key);
        String message = store.getRaw(key).replace("%prefix%", store.getPrefix());
        String result = Placeholders.apply(message, placeholders);
        Player player = sender instanceof Player p ? p : null;
        return ColorAPI.process(applyResolver(this.placeholderResolver, player, result));
    }

    /**
     * Applies an external placeholder resolver, when one is registered and the audience is a player. Package-private
     * and pure so the registration/clearing behavior is unit-testable without a live server.
     *
     * @param resolver The resolver, or {@code null}
     * @param player The player the text is for, or {@code null} for non-player audiences
     * @param text The text to expand
     * @return The expanded text, or {@code text} unchanged when no resolver applies
     */
    static String applyResolver(@Nullable TextResolver resolver, @Nullable Player player, String text) {
        if (resolver != null && player != null) {
            return resolver.resolve(player, text);
        }
        return text;
    }

    @SafeVarargs
    public final List<String> getStringList(
            String key, @Nullable Player player, Entry<String, Object>... placeholders) {
        return getStringList(key, player, (line) -> placeholders);
    }

    @Unmodifiable
    public List<String> getStringList(
            String key, @Nullable Player player, Function<String, Entry<String, Object>[]> placeholders) {
        String message = store.getRaw(key).replace("%prefix%", store.getPrefix());
        TextResolver resolver = this.placeholderResolver;
        return Arrays.stream(message.split("\n"))
                .map(line -> Placeholders.apply(line, placeholders.apply(line)))
                .map(line -> applyResolver(resolver, player, line))
                .map(ColorAPI::process)
                .toList();
    }

    public String formatDate(long millis) {
        return millis > 0
                ? new SimpleDateFormat(configService.current().settings().dateFormat()).format(millis)
                : "-";
    }

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

    public static @Nullable String getMessageKey(BuildWorldType type) {
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
