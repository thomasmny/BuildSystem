/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command.subcommand.worlds;

import com.google.common.collect.Lists;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.PagedCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpSubCommand extends PagedCommand implements SubCommand {

    public HelpSubCommand() {
        super("worlds_help_permission", "worlds_help_title_with_page");
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length == 1) {
            sendMessage(player, 1);
        } else if (args.length == 2) {
            try {
                int page = Integer.parseInt(args[1]);
                sendMessage(player, page);
            } catch (NumberFormatException e) {
                Messages.sendMessage(player, "worlds_help_invalid_page");
            }
        } else {
            Messages.sendMessage(player, "worlds_help_usage");
        }
    }

    @Override
    protected List<TextComponent> getCommands(Player player) {
        List<TextComponent> commands = Lists.newArrayList(
                createComponent(player, "worlds_help_help", "/worlds help <page>", "/worlds help", "-"),
                createComponent(player, "worlds_help_info", "/worlds info", "/worlds info", "buildsystem.info"),
                createComponent(player, "worlds_help_item", "/worlds item", "/worlds item", "buildsystem.navigator.item"),
                createComponent(player, "worlds_help_tp", "/worlds tp <world>", "/worlds tp ", "buildsystem.worldtp"),
                createComponent(player, "worlds_help_edit", "/worlds edit <world>", "/worlds edit ", "buildsystem.edit"),
                createComponent(player, "worlds_help_addbuilder", "/worlds addBuilder <world>", "/worlds addBuilder ", "buildsystem.addbuilder"),
                createComponent(player, "worlds_help_removebuilder", "/worlds removeBuilder <world>", "/worlds removeBuilder ", "buildsystem.removebuilder"),
                createComponent(player, "worlds_help_builders", "/worlds builders <world>", "/worlds builders ", "buildsystem.builders"),
                createComponent(player, "worlds_help_rename", "/worlds rename <world>", "/worlds rename ", "buildsystem.rename"),
                createComponent(player, "worlds_help_setitem", "/worlds setItem <world>", "/worlds setItem ", "buildsystem.setitem"),
                createComponent(player, "worlds_help_setcreator", "/worlds setCreator <world>", "/worlds setCreator ", "buildsystem.setcreator"),
                createComponent(player, "worlds_help_setproject", "/worlds setProject <world>", "/worlds setProject ", "buildsystem.setproject"),
                createComponent(player, "worlds_help_setpermission", "/worlds setPermission <world>", "/worlds setPermission ", "buildsystem.setpermission"),
                createComponent(player, "worlds_help_setstatus", "/worlds setStatus <world>", "/worlds setStatus ", "buildsystem.setstatus"),
                createComponent(player, "worlds_help_setspawn", "/worlds setSpawn", "/worlds setSpawn", "buildsystem.setspawn"),
                createComponent(player, "worlds_help_removespawn", "/worlds removeSpawn", "/worlds removeSpawn", "buildsystem.removespawn"),
                createComponent(player, "worlds_help_delete", "/worlds delete <world>", "/worlds delete ", "buildsystem.delete"),
                createComponent(player, "worlds_help_import", "/worlds import <world>", "/worlds import ", "buildsystem.import"),
                createComponent(player, "worlds_help_importall", "/worlds importAll", "/worlds importAll", "buildsystem.import.all"),
                createComponent(player, "worlds_help_unimport", "/worlds unimport", "/worlds unimport", "buildsystem.unimport")
        );
        commands.removeIf(textComponent -> textComponent.getText().isEmpty());
        return commands;
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.HELP;
    }
}