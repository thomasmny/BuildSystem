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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConfigCommand extends CommandBase {

    public ConfigCommand(BuildSystemPlugin plugin) {
        super(plugin, false);
    }

    @Override
    protected void run(CommandSender sender, String label, String[] args) {
        if (!requirePermission(sender, "buildsystem.config")) {
            return;
        }

        if (args.length != 1) {
            messages.sendMessage(sender, "config_usage");
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "rl":
            case "reload":
                messages.reload();
                plugin.reloadConfigData(true);
                messages.sendMessage(sender, "config_reloaded");
                break;
            default:
                messages.sendMessage(sender, "config_usage");
                break;
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (player.hasPermission("buildsystem.config")) {
            list.add("reload");
        }
        return list;
    }
}
