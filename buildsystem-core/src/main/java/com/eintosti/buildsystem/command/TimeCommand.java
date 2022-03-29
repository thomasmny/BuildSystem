/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.config.ConfigValues;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author einTosti
 */
public class TimeCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    public TimeCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();
        plugin.getCommand("day").setExecutor(this);
        plugin.getCommand("night").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(plugin.getString("sender_not_player"));
            return true;
        }

        Player player = (Player) sender;

        switch (label.toLowerCase()) {
            case "day": {
                if (!player.hasPermission("buildsystem.day")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                switch (args.length) {
                    case 0: {
                        World world = player.getWorld();
                        world.setTime(configValues.getNoonTime());
                        player.sendMessage(plugin.getString("day_set").replace("%world%", world.getName()));
                        break;
                    }
                    case 1: {
                        World world = Bukkit.getWorld(args[0]);
                        if (world == null) {
                            player.sendMessage(plugin.getString("day_unknown_world"));
                            return true;
                        }
                        world.setTime(configValues.getNoonTime());
                        player.sendMessage(plugin.getString("day_set").replace("%world%", world.getName()));
                        break;
                    }
                    default:
                        player.sendMessage(plugin.getString("day_usage"));
                        break;
                }
                break;
            }

            case "night": {
                if (!player.hasPermission("buildsystem.night")) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                switch (args.length) {
                    case 0: {
                        World world = player.getWorld();
                        world.setTime(configValues.getNightTime());
                        player.sendMessage(plugin.getString("night_set").replace("%world%", world.getName()));
                        break;
                    }
                    case 1: {
                        World world = Bukkit.getWorld(args[0]);
                        if (world == null) {
                            player.sendMessage(plugin.getString("night_unknown_world"));
                            return true;
                        }
                        world.setTime(configValues.getNightTime());
                        player.sendMessage(plugin.getString("night_set").replace("%world%", world.getName()));
                        break;
                    }
                    default:
                        player.sendMessage(plugin.getString("night_usage"));
                        break;
                }
                break;
            }
        }
        return true;
    }
}