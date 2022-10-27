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
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class SetStatusSubCommand implements SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public SetStatusSubCommand(BuildSystem plugin, String worldName) {
        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        WorldManager worldManager = plugin.getWorldManager();
        if (!worldManager.isPermitted(player, "buildsystem.setstatus", worldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length > 2) {
            player.sendMessage(plugin.getString("worlds_setstatus_usage"));
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("worlds_setstatus_unknown_world"));
            return;
        }

        plugin.getPlayerManager().getBuildPlayer(player).setCachedWorld(buildWorld);
        plugin.getStatusInventory().openInventory(player);
    }
}