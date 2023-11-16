/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;

public class TimeCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final ConfigValues configValues;
    private final WorldManager worldManager;

    public TimeCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("day").setExecutor(this);
        plugin.getCommand("night").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        Player player = (Player) sender;
        String worldName = args.length == 0 ? player.getWorld().getName() : args[0];
        World world = Bukkit.getWorld(worldName);

        switch (label.toLowerCase()) {
            case "day": {
                if (!worldManager.isPermitted(player, "buildsystem.day", worldName)) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                switch (args.length) {
                    case 0: {
                        world.setTime(configValues.getNoonTime());
                        Messages.sendMessage(player, "day_set", new AbstractMap.SimpleEntry<>("%world%", world.getName()));
                        break;
                    }
                    case 1: {
                        if (world == null) {
                            Messages.sendMessage(player, "day_unknown_world");
                            return true;
                        }
                        world.setTime(configValues.getNoonTime());
                        Messages.sendMessage(player, "day_set", new AbstractMap.SimpleEntry<>("%world%", world.getName()));
                        break;
                    }
                    default:
                        Messages.sendMessage(player, "day_usage");
                        break;
                }
                break;
            }

            case "night": {
                if (!worldManager.isPermitted(player, "buildsystem.night", worldName)) {
                    plugin.sendPermissionMessage(player);
                    return true;
                }

                switch (args.length) {
                    case 0: {
                        world.setTime(configValues.getNightTime());
                        Messages.sendMessage(player, "night_set", new AbstractMap.SimpleEntry<>("%world%", world.getName()));
                        break;
                    }
                    case 1: {
                        if (world == null) {
                            Messages.sendMessage(player, "night_unknown_world");
                            return true;
                        }
                        world.setTime(configValues.getNightTime());
                        Messages.sendMessage(player, "night_set", new AbstractMap.SimpleEntry<>("%world%", world.getName()));
                        break;
                    }
                    default:
                        Messages.sendMessage(player, "night_usage");
                        break;
                }
                break;
            }
        }
        return true;
    }
}