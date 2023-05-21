/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.entity.Player;

import java.util.AbstractMap;

public class SetPermissionSubCommand implements SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public SetPermissionSubCommand(BuildSystem plugin, String worldName) {
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
            Messages.sendMessage(player, "worlds_setpermission_usage");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_setpermission_unknown_world");
            return;
        }

        getPermissionInput(player, buildWorld, true);
    }

    public void getPermissionInput(Player player, BuildWorld buildWorld, boolean closeInventory) {
        new PlayerChatInput(plugin, player, "enter_world_permission", input -> {
            buildWorld.getData().permission().set(input.trim());
            plugin.getPlayerManager().forceUpdateSidebar(buildWorld);

            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            Messages.sendMessage(player, "worlds_setpermission_set", new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()));

            if (closeInventory) {
                player.closeInventory();
            } else {
                player.openInventory(plugin.getEditInventory().getInventory(player, buildWorld));
            }
        });
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.SET_PERMISSION;
    }
}