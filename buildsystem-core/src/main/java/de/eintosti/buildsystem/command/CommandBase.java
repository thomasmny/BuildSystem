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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.i18n.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

@NullMarked
public abstract class CommandBase implements CommandExecutor, TabCompleter {

    protected final @Nullable BuildSystemPlugin plugin;
    protected final Messages messages;
    protected final Logger logger;
    private final boolean playerOnly;

    protected CommandBase(BuildSystemPlugin plugin, boolean playerOnly) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        this.logger = plugin.getLogger();
        this.playerOnly = playerOnly;
    }

    CommandBase(Logger logger, Messages messages, boolean playerOnly) {
        this.plugin = null;
        this.logger = logger;
        this.messages = messages;
        this.playerOnly = playerOnly;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (playerOnly) {
            if (!(sender instanceof Player player)) {
                logger.warning(messages.getString("sender_not_player", sender));
                return true;
            }
            run(player, label, args);
        } else {
            run(sender, label, args);
        }
        return true;
    }

    protected void run(Player player, String label, String[] args) {}

    protected void run(CommandSender sender, String label, String[] args) {}

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return sender instanceof Player player ? complete(player, label, args) : List.of();
    }

    protected List<String> complete(Player player, String label, String[] args) {
        return List.of();
    }

    protected boolean requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            messages.sendPermissionError(sender);
            return false;
        }
        return true;
    }

    protected String worldNameFromArgs(Player player, String[] args, int index) {
        return args.length <= index ? player.getWorld().getName() : args[index];
    }

    protected static void addArgument(String input, String argument, List<String> list) {
        if (input.isEmpty() || argument.toLowerCase().startsWith(input.toLowerCase())) {
            list.add(argument);
        }
    }
}
