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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.util.WorldPermissions;
import de.eintosti.buildsystem.config.Config.World.Default.Time;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TimeCommand implements CommandExecutor {

    private final BuildSystemPlugin plugin;

    public TimeCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("day").setExecutor(this);
        plugin.getCommand("night").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        String worldName = args.length == 0 ? player.getWorld().getName() : args[0];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Messages.sendMessage(player, "day_unknown_world");
            return true;
        }

        BuildWorld buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(world);
        WorldPermissions permissions = buildWorld.getPermissions();

        switch (label.toLowerCase(Locale.ROOT)) {
            case "day" -> {
                if (!permissions.canPerformCommand(player, "buildsystem.day")) {
                    Messages.sendPermissionError(player);
                    return true;
                }

                switch (args.length) {
                    case 0, 1 -> {
                        world.setTime(Time.noon);
                        Messages.sendMessage(player, "day_set", Map.entry("%world%", world.getName()));
                    }
                    default -> Messages.sendMessage(player, "day_usage");
                }
            }

            case "night" -> {
                if (!permissions.canPerformCommand(player, "buildsystem.night")) {
                    Messages.sendPermissionError(player);
                    return true;
                }

                switch (args.length) {
                    case 0, 1 -> {
                        world.setTime(Time.night);
                        Messages.sendMessage(player, "night_set", Map.entry("%world%", world.getName()));
                    }
                    default -> {
                        Messages.sendMessage(player, "night_usage");
                    }
                }
            }
        }
        return true;
    }
}