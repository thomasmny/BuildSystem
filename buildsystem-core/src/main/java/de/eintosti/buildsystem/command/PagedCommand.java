/*
 * Copyright (c) 2018-2025, Thomas Meaney
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

import de.eintosti.buildsystem.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class PagedCommand {

    private static final int MAX_COMMANDS_PER_PAGE = 7;

    private final String title, permissionTemplate;

    public PagedCommand(String title, String permissionTemplate) {
        this.title = title;
        this.permissionTemplate = permissionTemplate;
    }

    protected void sendMessage(Player player, int pageNum) {
        List<TextComponent> commands = getCommands(player);
        int numPages = (int) Math.ceil((double) commands.size() / MAX_COMMANDS_PER_PAGE);

        List<TextComponent> page = createPage(commands, numPages, pageNum);
        page.add(0, new TextComponent("§7§m----------------------------------------------------"));
        page.add(1, new TextComponent(Messages.getString(this.title, player)
                .replace("%page%", String.valueOf(pageNum))
                .replace("%max%", String.valueOf(numPages))
                .concat("\n"))
        );
        page.add(new TextComponent("§7§m----------------------------------------------------"));
        page.forEach(line -> player.spigot().sendMessage(line));
    }

    private List<TextComponent> createPage(List<TextComponent> commands, int numPages, int page) {
        List<List<TextComponent>> pages = new ArrayList<>(numPages);
        IntStream.range(0, numPages).forEach(i -> pages.add(new ArrayList<>()));

        int currentPage = 0;
        int commandsInPage = 0;
        for (TextComponent command : commands) {
            pages.get(currentPage).add(command);
            commandsInPage++;

            if (commandsInPage >= MAX_COMMANDS_PER_PAGE) {
                currentPage++;
                commandsInPage = 0;
            }
        }

        return pages.get(page - 1);
    }

    protected abstract List<TextComponent> getCommands(Player player);

    @Contract("_, _, _, _, _-> new")
    protected TextComponent createComponent(Player player, String command, String commandDescriptionKey, String suggest, String permission) {
        if (command.isEmpty()) {
            return new TextComponent();
        }

        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(" §8» " + Messages.getString(commandDescriptionKey, player));

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(Messages.getString(this.permissionTemplate, player, Map.entry("%permission%", permission)))
        ));
        commandComponent.addExtra(textComponent);
        return commandComponent;
    }
}