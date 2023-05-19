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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The tab completer which is used for commands which do not have any arguments.
 *
 * @author einTosti
 */
public class EmptyTabComplete implements TabCompleter {

    public EmptyTabComplete(BuildSystem plugin) {
        plugin.getCommand("back").setTabCompleter(this);
        plugin.getCommand("blocks").setTabCompleter(this);
        plugin.getCommand("buildsystem").setTabCompleter(this);
        plugin.getCommand("settings").setTabCompleter(this);
        plugin.getCommand("setup").setTabCompleter(this);
        plugin.getCommand("top").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        return new ArrayList<>();
    }
}