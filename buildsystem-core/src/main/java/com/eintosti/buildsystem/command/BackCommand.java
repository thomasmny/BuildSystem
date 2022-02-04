/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.PlayerManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class BackCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final PlayerManager playerManager;

    public BackCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        plugin.getCommand("back").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("buildsystem.back")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        if (args.length == 0) {
            teleportBack(player);
        } else {
            player.sendMessage(plugin.getString("back_usage"));
        }

        return true;
    }

    private void teleportBack(Player player) {
        UUID playerUuid = player.getUniqueId();
        Location previousLocation = playerManager.getPreviousLocation().get(playerUuid);

        if (previousLocation == null) {
            player.sendMessage(plugin.getString("back_failed"));
            return;
        }

        player.teleport(previousLocation);
        player.sendMessage(plugin.getString("back_teleported"));
        playerManager.getPreviousLocation().remove(playerUuid);
    }
}
