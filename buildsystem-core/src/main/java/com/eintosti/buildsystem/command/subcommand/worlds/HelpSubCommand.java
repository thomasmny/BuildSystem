/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.command.subcommand.worlds;

import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.google.common.collect.Lists;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author einTosti
 */
public class HelpSubCommand extends SubCommand {

    private static final int MAX_COMMANDS_PER_PAGE = 7;

    public HelpSubCommand() {
        super(WorldsTabComplete.WorldsArgument.HELP);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length == 1) {
            sendHelpMessage(player, 1);
        } else if (args.length == 2) {
            try {
                int page = Integer.parseInt(args[1]);
                sendHelpMessage(player, page);
            } catch (NumberFormatException e) {
                Messages.sendMessage(player, "worlds_help_invalid_page");
            }
        } else {
            Messages.sendMessage(player, "worlds_help_usage");
        }
    }

    private void sendHelpMessage(Player player, int pageNum) {
        List<TextComponent> commands = getCommands();
        int numPages = (int) Math.ceil((double) commands.size() / MAX_COMMANDS_PER_PAGE);

        List<TextComponent> page = createPage(commands, numPages, pageNum);
        page.add(0, new TextComponent("§7§m----------------------------------------------------"));
        page.add(1, new TextComponent(Messages.getString("worlds_help_title_with_page")
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

    private List<TextComponent> getCommands() {
        List<TextComponent> commands = Lists.newArrayList(
                createComponent("/worlds help <page>", Messages.getString("worlds_help_help"), "/worlds help", "-"),
                createComponent("/worlds info", Messages.getString("worlds_help_info"), "/worlds info", "buildsystem.info"),
                createComponent("/worlds item", Messages.getString("worlds_help_item"), "/worlds item", "buildsystem.navigator.item"),
                createComponent("/worlds tp <world>", Messages.getString("worlds_help_tp"), "/worlds tp ", "buildsystem.worldtp"),
                createComponent("/worlds edit <world>", Messages.getString("worlds_help_edit"), "/worlds edit ", "buildsystem.edit"),
                createComponent("/worlds addBuilder <world>", Messages.getString("worlds_help_addbuilder"), "/worlds addBuilder ", "buildsystem.addbuilder"),
                createComponent("/worlds removeBuilder <world>", Messages.getString("worlds_help_removebuilder"), "/worlds removeBuilder ", "buildsystem.removebuilder"),
                createComponent("/worlds builders <world>", Messages.getString("worlds_help_builders"), "/worlds builders ", "buildsystem.builders"),
                createComponent("/worlds rename <world>", Messages.getString("worlds_help_rename"), "/worlds rename ", "buildsystem.rename"),
                createComponent("/worlds setItem <world>", Messages.getString("worlds_help_setitem"), "/worlds setItem ", "buildsystem.setitem"),
                createComponent("/worlds setCreator <world>", Messages.getString("worlds_help_setcreator"), "/worlds setCreator ", "buildsystem.setcreator"),
                createComponent("/worlds setProject <world>", Messages.getString("worlds_help_setproject"), "/worlds setProject ", "buildsystem.setproject"),
                createComponent("/worlds setPermission <world>", Messages.getString("worlds_help_setpermission"), "/worlds setPermission ", "buildsystem.setpermission"),
                createComponent("/worlds setStatus <world>", Messages.getString("worlds_help_setstatus"), "/worlds setStatus ", "buildsystem.setstatus"),
                createComponent("/worlds setSpawn", Messages.getString("worlds_help_setspawn"), "/worlds setSpawn", "buildsystem.setspawn"),
                createComponent("/worlds removeSpawn", Messages.getString("worlds_help_removespawn"), "/worlds removeSpawn", "buildsystem.removespawn"),
                createComponent("/worlds delete <world>", Messages.getString("worlds_help_delete"), "/worlds delete ", "buildsystem.delete"),
                createComponent("/worlds import <world>", Messages.getString("worlds_help_import"), "/worlds import ", "buildsystem.import"),
                createComponent("/worlds importAll", Messages.getString("worlds_help_importall"), "/worlds importAll", "buildsystem.import.all"),
                createComponent("/worlds unimport", Messages.getString("worlds_help_unimport"), "/worlds unimport", "buildsystem.unimport")
        );
        commands.removeIf(textComponent -> textComponent.getText().isEmpty());
        return commands;
    }

    private TextComponent createComponent(String command, String text, String suggest, String permission) {
        if (text.isEmpty()) {
            return new TextComponent();
        }

        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(" §8» " + text);

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Messages.getString("worlds_help_permission", new AbstractMap.SimpleEntry<>("%permission%", permission))).create()
        ));
        commandComponent.addExtra(textComponent);
        return commandComponent;
    }
}