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
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class RemoveSpawnSubCommand extends SubCommand {

    private final BuildSystem plugin;

    public RemoveSpawnSubCommand(BuildSystem plugin) {
        super(WorldsTabComplete.WorldsArgument.REMOVE_SPAWN);

        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        WorldManager worldManager = plugin.getWorldManager();
        String playerWorldName = player.getWorld().getName();
        if (!worldManager.isPermitted(player, getArgument().getPermission(), playerWorldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("worlds_removespawn_world_not_imported"));
            return;
        }

        buildWorld.removeCustomSpawn();
        player.sendMessage(plugin.getString("worlds_removespawn_world_spawn_removed").replace("%world%", buildWorld.getName()));
    }
}