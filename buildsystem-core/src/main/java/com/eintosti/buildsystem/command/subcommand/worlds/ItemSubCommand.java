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
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class ItemSubCommand extends SubCommand {

    private final BuildSystem plugin;

    public ItemSubCommand(BuildSystem plugin) {
        super(WorldsTabComplete.WorldsArgument.ITEM);

        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!hasPermission(player)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        player.getInventory().addItem(plugin.getInventoryUtil().getItemStack(plugin.getConfigValues().getNavigatorItem(), Messages.getString("navigator_item")));
        Messages.sendMessage(player, "worlds_item_receive");
    }
}