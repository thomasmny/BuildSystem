/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.util.ManageEntityAI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * @author einTosti
 */
public class NoAICommand implements CommandExecutor {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public NoAICommand(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("noai").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLogger().log(Level.WARNING, plugin.getString("sender_not_player"));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("buildsystem.noai")) {
            plugin.sendPermissionMessage(player);
            return true;
        }

        switch (args.length) {
            case 0:
                toggleAI(player, player.getWorld());
                break;
            case 1:
                toggleAI(player, Bukkit.getWorld(args[0]));
                break;
            default:
                player.sendMessage(plugin.getString("noai_usage"));
                break;
        }
        return true;
    }

    private void toggleAI(Player player, World bukkitWorld) {
        if (bukkitWorld == null) {
            player.sendMessage(plugin.getString("noai_unknown_world"));
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("noai_world_not_imported"));
            return;
        }

        if (buildWorld.isMobAI()) {
            buildWorld.setMobAI(false);
            player.sendMessage(plugin.getString("noai_activated").replace("%world%", buildWorld.getName()));
        } else {
            buildWorld.setMobAI(true);
            player.sendMessage(plugin.getString("noai_deactivated").replace("%world%", buildWorld.getName()));
        }

        boolean mobAI = buildWorld.isMobAI();
        for (Entity entity : bukkitWorld.getEntities()) {
            if (entity instanceof LivingEntity) {
                ManageEntityAI.setAIEnabled(entity, mobAI);
            }
        }
    }
}
