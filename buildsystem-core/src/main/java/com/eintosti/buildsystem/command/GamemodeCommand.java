/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command;

import com.eintosti.buildsystem.BuildSystem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author einTosti
 */
public class GamemodeCommand implements CommandExecutor {

    private final BuildSystem plugin;

    public GamemodeCommand(BuildSystem plugin) {
        this.plugin = plugin;
        plugin.getCommand("gamemode").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(plugin.getString("sender_not_player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("buildsystem.gamemode")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "survival":
                case "s":
                case "0":
                    setGamemode(player, args, GameMode.SURVIVAL, plugin.getString("gamemode_survival"));
                    break;
                case "creative":
                case "c":
                case "1":
                    setGamemode(player, args, GameMode.CREATIVE, plugin.getString("gamemode_creative"));
                    break;
                case "adventure":
                case "a":
                case "2":
                    setGamemode(player, args, GameMode.ADVENTURE, plugin.getString("gamemode_adventure"));
                    break;
                case "spectator":
                case "sp":
                case "3":
                    setGamemode(player, args, GameMode.SPECTATOR, plugin.getString("gamemode_spectator"));
                    break;
                default:
                    sendUsageMessage(player);
                    break;
            }
        }

        return true;
    }

    private void setGamemode(Player player, String[] args, GameMode gameMode, String gameModeName) {
        switch (args.length) {
            case 1:
                this.setPlayerGamemode(player, gameMode, gameModeName);
                break;
            case 2:
                this.setTargetGamemode(player, args, gameMode, gameModeName);
                break;
            default:
                this.sendUsageMessage(player);
        }
    }

    private void sendUsageMessage(Player player) {
        player.sendMessage(plugin.getString("gamemode_usage"));
    }

    private void setPlayerGamemode(Player player, GameMode gameMode, String gameModeName) {
        if (!player.hasPermission("buildsystem.gamemode")) {
            plugin.sendPermissionMessage(player);
            return;
        }

        player.setGameMode(gameMode);
        player.sendMessage(plugin.getString("gamemode_set_self").replace("%gamemode%", gameModeName));
    }

    private void setTargetGamemode(Player player, String[] args, GameMode gameMode, String gameModeName) {
        if (!player.hasPermission("buildsystem.gamemode.others")) {
            plugin.sendPermissionMessage(player);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(plugin.getString("gamemode_player_not_found"));
            return;
        }

        target.setGameMode(gameMode);
        target.sendMessage(plugin.getString("gamemode_set_self")
                .replace("%gamemode%", gameModeName));
        player.sendMessage(plugin.getString("gamemode_set_other")
                .replace("%target%", target.getName())
                .replace("%gamemode%", gameModeName));
    }
}
