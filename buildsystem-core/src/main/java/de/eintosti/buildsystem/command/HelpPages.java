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
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class HelpPages {

    private static final int MAX_COMMANDS_PER_PAGE = 7;

    private final Messages messages;
    private final String title, permissionTemplate;

    public HelpPages(Messages messages, String title, String permissionTemplate) {
        this.messages = messages;
        this.title = title;
        this.permissionTemplate = permissionTemplate;
    }

    /**
     * Sends one page of {@code commands}, clamping {@code pageNum} into range.
     */
    public void send(Player player, int pageNum, List<TextComponent> commands) {
        int numPages = Math.max(1, Math.ceilDiv(commands.size(), MAX_COMMANDS_PER_PAGE));
        pageNum = Math.clamp(pageNum, 1, numPages);

        List<TextComponent> page = createPage(commands, pageNum);
        page.add(0, new TextComponent("§7§m----------------------------------------------------"));
        page.add(
                1,
                new TextComponent(messages.getString(this.title, player)
                        .replace("%page%", String.valueOf(pageNum))
                        .replace("%max%", String.valueOf(numPages))
                        .concat("\n")));
        page.add(new TextComponent("§7§m----------------------------------------------------"));
        page.forEach(line -> player.spigot().sendMessage(line));
    }

    private static List<TextComponent> createPage(List<TextComponent> commands, int page) {
        int from = (page - 1) * MAX_COMMANDS_PER_PAGE;
        return new ArrayList<>(commands.subList(from, Math.min(from + MAX_COMMANDS_PER_PAGE, commands.size())));
    }

    @Contract("_, _, _, _, _-> new")
    public TextComponent component(
            Player player, String command, String commandDescriptionKey, String suggest, String permission) {
        if (command.isEmpty()) {
            return new TextComponent();
        }

        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(" §8» " + messages.getString(commandDescriptionKey, player));

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new Text(messages.getString(
                        this.permissionTemplate, player, Placeholders.of("%permission%", permission)))));
        commandComponent.addExtra(textComponent);
        return commandComponent;
    }
}
