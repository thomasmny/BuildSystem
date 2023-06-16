/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command.subcommand.worlds;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.world.BuildWorldManager;
import de.eintosti.buildsystem.world.CraftBuildWorld;
import org.bukkit.entity.Player;

public class BuildersSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;
    private final String worldName;

    public BuildersSubCommand(BuildSystemPlugin plugin, String worldName) {
        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        BuildWorldManager worldManager = plugin.getWorldManager();
        if (!worldManager.isPermitted(player, getArgument().getPermission(), worldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length > 2) {
            Messages.sendMessage(player, "worlds_builders_usage");
            return;
        }

        CraftBuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_builders_unknown_world");
            return;
        }

        plugin.getPlayerManager().getBuildPlayer(player).setCachedWorld(buildWorld);
        plugin.getBuilderInventory().openInventory(buildWorld, player);
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.BUILDERS;
    }
}