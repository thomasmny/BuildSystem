/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;

/**
 * @author einTosti
 */
public class ExplosionsCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public ExplosionsCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("explosions").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(Messages.getString("sender_not_player"));
            return true;
        }

        Player player = (Player) sender;
        String worldName = args.length == 0 ? player.getWorld().getName() : args[0];
        if (!worldManager.isPermitted(player, "buildsystem.explosions", worldName)) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        switch (args.length) {
            case 0:
                toggleExplosions(player, player.getWorld());
                break;
            case 1:
                toggleExplosions(player, Bukkit.getWorld(args[0]));
                break;
            default:
                Messages.sendMessage(player, "explosions_usage");
                break;
        }

        return true;
    }

    private void toggleExplosions(Player player, World bukkitWorld) {
        if (bukkitWorld == null) {
            Messages.sendMessage(player, "explosions_unknown_world");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());
        if (buildWorld == null) {
            Messages.sendMessage(player, "explosions_world_not_imported");
            return;
        }

        if (!buildWorld.isExplosions()) {
            buildWorld.setExplosions(true);
            Messages.sendMessage(player, "explosions_activated", new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()));
        } else {
            buildWorld.setExplosions(false);
            Messages.sendMessage(player, "explosions_deactivated", new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()));
        }
    }
}