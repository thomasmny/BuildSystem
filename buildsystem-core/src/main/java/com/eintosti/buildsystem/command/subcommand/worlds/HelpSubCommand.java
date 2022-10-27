/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command.subcommand.worlds;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.eintosti.buildsystem.util.Messages;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.AbstractMap;

/**
 * @author einTosti
 */
public class HelpSubCommand extends SubCommand {

    private final BuildSystem plugin;

    public HelpSubCommand(BuildSystem plugin) {
        super(WorldsTabComplete.WorldsArgument.HELP);

        this.plugin = plugin;
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

    private void sendHelpMessage(Player player, int page) {
        final int maxPages = 2;
        if (page > maxPages) {
            page = maxPages;
        }

        TextComponent line1 = new TextComponent("§7§m----------------------------------------------------\n");
        TextComponent line2 = new TextComponent(Messages.getString("worlds_help_title_with_page")
                .replace("%page%", String.valueOf(page))
                .replace("%max%", String.valueOf(maxPages))
                .concat("\n"));
        TextComponent line3 = new TextComponent("§7 \n");

        TextComponent line4;
        TextComponent line5;
        TextComponent line6;
        TextComponent line7;
        TextComponent line8;
        TextComponent line9;
        TextComponent line10;
        TextComponent line11;
        TextComponent line12;
        TextComponent line13;

        if (page == 1) {
            line4 = createComponent("/worlds help <page>", " §8» " + Messages.getString("worlds_help_help"), "/worlds help", "-");
            line5 = createComponent("/worlds info", " §8» " + Messages.getString("worlds_help_info"), "/worlds info", "buildsystem.info");
            line6 = createComponent("/worlds item", " §8» " + Messages.getString("worlds_help_item"), "/worlds item", "buildsystem.navigator.item");
            line7 = createComponent("/worlds tp <world>", " §8» " + Messages.getString("worlds_help_tp"), "/worlds tp ", "buildsystem.worldtp");
            line8 = createComponent("/worlds edit <world>", " §8» " + Messages.getString("worlds_help_edit"), "/worlds edit ", "buildsystem.edit");
            line9 = createComponent("/worlds addBuilder <world>", " §8» " + Messages.getString("worlds_help_addbuilder"), "/worlds addBuilder ", "buildsystem.addbuilder");
            line10 = createComponent("/worlds removeBuilder <world>", " §8» " + Messages.getString("worlds_help_removebuilder"), "/worlds removeBuilder ", "buildsystem.removebuilder");
            line11 = createComponent("/worlds builders <world>", " §8» " + Messages.getString("worlds_help_builders"), "/worlds builders ", "buildsystem.builders");
            line12 = createComponent("/worlds rename <world>", " §8» " + Messages.getString("worlds_help_rename"), "/worlds rename ", "buildsystem.rename");
            line13 = createComponent("/worlds setItem <world>", " §8» " + Messages.getString("worlds_help_setitem"), "/worlds setItem ", "buildsystem.setitem");
        } else {
            line4 = createComponent("/worlds setCreator <world>", " §8» " + Messages.getString("worlds_help_setcreator"), "/worlds setCreator ", "buildsystem.setcreator");
            line5 = createComponent("/worlds setProject <world>", " §8» " + Messages.getString("worlds_help_setproject"), "/worlds setProject ", "buildsystem.setproject");
            line6 = createComponent("/worlds setPermission <world>", " §8» " + Messages.getString("worlds_help_setpermission"), "/worlds setPermission ", "buildsystem.setpermission");
            line7 = createComponent("/worlds setStatus <world>", " §8» " + Messages.getString("worlds_help_setstatus"), "/worlds setStatus ", "buildsystem.setstatus");
            line8 = createComponent("/worlds setSpawn", " §8» " + Messages.getString("worlds_help_setspawn"), "/worlds setSpawn", "buildsystem.setspawn");
            line9 = createComponent("/worlds removeSpawn", " §8» " + Messages.getString("worlds_help_removespawn"), "/worlds removeSpawn", "buildsystem.removespawn");
            line10 = createComponent("/worlds delete <world>", " §8» " + Messages.getString("worlds_help_delete"), "/worlds delete ", "buildsystem.delete");
            line11 = createComponent("/worlds import <world>", " §8» " + Messages.getString("worlds_help_import"), "/worlds import ", "buildsystem.import");
            line12 = createComponent("/worlds importAll", " §8» " + Messages.getString("worlds_help_importall"), "/worlds importAll", "buildsystem.import.all");
            line13 = createComponent("/worlds unimport", " §8» " + Messages.getString("worlds_help_unimport"), "/worlds unimport", "buildsystem.unimport");
        }

        TextComponent line14 = new TextComponent("§7§m----------------------------------------------------");

        player.spigot().sendMessage(line1, line2, line3, line4, line5, line6, line7, line8, line9, line10, line11, line12, line13, line14);
    }

    private TextComponent createComponent(String command, String text, String suggest, String permission) {
        TextComponent commandComponent = new TextComponent("§b" + command);
        TextComponent textComponent = new TextComponent(text + "\n");

        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Messages.getString("worlds_help_permission", new AbstractMap.SimpleEntry<>("%permission%", permission))).create()
        ));
        commandComponent.addExtra(textComponent);
        return commandComponent;
    }
}