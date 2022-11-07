/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command.subcommand.worlds;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class DeleteSubCommand extends SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public DeleteSubCommand(BuildSystem plugin, String worldName) {
        super(WorldsTabComplete.WorldsArgument.DELETE);

        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        WorldManager worldManager = plugin.getWorldManager();
        if (!worldManager.isPermitted(player, getArgument().getPermission(), worldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length > 2) {
            Messages.sendMessage(player, "worlds_delete_usage");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_delete_unknown_world");
            return;
        }

        plugin.getPlayerManager().getBuildPlayer(player).setCachedWorld(buildWorld);
        plugin.getDeleteInventory().openInventory(player, buildWorld);
    }
}