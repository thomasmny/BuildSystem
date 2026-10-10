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

import de.eintosti.buildsystem.command.HelpPages;
import de.eintosti.buildsystem.command.HelpPages.Entry;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.util.Permissions;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class HelpSubCommand implements SubCommand {

    static final List<Entry> COMMANDS = List.of(
            new Entry("/worlds help [page]", "worlds_help_help", "/worlds help", Permissions.HELP_WORLDS),
            new Entry("/worlds info [world]", "worlds_help_info", "/worlds info ", Permissions.INFO),
            new Entry("/worlds item", "worlds_help_item", "/worlds item", Permissions.NAVIGATOR_ITEM),
            new Entry("/worlds tp <world>", "worlds_help_tp", "/worlds tp ", Permissions.WORLDTP),
            new Entry("/worlds edit [world]", "worlds_help_edit", "/worlds edit ", Permissions.EDIT),
            new Entry(
                    "/worlds addBuilder [player]",
                    "worlds_help_addbuilder",
                    "/worlds addBuilder ",
                    Permissions.ADDBUILDER),
            new Entry(
                    "/worlds removeBuilder [player]",
                    "worlds_help_removebuilder",
                    "/worlds removeBuilder ",
                    Permissions.REMOVEBUILDER),
            new Entry("/worlds builders [world]", "worlds_help_builders", "/worlds builders ", Permissions.BUILDERS),
            new Entry("/worlds rename [world]", "worlds_help_rename", "/worlds rename ", Permissions.RENAME),
            new Entry("/worlds setItem [world]", "worlds_help_setitem", "/worlds setItem ", Permissions.SETITEM),
            new Entry(
                    "/worlds setCreator [world]",
                    "worlds_help_setcreator",
                    "/worlds setCreator ",
                    Permissions.SETCREATOR),
            new Entry(
                    "/worlds setProject [world]",
                    "worlds_help_setproject",
                    "/worlds setProject ",
                    Permissions.SETPROJECT),
            new Entry(
                    "/worlds saveTemplate [world] [template]",
                    "worlds_help_savetemplate",
                    "/worlds saveTemplate ",
                    Permissions.SAVETEMPLATE),
            new Entry(
                    "/worlds setPermission [world]",
                    "worlds_help_setpermission",
                    "/worlds setPermission ",
                    Permissions.SETPERMISSION),
            new Entry(
                    "/worlds setStatus [world]", "worlds_help_setstatus", "/worlds setStatus ", Permissions.SETSTATUS),
            new Entry("/worlds setSpawn", "worlds_help_setspawn", "/worlds setSpawn", Permissions.SETSPAWN),
            new Entry("/worlds removeSpawn", "worlds_help_removespawn", "/worlds removeSpawn", Permissions.REMOVESPAWN),
            new Entry("/worlds backup [create]", "worlds_help_backup", "/worlds backup", Permissions.BACKUP),
            new Entry(
                    "/worlds folder <folder> <add|remove|delete|setPermission|setProject|setItem> [world]",
                    "worlds_help_folder",
                    "/worlds folder ",
                    Permissions.FOLDER),
            new Entry("/worlds delete [world]", "worlds_help_delete", "/worlds delete ", Permissions.DELETE),
            new Entry("/worlds download [world]", "worlds_help_download", "/worlds download ", Permissions.DOWNLOAD),
            new Entry(
                    "/worlds import <world> [-g <generator>] [-c <creator>] [-t <type>]",
                    "worlds_help_import",
                    "/worlds import ",
                    Permissions.IMPORT),
            new Entry(
                    "/worlds importAll [-g <generator>] [-c <creator>]",
                    "worlds_help_importall",
                    "/worlds importAll",
                    Permissions.IMPORT_ALL),
            new Entry("/worlds unimport [world]", "worlds_help_unimport", "/worlds unimport ", Permissions.UNIMPORT));

    private final Messages messages;
    private final HelpPages pages;

    public HelpSubCommand(Messages messages) {
        this.messages = messages;
        this.pages = new HelpPages(messages, "worlds_help", COMMANDS);
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        if (!hasPermission(player)) {
            messages.sendPermissionError(player);
            return;
        }

        switch (args.length) {
            case 1 -> pages.send(player, 1);
            case 2 -> pages.send(player, args[1]);
            default -> messages.sendMessage(player, "worlds_help_usage");
        }
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.HELP;
    }
}
