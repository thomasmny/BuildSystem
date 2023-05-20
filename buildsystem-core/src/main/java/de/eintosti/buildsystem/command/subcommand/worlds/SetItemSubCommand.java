/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.AbstractMap;

public class SetItemSubCommand implements SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public SetItemSubCommand(BuildSystem plugin, String worldName) {
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
            Messages.sendMessage(player, "worlds_setitem_usage");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_setitem_unknown_world");
            return;
        }

        ItemStack itemStack = player.getItemInHand();
        if (itemStack.getType() == Material.AIR) {
            Messages.sendMessage(player, "worlds_setitem_hand_empty");
            return;
        }

        buildWorld.getData().material().set(XMaterial.matchXMaterial(itemStack));
        Messages.sendMessage(player, "worlds_setitem_set", new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()));
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.SET_ITEM;
    }
}