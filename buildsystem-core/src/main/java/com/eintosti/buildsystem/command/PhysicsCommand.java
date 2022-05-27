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
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author einTosti
 */
public class PhysicsCommand implements CommandExecutor {

    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public PhysicsCommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("physics").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().warning(plugin.getString("sender_not_player"));
            return true;
        }

        Player player = (Player) sender;
        String worldName = args.length == 0 ? player.getWorld().getName() : args[0];
        if (!plugin.isPermitted(player, "buildsystem.physics", worldName)) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        switch (args.length) {
            case 0:
                togglePhysics(player, player.getWorld());
                break;
            case 1:
                //TODO: Check each world for permission individually?
                if (args[0].equalsIgnoreCase("all") && worldManager.getBuildWorld("all") == null) {
                    worldManager.getBuildWorlds().forEach(buildWorld -> buildWorld.setPhysics(true));
                    player.sendMessage(plugin.getString("physics_activated_all"));
                } else {
                    togglePhysics(player, Bukkit.getWorld(args[0]));
                }
                break;
            default:
                player.sendMessage(plugin.getString("physics_usage"));
                break;
        }
        return true;
    }

    private void togglePhysics(Player player, World bukkitWorld) {
        if (bukkitWorld == null) {
            player.sendMessage(plugin.getString("physics_unknown_world"));
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("physics_world_not_imported"));
            return;
        }

        if (!buildWorld.isPhysics()) {
            buildWorld.setPhysics(true);
            player.sendMessage(plugin.getString("physics_activated").replace("%world%", buildWorld.getName()));
        } else {
            buildWorld.setPhysics(false);
            player.sendMessage(plugin.getString("physics_deactivated").replace("%world%", buildWorld.getName()));
        }
    }
}