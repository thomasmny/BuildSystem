/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command.subcommand.worlds;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeleportSubCommand implements SubCommand {

    private final BuildSystem plugin;

    public TeleportSubCommand(BuildSystem plugin) {
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

        String permission = buildWorld.getData().permission().get();
        if (player.hasPermission(permission) || permission.equalsIgnoreCase("-")) {
            worldManager.teleport(player, buildWorld);
        } else {
            Messages.sendMessage(player, "worlds_tp_entry_forbidden");
        }
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.TP;
    }
}