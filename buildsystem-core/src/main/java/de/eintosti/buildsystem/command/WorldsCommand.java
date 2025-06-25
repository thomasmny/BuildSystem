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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.AddBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.BuildersSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.DeleteSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.EditSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.FolderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.HelpSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ImportAllSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ImportSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.InfoSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.ItemSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RemoveBuilderSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RemoveSpawnSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.RenameSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetCreatorSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetItemSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetPermissionSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetProjectSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetSpawnSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.SetStatusSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.TeleportSubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.UnimportSubCommand;
import de.eintosti.buildsystem.navigator.inventory.NavigatorInventory;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldsCommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;

    public WorldsCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("worlds").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        if (args.length == 0) {
            if (!player.hasPermission("buildsystem.navigator")) {
                Messages.sendPermissionError(player);
                return true;
            }

            new NavigatorInventory(plugin).openInventory(player);
            XSound.BLOCK_CHEST_OPEN.play(player);
            return true;
        }

        WorldsTabComplete.WorldsArgument argument = WorldsTabComplete.WorldsArgument.matchArgument(args[0]);
        if (argument == null) {
            Messages.sendMessage(player, "worlds_unknown_command");
            return true;
        }

        // Most commands use the structure /worlds <argument> <world> <...> which is why we assume that args[1] is the world name
        // Make sure to change if this is not the case for any specific command
        String worldName = args.length >= 2 ? args[1] : player.getWorld().getName();

        SubCommand subCommand = switch (argument) {
            case ADD_BUILDER -> new AddBuilderSubCommand(plugin, player.getWorld().getName());
            case BUILDERS -> new BuildersSubCommand(plugin, worldName);
            case DELETE -> new DeleteSubCommand(plugin, worldName);
            case EDIT -> new EditSubCommand(plugin, worldName);
            case FOLDER -> new FolderSubCommand(plugin);
            case HELP -> new HelpSubCommand();
            case IMPORT_ALL -> new ImportAllSubCommand(plugin);
            case IMPORT -> new ImportSubCommand(plugin, worldName);
            case INFO -> new InfoSubCommand(plugin, worldName);
            case ITEM -> new ItemSubCommand(plugin);
            case REMOVE_BUILDER -> new RemoveBuilderSubCommand(plugin, player.getWorld().getName());
            case REMOVE_SPAWN -> new RemoveSpawnSubCommand(plugin);
            case RENAME -> new RenameSubCommand(plugin, worldName);
            case SET_CREATOR -> new SetCreatorSubCommand(plugin, worldName);
            case SET_ITEM -> new SetItemSubCommand(plugin, worldName);
            case SET_PERMISSION -> new SetPermissionSubCommand(plugin, worldName);
            case SET_PROJECT -> new SetProjectSubCommand(plugin, worldName);
            case SET_SPAWN -> new SetSpawnSubCommand(plugin);
            case SET_STATUS -> new SetStatusSubCommand(plugin, worldName);
            case TP -> new TeleportSubCommand(plugin);
            case UNIMPORT -> new UnimportSubCommand(plugin, worldName);
        };
        subCommand.execute(player, args);
        return true;
    }
}