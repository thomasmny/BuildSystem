/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.Messages;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public abstract class PagedCommand {

    private static final int MAX_COMMANDS_PER_PAGE = 7;

    private final String permission, title;

    public PagedCommand(String permission, String title) {
        this.permission = permission;
        this.title = title;
    }

    protected void sendMessage(Player player, int pageNum) {
        List<TextComponent> commands = getCommands();
        int numPages = (int) Math.ceil((double) commands.size() / MAX_COMMANDS_PER_PAGE);

        List<TextComponent> page = createPage(commands, numPages, pageNum);
        page.add(0, new TextComponent("§7§m----------------------------------------------------"));
        page.add(1, new TextComponent(Messages.getString(this.title)
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

    protected abstract List<TextComponent> getCommands();

    protected TextComponent createComponent(String command, String text, String suggest, String permission) {
        if (text.isEmpty()) {
            return new TextComponent();
        }

        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(" §8» " + text);

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Messages.getString(this.permission, new AbstractMap.SimpleEntry<>("%permission%", permission))).create()
        ));
        commandComponent.addExtra(textComponent);
        return commandComponent;
    }
}