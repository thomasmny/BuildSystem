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
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.eintosti.buildsystem.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.AbstractMap;

/**
 * @author einTosti
 */
public class InfoSubCommand extends SubCommand {

    private final BuildSystem plugin;
    private String worldName;

    public InfoSubCommand(BuildSystem plugin, String worldName) {
        super(WorldsTabComplete.WorldsArgument.INFO);

        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        WorldManager worldManager = plugin.getWorldManager();
        World playerWorld = player.getWorld();
        if (args.length != 2) {
            // When running /worlds info, use the player's world when checking for permission
            worldName = playerWorld.getName();
        }

        if (!worldManager.isPermitted(player, getArgument().getPermission(), worldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(playerWorld.getName());
        if (args.length != 2) {
            Messages.sendMessage(player, "worlds_info_usage");
            return;
        }

        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_info_unknown_world");
            return;
        }
        buildWorld = worldManager.getBuildWorld(args[1]);

        //TODO: Print information about the custom generator?
        Messages.sendMessage(player, "world_info",
                new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()),
                new AbstractMap.SimpleEntry<>("%creator%", buildWorld.getCreatorId()),
                new AbstractMap.SimpleEntry<>("%type%", buildWorld.getType().getName()),
                new AbstractMap.SimpleEntry<>("%private%", buildWorld.isPrivate()),
                new AbstractMap.SimpleEntry<>("%builders_enabled%", buildWorld.isBuilders()),
                new AbstractMap.SimpleEntry<>("%builders%", buildWorld.getBuildersInfo()),
                new AbstractMap.SimpleEntry<>("%block_breaking%", buildWorld.isBlockPlacement()),
                new AbstractMap.SimpleEntry<>("%block_placement%", buildWorld.getMaterial().name()),
                new AbstractMap.SimpleEntry<>("%status%", buildWorld.getStatus().getName()),
                new AbstractMap.SimpleEntry<>("%project%", buildWorld.getProject()),
                new AbstractMap.SimpleEntry<>("%permission%", buildWorld.getPermission()),
                new AbstractMap.SimpleEntry<>("%time%", buildWorld.getWorld()),
                new AbstractMap.SimpleEntry<>("%creation%", buildWorld.getFormattedCreationDate()),
                new AbstractMap.SimpleEntry<>("%date%", buildWorld.getFormattedCreationDate()),
                new AbstractMap.SimpleEntry<>("%physics%", buildWorld.isPhysics()),
                new AbstractMap.SimpleEntry<>("%explosions%", buildWorld.isExplosions()),
                new AbstractMap.SimpleEntry<>("%mobai%", buildWorld.isMobAI()),
                new AbstractMap.SimpleEntry<>("%custom_spawn%", getCustomSpawn(buildWorld))
        );
    }

    private String getCustomSpawn(BuildWorld buildWorld) {
        if (buildWorld.getCustomSpawn() == null) {
            return "-";
        }

        String[] spawnString = buildWorld.getCustomSpawn().split(";");
        Location location = new Location(
                Bukkit.getWorld(buildWorld.getName()),
                Double.parseDouble(spawnString[0]),
                Double.parseDouble(spawnString[1]),
                Double.parseDouble(spawnString[2]),
                Float.parseFloat(spawnString[3]),
                Float.parseFloat(spawnString[4])
        );

        return "XYZ: " + round(location.getX()) + " / " + round(location.getY()) + " / " + round(location.getZ());
    }

    private double round(double value) {
        int scale = (int) Math.pow(10, 2);
        return (double) Math.round(value * scale) / scale;
    }
}