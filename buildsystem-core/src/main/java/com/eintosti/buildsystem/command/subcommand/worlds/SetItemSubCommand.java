/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 */
public class SetItemSubCommand extends SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public SetItemSubCommand(BuildSystem plugin, String worldName) {
        super(WorldsTabComplete.WorldsArgument.SET_ITEM);

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
            player.sendMessage(plugin.getString("worlds_setitem_usage"));
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            player.sendMessage(plugin.getString("worlds_setitem_unknown_world"));
            return;
        }

        ItemStack itemStack = player.getItemInHand();
        if (itemStack.getType() == Material.AIR) {
            player.sendMessage(plugin.getString("worlds_setitem_hand_empty"));
            return;
        }

        buildWorld.setMaterial(XMaterial.matchXMaterial(itemStack));
        player.sendMessage(plugin.getString("worlds_setitem_set").replace("%world%", buildWorld.getName()));
    }
}