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
package de.eintosti.buildsystem.command.subcommand.worlds;

import com.google.common.collect.Lists;
import de.eintosti.buildsystem.command.PagedCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.util.Permissions;
import java.util.List;
import java.util.logging.Logger;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class HelpSubCommand extends PagedCommand implements SubCommand {

    public HelpSubCommand(Messages messages, Logger logger) {
        super(messages, logger, "worlds_help_title_with_page", "worlds_help_permission");
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        if (!hasPermission(player)) {
            messages.sendPermissionError(player);
            return;
        }

        if (args.length == 1) {
            sendMessage(player, 1);
        } else if (args.length == 2) {
            try {
                int page = Integer.parseInt(args[1]);
                sendMessage(player, page);
            } catch (NumberFormatException e) {
                messages.sendMessage(player, "worlds_help_invalid_page");
            }
        } else {
            messages.sendMessage(player, "worlds_help_usage");
        }
    }

    @Override
    protected List<TextComponent> getCommands(Player player) {
        List<TextComponent> commands = Lists.newArrayList(
                createComponent(player, "/worlds help <page>", "worlds_help_help", "/worlds help", "-"),
                createComponent(player, "/worlds info", "worlds_help_info", "/worlds info", Permissions.INFO),
                createComponent(player, "/worlds item", "worlds_help_item", "/worlds item", Permissions.NAVIGATOR_ITEM),
                createComponent(player, "/worlds tp <world>", "worlds_help_tp", "/worlds tp ", Permissions.WORLDTP),
                createComponent(player, "/worlds edit <world>", "worlds_help_edit", "/worlds edit ", Permissions.EDIT),
                createComponent(
                        player,
                        "/worlds addBuilder <world>",
                        "worlds_help_addbuilder",
                        "/worlds addBuilder ",
                        Permissions.ADDBUILDER),
                createComponent(
                        player,
                        "/worlds removeBuilder <world>",
                        "worlds_help_removebuilder",
                        "/worlds removeBuilder ",
                        Permissions.REMOVEBUILDER),
                createComponent(
                        player,
                        "/worlds builders <world>",
                        "worlds_help_builders",
                        "/worlds builders ",
                        Permissions.BUILDERS),
                createComponent(
                        player, "/worlds rename <world>", "worlds_help_rename", "/worlds rename ", Permissions.RENAME),
                createComponent(
                        player,
                        "/worlds setItem <world>",
                        "worlds_help_setitem",
                        "/worlds setItem ",
                        Permissions.SETITEM),
                createComponent(
                        player,
                        "/worlds setCreator <world>",
                        "worlds_help_setcreator",
                        "/worlds setCreator ",
                        Permissions.SETCREATOR),
                createComponent(
                        player,
                        "/worlds setProject <world>",
                        "worlds_help_setproject",
                        "/worlds setProject ",
                        Permissions.SETPROJECT),
                createComponent(
                        player,
                        "/worlds saveTemplate <world> [template]",
                        "worlds_help_savetemplate",
                        "/worlds saveTemplate ",
                        Permissions.SAVETEMPLATE),
                createComponent(
                        player,
                        "/worlds setPermission <world>",
                        "worlds_help_setpermission",
                        "/worlds setPermission ",
                        Permissions.SETPERMISSION),
                createComponent(
                        player,
                        "/worlds setStatus <world>",
                        "worlds_help_setstatus",
                        "/worlds setStatus ",
                        Permissions.SETSTATUS),
                createComponent(
                        player, "/worlds setSpawn", "worlds_help_setspawn", "/worlds setSpawn", Permissions.SETSPAWN),
                createComponent(
                        player,
                        "/worlds removeSpawn",
                        "worlds_help_removespawn",
                        "/worlds removeSpawn",
                        Permissions.REMOVESPAWN),
                createComponent(
                        player, "/worlds delete <world>", "worlds_help_delete", "/worlds delete ", Permissions.DELETE),
                createComponent(
                        player, "/worlds import <world>", "worlds_help_import", "/worlds import ", Permissions.IMPORT),
                createComponent(
                        player,
                        "/worlds importAll",
                        "worlds_help_importall",
                        "/worlds importAll",
                        Permissions.IMPORT_ALL),
                createComponent(
                        player, "/worlds unimport", "worlds_help_unimport", "/worlds unimport", Permissions.UNIMPORT));
        commands.removeIf(textComponent -> textComponent.getText().isEmpty());
        return commands;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.HELP;
    }
}
