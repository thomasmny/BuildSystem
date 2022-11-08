/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.Titles;
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
public class EditSubCommand extends SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public EditSubCommand(BuildSystem plugin, String worldName) {
        super(WorldsTabComplete.WorldsArgument.EDIT);

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
            Messages.sendMessage(player, "worlds_edit_usage");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_edit_unknown_world");
            return;
        }

        if (buildWorld.isLoaded()) {
            plugin.getPlayerManager().getBuildPlayer(player).setCachedWorld(buildWorld);
            plugin.getEditInventory().openInventory(player, buildWorld);
        } else {
            XSound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.play(player);
            Titles.sendTitle(player, 5, 70, 20, " ", Messages.getString("world_not_loaded"));
        }
    }
}