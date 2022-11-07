/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command.subcommand.worlds;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.eintosti.buildsystem.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class TeleportSubCommand extends SubCommand {

    private final BuildSystem plugin;

    public TeleportSubCommand(BuildSystem plugin) {
        super(WorldsTabComplete.WorldsArgument.TP);

        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!hasPermission(player)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length != 2) {
            Messages.sendMessage(player, "worlds_tp_usage");
            return;
        }

        WorldManager worldManager = plugin.getWorldManager();
        BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_tp_unknown_world");
            return;
        }

        World bukkitWorld = Bukkit.getServer().getWorld(args[1]);
        if (buildWorld.isLoaded() && bukkitWorld == null) {
            Messages.sendMessage(player, "worlds_tp_unknown_world");
            return;
        }

        if (player.hasPermission(buildWorld.getPermission()) || buildWorld.getPermission().equalsIgnoreCase("-")) {
            worldManager.teleport(player, buildWorld);
        } else {
            Messages.sendMessage(player, "worlds_tp_entry_forbidden");
        }
    }
}