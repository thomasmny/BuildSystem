/*
 * Copyright (c) 2018-2023, Thomas Meaney
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
    protected List<TextComponent> getCommands() {
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

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.HELP;
    }
}