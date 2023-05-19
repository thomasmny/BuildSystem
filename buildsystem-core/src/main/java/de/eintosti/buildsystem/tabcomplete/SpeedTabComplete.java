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

public class SpeedTabComplete implements TabCompleter {

    public SpeedTabComplete(BuildSystem plugin) {
        plugin.getCommand("speed").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            arrayList.add(String.valueOf(i));
        }
        return arrayList;
    }
}