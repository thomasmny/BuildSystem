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
import org.bukkit.entity.Player;

import java.util.AbstractMap;

public class RemoveSpawnSubCommand implements SubCommand {

    private final BuildSystem plugin;

    public RemoveSpawnSubCommand(BuildSystem plugin) {
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
            Messages.sendMessage(player, "worlds_removespawn_world_not_imported");
            return;
        }

        buildWorld.getData().customSpawn().set(null);
        Messages.sendMessage(player, "worlds_removespawn_world_spawn_removed", new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()));
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.REMOVE_SPAWN;
    }
}