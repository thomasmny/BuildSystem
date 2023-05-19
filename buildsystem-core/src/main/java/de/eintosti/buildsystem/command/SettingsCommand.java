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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SettingsCommand implements CommandExecutor {

    private final BuildSystem plugin;

    public SettingsCommand(BuildSystem plugin) {
        this.plugin = plugin;
        plugin.getCommand("settings").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("buildsystem.settings")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        plugin.getSettingsInventory().openInventory(player);
        return true;
    }
}