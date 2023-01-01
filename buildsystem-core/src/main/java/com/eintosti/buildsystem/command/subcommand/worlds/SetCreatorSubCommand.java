/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.eintosti.buildsystem.util.UUIDFetcher;
import com.eintosti.buildsystem.util.external.PlayerChatInput;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import org.bukkit.entity.Player;

import java.util.AbstractMap;

/**
 * @author einTosti
 */
public class SetCreatorSubCommand extends SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public SetCreatorSubCommand(BuildSystem plugin, String worldName) {
        super(WorldsTabComplete.WorldsArgument.SET_CREATOR);

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
            Messages.sendMessage(player, "worlds_setcreator_usage");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_setcreator_unknown_world");
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_creator", input -> {
            String creator = input.trim();
            buildWorld.setCreator(creator);
            if (!creator.equalsIgnoreCase("-")) {
                buildWorld.setCreatorId(UUIDFetcher.getUUID(creator));
            } else {
                buildWorld.setCreatorId(null);
            }

            plugin.getPlayerManager().forceUpdateSidebar(buildWorld);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            Messages.sendMessage(player, "worlds_setcreator_set", new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()));
            player.closeInventory();
        });
    }
}