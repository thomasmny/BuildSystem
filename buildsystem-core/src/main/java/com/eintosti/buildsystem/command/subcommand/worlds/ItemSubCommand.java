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
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class ItemSubCommand implements SubCommand {

    private final BuildSystem plugin;

    public ItemSubCommand(BuildSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!player.hasPermission("buildsystem.navigator.item")) {
            plugin.sendPermissionMessage(player);
            return;
        }

        player.getInventory().addItem(plugin.getInventoryManager().getItemStack(plugin.getConfigValues().getNavigatorItem(), plugin.getString("navigator_item")));
        player.sendMessage(plugin.getString("worlds_item_receive"));
    }
}