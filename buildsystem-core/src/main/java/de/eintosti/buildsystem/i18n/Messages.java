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
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.util.color.ColorAPI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class Messages {

    /**
     * Used when {@code settings.date-format} is not a pattern the formatter accepts, so a bad config line degrades to a
     * readable date instead of throwing on every render.
     */
    private static final String FALLBACK_DATE_FORMAT = "dd/MM/yyyy";

    private static final String TIME_SUFFIX = " HH:mm:ss";

    private final ConfigService configService;
    private final MessageStore store;
    private final Logger logger;
    private volatile @Nullable TextResolver placeholderResolver;

    /**
     * The formatters built for the currently configured pattern. Cached because the scoreboard renders several
     * timestamps per player per second, and rebuilt when a reload changes the pattern.
     */
    private volatile @Nullable Formatters formatters;

    public Messages(BuildSystemPlugin plugin, ConfigService configService) {
        this.configService = configService;
        this.store = new MessageStore(plugin);
        this.logger = plugin.getLogger();
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

    /**
     * Returns the raw, unrendered value for a key only if the loaded {@code messages.yml} contains it, without logging
     * a missing-key warning. Intended for one-time migrations that read legacy keys (e.g. seeding status names from the
     * pre-4.0 {@code status_*} keys).
     *
     * @param key The message key
     * @return The raw value, or {@link Optional#empty()} when absent
     */
    public Optional<String> findRaw(String key) {
        return store.find(key);
    }

    public void reload() {
        this.store.load();
    }

    public void sendPermissionError(CommandSender sender) {
        sendMessage(sender, "no_permissions");
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Placeholders.none());
    }

    public void sendMessage(CommandSender sender, String key, Placeholders placeholders) {
        String message = getString(key, sender, placeholders);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public String getString(String key, CommandSender sender) {
        return getString(key, sender, Placeholders.none());
    }

    public String getString(String key, CommandSender sender, Placeholders placeholders) {
        store.checkIfKeyPresent(key);
        String message = store.getRaw(key).replace("%prefix%", store.getPrefix());
        String result = placeholders.applyTo(message);
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

    public List<String> getStringList(String key, @Nullable Player player) {
        return getStringList(key, player, Placeholders.none());
    }

    public List<String> getStringList(String key, @Nullable Player player, Placeholders placeholders) {
        return getStringList(key, player, line -> placeholders);
    }

    /**
     * Renders a multi-line message, choosing the substitutions per line. Only the world-item lore needs this, to expand
     * a builder list across as many lines as it takes.
     *
     * @param key The message key
     * @param player The player the text is for, or {@code null} for a non-player audience
     * @param placeholders Supplies the substitutions for a given raw line
     * @return The rendered lines
     */
    @Unmodifiable
    public List<String> getStringList(
            String key, @Nullable Player player, Function<String, Placeholders> placeholders) {
        store.checkIfKeyPresent(key);
        String message = store.getRaw(key).replace("%prefix%", store.getPrefix());
        TextResolver resolver = this.placeholderResolver;
        return Arrays.stream(message.split("\n"))
                .map(line -> placeholders.apply(line).applyTo(line))
                .map(line -> applyResolver(resolver, player, line))
                .map(ColorAPI::process)
                .toList();
    }

    /**
     * Formats a timestamp as a date, using {@code settings.date-format}.
     *
     * @param millis The epoch milliseconds, or a non-positive value for "never"
     * @return The formatted date, or {@code "-"} when there is no timestamp
     */
    public String formatDate(long millis) {
        return format(millis, formatters().date());
    }

    /**
     * Formats a timestamp as a date and time-of-day. Used where the exact moment matters, such as naming a backup.
     *
     * @param millis The epoch milliseconds, or a non-positive value for "never"
     * @return The formatted timestamp, or {@code "-"} when there is no timestamp
     */
    public String formatDateTime(long millis) {
        return format(millis, formatters().dateTime());
    }

    private static String format(long millis, DateTimeFormatter formatter) {
        return millis > 0 ? formatter.format(Instant.ofEpochMilli(millis)) : "-";
    }

    private Formatters formatters() {
        String pattern = configService.current().settings().dateFormat();
        Formatters current = this.formatters;
        if (current != null && current.pattern().equals(pattern)) {
            return current;
        }

        Formatters rebuilt = buildFormatters(pattern);
        this.formatters = rebuilt;
        return rebuilt;
    }

    private Formatters buildFormatters(String pattern) {
        try {
            return new Formatters(pattern, formatter(pattern), formatter(pattern + TIME_SUFFIX));
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid settings.date-format \"" + pattern + "\", falling back to " + FALLBACK_DATE_FORMAT
                    + ": " + e.getMessage());
            return new Formatters(
                    pattern, formatter(FALLBACK_DATE_FORMAT), formatter(FALLBACK_DATE_FORMAT + TIME_SUFFIX));
        }
    }

    private static DateTimeFormatter formatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
    }

    /**
     * The date and date-time formatters derived from one configured pattern. {@link DateTimeFormatter} is immutable and
     * thread-safe, so these can be shared across the async scoreboard threads that render them.
     */
    private record Formatters(String pattern, DateTimeFormatter date, DateTimeFormatter dateTime) {}

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
            case UNKNOWN -> "type_unknown";
        };
    }
}
