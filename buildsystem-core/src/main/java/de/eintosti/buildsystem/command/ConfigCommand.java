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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConfigCommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;

    public ConfigCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("config").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("buildsystem.config")) {
            Messages.sendPermissionError(sender);
            return true;
        }

        if (args.length != 1) {
            Messages.sendMessage(sender, "config_usage");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "rl":
            case "reload":
                Messages.reloadMessages();
                plugin.reloadConfigData(true);
                Messages.sendMessage(sender, "config_reloaded");
                break;
            default:
                Messages.sendMessage(sender, "config_usage");
                break;
        }

        return true;
    }
}