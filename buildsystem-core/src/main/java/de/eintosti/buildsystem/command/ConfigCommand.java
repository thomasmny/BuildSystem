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
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ConfigCommand implements CommandExecutor {

    private final BuildSystem plugin;

    public ConfigCommand(BuildSystem plugin) {
        this.plugin = plugin;
        plugin.getCommand("config").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!sender.hasPermission("buildsystem.config")) {
            plugin.sendPermissionMessage(sender);
            return true;
        }

        if (args.length != 1) {
            Messages.sendMessage(sender, "config_usage");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "rl":
            case "reload":
                plugin.reloadConfig();
                plugin.reloadConfigData(true);
                Messages.sendMessage(sender, "config_reloaded");
                break;
            default:
                Messages.sendMessage(sender, "config_usage");
                break;
        }

        return true;
    }
}