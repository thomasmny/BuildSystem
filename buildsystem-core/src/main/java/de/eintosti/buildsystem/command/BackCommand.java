/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.player.BuildPlayer;
import de.eintosti.buildsystem.player.PlayerManager;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
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
            Messages.sendMessage(player, "back_usage");
        }

        return true;
    }

    private void teleportBack(Player player) {
        UUID playerUuid = player.getUniqueId();
        BuildPlayer buildPlayer = playerManager.getBuildPlayer(playerUuid);
        Location previousLocation = buildPlayer.getPreviousLocation();

        if (previousLocation == null) {
            Messages.sendMessage(player, "back_failed");
            return;
        }

        PaperLib.teleportAsync(player, previousLocation)
                .whenComplete((completed, throwable) -> {
                    if (!completed) {
                        return;
                    }
                    XSound.ENTITY_ZOMBIE_INFECT.play(player);
                    Messages.sendMessage(player, "back_teleported");
                    buildPlayer.setPreviousLocation(null);
                });
    }
}