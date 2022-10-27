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
import com.eintosti.buildsystem.object.world.generator.Generator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;

/**
 * @author einTosti
 */
public class ImportSubCommand implements SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public ImportSubCommand(BuildSystem plugin, String worldName) {
        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!player.hasPermission("buildsystem.import")) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getString("worlds_import_usage"));
            return;
        }

        WorldManager worldManager = plugin.getWorldManager();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld != null) {
            player.sendMessage(plugin.getString("worlds_import_world_is_imported"));
            return;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), args[1]);
        File levelFile = new File(worldFolder.getAbsolutePath() + File.separator + "level.dat");
        if (!worldFolder.isDirectory() || !levelFile.exists()) {
            player.sendMessage(plugin.getString("worlds_import_unknown_world"));
            return;
        }

        switch (args.length) {
            case 2:
                worldManager.importWorld(player, args[1], Generator.VOID);
                break;
            case 4:
                if (!args[2].equalsIgnoreCase("-g")) {
                    player.sendMessage(plugin.getString("worlds_import_usage"));
                    return;
                }

                Generator generator;
                try {
                    generator = Generator.valueOf(args[3].toUpperCase());
                } catch (IllegalArgumentException e) {
                    generator = Generator.CUSTOM;
                }
                worldManager.importWorld(player, args[1], generator, args[3]);
                break;
            default:
                player.sendMessage(plugin.getString("worlds_import_usage"));
                break;
        }
    }
}