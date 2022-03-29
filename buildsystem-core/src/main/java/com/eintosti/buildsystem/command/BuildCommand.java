/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command;

import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author einTosti
 */
public class BuildCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final PlayerManager playerManager;

    public BuildCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        plugin.getCommand("build").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(plugin.getString("sender_not_player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("buildsystem.build")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        switch (args.length) {
            case 0:
                toggleBuildMode(player, null, true);
                break;
            case 1:
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(plugin.getString("build_player_not_found"));
                    return true;
                }
                toggleBuildMode(target, player, false);
                break;
            default:
                player.sendMessage(plugin.getString("build_usage"));
                break;
        }

        return true;
    }

    private void toggleBuildMode(Player target, Player sender, boolean self) {
        UUID targetUuid = target.getUniqueId();

        if (playerManager.getBuildPlayers().contains(targetUuid)) {
            playerManager.getBuildPlayers().remove(targetUuid);
            if (playerManager.getPlayerGamemode().containsKey(targetUuid)) {
                target.setGameMode(playerManager.getPlayerGamemode().get(targetUuid));
                playerManager.getPlayerGamemode().remove(targetUuid);
            }

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(target);
            if (self) {
                target.sendMessage(plugin.getString("build_deactivated_self"));
            } else {
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(sender);
                sender.sendMessage(plugin.getString("build_deactivated_other_sender").replace("%target%", target.getName()));
                target.sendMessage(plugin.getString("build_deactivated_other_target").replace("%sender%", sender.getName()));
            }
        } else {
            playerManager.getBuildPlayers().add(targetUuid);
            playerManager.getPlayerGamemode().put(targetUuid, target.getGameMode());
            target.setGameMode(GameMode.CREATIVE);

            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(target);
            if (self) {
                target.sendMessage(plugin.getString("build_activated_self"));
            } else {
                XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(sender);
                sender.sendMessage(plugin.getString("build_activated_other_sender").replace("%target%", target.getName()));
                target.sendMessage(plugin.getString("build_activated_other_target").replace("%sender%", sender.getName()));
            }
        }
    }
}