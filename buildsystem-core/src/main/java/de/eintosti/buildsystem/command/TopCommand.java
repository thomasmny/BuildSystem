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
import de.eintosti.buildsystem.world.WorldManager;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TopCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public TopCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("top").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("buildsystem.top")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        if (args.length != 0) {
            Messages.sendMessage(player, "top_usage");
            return true;
        }

        sendToTop(player);
        return true;
    }

    private void sendToTop(Player player) {
        World bukkitWorld = player.getWorld();
        Location playerLocation = player.getLocation();
        Location blockLocation = null;

        for (int y = bukkitWorld.getMaxHeight(); y > 0; y--) {
            Block block = bukkitWorld.getBlockAt(playerLocation.getBlockX(), y, playerLocation.getBlockZ());
            if (worldManager.isSafeLocation(block.getLocation()) && y > playerLocation.getY()) {
                blockLocation = block.getLocation();
                break;
            }
        }

        if (blockLocation != null && !Objects.equals(blockLocation.getBlock(), playerLocation.getBlock())) {
            PaperLib.teleportAsync(player, blockLocation.add(0.5, 0, 0.5));
            Messages.sendMessage(player, "top_teleported");
        } else {
            Messages.sendMessage(player, "top_failed");
        }
    }
}