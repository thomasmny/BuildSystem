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

public class UnimportSubCommand implements SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public UnimportSubCommand(BuildSystem plugin, String worldName) {
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
            Messages.sendMessage(player, "worlds_unimport_usage");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_unimport_unknown_world");
            return;
        }

        worldManager.unimportWorld(buildWorld, true);
        Messages.sendMessage(player, "worlds_unimport_finished", new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()));
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.UNIMPORT;
    }
}