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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;

public class SpeedCommand implements CommandExecutor {

    private final BuildSystem plugin;

    public SpeedCommand(BuildSystem plugin) {
        this.plugin = plugin;
        plugin.getCommand("speed").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player", null));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("buildsystem.speed")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        switch (args.length) {
            case 0:
                plugin.getSpeedInventory().openInventory(player);
                XSound.BLOCK_CHEST_OPEN.play(player);
                break;
            case 1:
                String speedString = args[0];
                switch (speedString) {
                    case "1":
                        setSpeed(player, 0.2f, speedString);
                        break;
                    case "2":
                        setSpeed(player, 0.4f, speedString);
                        break;
                    case "3":
                        setSpeed(player, 0.6f, speedString);
                        break;
                    case "4":
                        setSpeed(player, 0.8f, speedString);
                        break;
                    case "5":
                        setSpeed(player, 1.0f, speedString);
                        break;
                    default:
                        Messages.sendMessage(player, "speed_usage");
                        break;
                }
                break;
            default:
                Messages.sendMessage(player, "speed_usage");
                break;
        }

        return true;
    }

    private void setSpeed(Player player, float speed, String speedString) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            Messages.sendMessage(player, "speed_set_flying", new AbstractMap.SimpleEntry<>("%speed%", speedString));
        } else {
            player.setWalkSpeed(speed);
            Messages.sendMessage(player, "speed_set_walking", new AbstractMap.SimpleEntry<>("%speed%", speedString));
        }
    }
}