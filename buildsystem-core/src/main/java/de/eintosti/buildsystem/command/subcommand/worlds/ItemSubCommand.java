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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemSubCommand implements SubCommand {

    private final BuildSystem plugin;

    public ItemSubCommand(BuildSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!hasPermission(player)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        ItemStack navigator = plugin.getInventoryUtil().getItemStack(plugin.getConfigValues().getNavigatorItem(), Messages.getString("navigator_item", player));
        player.getInventory().addItem(navigator);
        Messages.sendMessage(player, "worlds_item_receive");
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.ITEM;
    }
}