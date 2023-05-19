/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SpawnTabComplete extends ArgumentSorter implements TabCompleter {

    public SpawnTabComplete(BuildSystem plugin) {
        plugin.getCommand("spawn").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return arrayList;
        }
        Player player = (Player) sender;

        if (player.hasPermission("buildsystem.spawn")) {
            for (Argument argument : Argument.values()) {
                String argumentName = argument.name();
                addArgument(args[0], argumentName, arrayList);
            }
        }

        return arrayList;
    }

    private enum Argument {
        set, remove
    }
}