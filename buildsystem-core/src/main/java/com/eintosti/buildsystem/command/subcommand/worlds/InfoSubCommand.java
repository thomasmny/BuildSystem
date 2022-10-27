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
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
        if (args.length == 2) {
            if (buildWorld == null) {
                player.sendMessage(plugin.getString("worlds_info_unknown_world"));
                return;
            }
            buildWorld = worldManager.getBuildWorld(args[1]);
        } else {
            player.sendMessage(plugin.getString("worlds_info_usage"));
        }

        sendInfoMessage(player, buildWorld);
    }

    //TODO: Print information about the custom generator?
    private void sendInfoMessage(Player player, BuildWorld buildWorld) {
        List<String> infoMessage = new ArrayList<>();
        for (String line : plugin.getStringList("world_info")) {
            infoMessage.add(line
                    .replace("%world%", buildWorld.getName())
                    .replace("%creator%", buildWorld.getCreator())
                    .replace("%type%", buildWorld.getType().getName())
                    .replace("%private%", String.valueOf(buildWorld.isPrivate()))
                    .replace("%builders_enabled%", String.valueOf(buildWorld.isBuilders()))
                    .replace("%builders%", buildWorld.getBuildersInfo())
                    .replace("%block_breaking%", String.valueOf(buildWorld.isBlockBreaking()))
                    .replace("%block_placement%", String.valueOf(buildWorld.isBlockPlacement()))
                    .replace("%item%", buildWorld.getMaterial().name())
                    .replace("%status%", buildWorld.getStatus().getName())
                    .replace("%project%", buildWorld.getProject())
                    .replace("%permission%", buildWorld.getPermission())
                    .replace("%time%", buildWorld.getWorldTime())
                    .replace("%creation%", buildWorld.getFormattedCreationDate())
                    .replace("%date%", buildWorld.getFormattedCreationDate())
                    .replace("%physics%", String.valueOf(buildWorld.isPhysics()))
                    .replace("%explosions%", String.valueOf(buildWorld.isExplosions()))
                    .replace("%mobai%", String.valueOf(buildWorld.isMobAI()))
                    .replace("%custom_spawn%", getCustomSpawn(buildWorld))
            );
        }
        StringBuilder stringBuilder = new StringBuilder();
        infoMessage.forEach(line -> stringBuilder.append(line).append("\n"));
        player.sendMessage(stringBuilder.toString());
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