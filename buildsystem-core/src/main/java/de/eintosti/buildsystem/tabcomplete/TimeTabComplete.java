/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.tabcomplete;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimeTabComplete extends ArgumentSorter implements TabCompleter {

    private final WorldManager worldManager;

    public TimeTabComplete(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("day").setTabCompleter(this);
        plugin.getCommand("night").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return arrayList;
        }
        Player player = (Player) sender;

        switch (label.toLowerCase(Locale.ROOT)) {
            case "day":
                worldManager.getBuildWorlds().forEach(world -> {
                    String worldName = world.getName();
                    if (worldManager.isPermitted(player, "buildsystem.day", worldName)) {
                        addArgument(args[0], worldName, arrayList);
                    }
                });
                break;

            case "night":
                worldManager.getBuildWorlds().forEach(world -> {
                    String worldName = world.getName();
                    if (worldManager.isPermitted(player, "buildsystem.night", worldName)) {
                        addArgument(args[0], worldName, arrayList);
                    }
                });
                break;
        }

        return arrayList;
    }
}