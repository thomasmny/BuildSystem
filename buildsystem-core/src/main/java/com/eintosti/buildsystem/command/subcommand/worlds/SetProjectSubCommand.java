/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.util.external.PlayerChatInput;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class SetProjectSubCommand implements SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public SetProjectSubCommand(BuildSystem plugin, String worldName) {
        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        WorldManager worldManager = plugin.getWorldManager();
        if (!worldManager.isPermitted(player, "buildsystem.setproject", worldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length > 2) {
            player.sendMessage(plugin.getString("worlds_setproject_usage"));
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("worlds_setproject_unknown_world"));
            return;
        }

        getProjectInput(player, buildWorld, true);
    }

    public void getProjectInput(Player player, BuildWorld buildWorld, boolean closeInventory) {
        new PlayerChatInput(plugin, player, "enter_world_project", input -> {
            buildWorld.setProject(input.trim());
            plugin.getPlayerManager().forceUpdateSidebar(buildWorld);

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            player.sendMessage(plugin.getString("worlds_setproject_set").replace("%world%", buildWorld.getName()));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getEditInventory().getInventory(player, buildWorld));
            }
        });
    }
}