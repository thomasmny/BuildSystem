/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GamemodeTabComplete extends ArgumentSorter implements TabCompleter {

    public GamemodeTabComplete(BuildSystem plugin) {
        plugin.getCommand("gamemode").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return arrayList;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.gamemode")) {
            return arrayList;
        }

        if (args.length == 1) {
            for (GameMode gameMode : GameMode.values()) {
                addArgument(args[0], gameMode.name().toLowerCase(Locale.ROOT), arrayList);
            }
        } else if (args.length == 2) {
            if (!player.hasPermission("buildsystem.gamemode.others")) {
                return arrayList;
            }

            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "survival":
                case "s":
                case "0":
                case "creative":
                case "c":
                case "1":
                case "adventure":
                case "a":
                case "2":
                case "spectator":
                case "sp":
                case "3":
                    Bukkit.getOnlinePlayers().forEach(pl -> addArgument(args[1], pl.getName(), arrayList));
                    break;
            }
        }

        return arrayList;
    }
}