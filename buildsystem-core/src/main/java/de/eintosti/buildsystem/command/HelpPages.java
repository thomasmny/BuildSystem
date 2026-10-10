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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import java.util.List;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * A paged list of commands, each shown with its description and suggested on click. The message keys share a prefix:
 * {@code <prefix>_title_with_page}, {@code <prefix>_permission} and {@code <prefix>_invalid_page}.
 */
@NullMarked
public final class HelpPages {

    private static final int MAX_COMMANDS_PER_PAGE = 7;
    private static final String RULE = "§7§m----------------------------------------------------";

    /**
     * One line of the help: what to show, the description's message key, what a click suggests, and the permission
     * named in the hover text.
     */
    public record Entry(String usage, String descriptionKey, String suggest, String permission) {}

    private final Messages messages;
    private final String keyPrefix;
    private final List<Entry> entries;

    public HelpPages(Messages messages, String keyPrefix, List<Entry> entries) {
        this.messages = messages;
        this.keyPrefix = keyPrefix;
        this.entries = entries;
    }

    /**
     * Sends the page the player typed, or {@code <prefix>_invalid_page} if it is not a number.
     */
    public void send(Player player, String page) {
        try {
            send(player, Integer.parseInt(page));
        } catch (NumberFormatException e) {
            messages.sendMessage(player, keyPrefix + "_invalid_page");
        }
    }

    /**
     * Sends one page, clamping {@code page} into range.
     */
    public void send(Player player, int page) {
        int numPages = Math.max(1, Math.ceilDiv(entries.size(), MAX_COMMANDS_PER_PAGE));
        int shown = Math.clamp(page, 1, numPages);
        int from = (shown - 1) * MAX_COMMANDS_PER_PAGE;

        player.spigot().sendMessage(new TextComponent(RULE));
        player.spigot()
                .sendMessage(new TextComponent(messages.getString(keyPrefix + "_title_with_page", player)
                        .replace("%page%", String.valueOf(shown))
                        .replace("%max%", String.valueOf(numPages))
                        .concat("\n")));
        entries.subList(from, Math.min(from + MAX_COMMANDS_PER_PAGE, entries.size()))
                .forEach(entry -> player.spigot().sendMessage(component(player, entry)));
        player.spigot().sendMessage(new TextComponent(RULE));
    }

    private TextComponent component(Player player, Entry entry) {
        TextComponent component = new TextComponent("§b" + entry.usage());
        component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, entry.suggest()));
        component.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new Text(messages.getString(
                        keyPrefix + "_permission", player, Placeholders.of("%permission%", entry.permission())))));
        component.addExtra(new TextComponent(" §8» " + messages.getString(entry.descriptionKey(), player)));
        return component;
    }
}
