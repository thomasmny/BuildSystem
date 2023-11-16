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
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import de.eintosti.buildsystem.world.data.WorldData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.AbstractMap;

public class InfoSubCommand implements SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public InfoSubCommand(BuildSystem plugin, String worldName) {
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
            Messages.sendMessage(player, "worlds_info_usage");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_info_unknown_world");
            return;
        }

        //TODO: Print information about the custom generator?
        WorldData worldData = buildWorld.getData();
        Messages.sendMessage(player, "world_info",
                new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName()),
                new AbstractMap.SimpleEntry<>("%creator%", buildWorld.getCreator()),
                new AbstractMap.SimpleEntry<>("%item%", worldData.material().get().name()),
                new AbstractMap.SimpleEntry<>("%type%", buildWorld.getType().getName(player)),
                new AbstractMap.SimpleEntry<>("%private%", worldData.privateWorld().get()),
                new AbstractMap.SimpleEntry<>("%builders_enabled%", worldData.buildersEnabled().get()),
                new AbstractMap.SimpleEntry<>("%builders%", buildWorld.getBuildersInfo(player)),
                new AbstractMap.SimpleEntry<>("%block_breaking%", worldData.blockBreaking().get()),
                new AbstractMap.SimpleEntry<>("%block_placement%", worldData.blockPlacement().get()),
                new AbstractMap.SimpleEntry<>("%status%", worldData.status().get().getName(player)),
                new AbstractMap.SimpleEntry<>("%project%", worldData.project().get()),
                new AbstractMap.SimpleEntry<>("%permission%", worldData.permission().get()),
                new AbstractMap.SimpleEntry<>("%time%", buildWorld.getWorldTime()),
                new AbstractMap.SimpleEntry<>("%creation%", Messages.formatDate(buildWorld.getCreationDate())),
                new AbstractMap.SimpleEntry<>("%physics%", worldData.physics().get()),
                new AbstractMap.SimpleEntry<>("%explosions%", worldData.explosions().get()),
                new AbstractMap.SimpleEntry<>("%mobai%", worldData.mobAi().get()),
                new AbstractMap.SimpleEntry<>("%custom_spawn%", getCustomSpawn(buildWorld)),
                new AbstractMap.SimpleEntry<>("%lastedited%", Messages.formatDate(worldData.lastEdited().get())),
                new AbstractMap.SimpleEntry<>("%lastloaded%", Messages.formatDate(worldData.lastLoaded().get())),
                new AbstractMap.SimpleEntry<>("%lastunloaded%", Messages.formatDate(worldData.lastUnloaded().get()))
        );
    }

    private String getCustomSpawn(BuildWorld buildWorld) {
        WorldData worldData = buildWorld.getData();
        Location spawn = worldData.getCustomSpawnLocation();
        if (spawn == null) {
            return "-";
        }
        return "XYZ: " + round(spawn.getX()) + " / " + round(spawn.getY()) + " / " + round(spawn.getZ());
    }

    private double round(double value) {
        int scale = (int) Math.pow(10, 2);
        return (double) Math.round(value * scale) / scale;
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.INFO;
    }
}