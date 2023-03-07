/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.command.subcommand.worlds;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.command.subcommand.Argument;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.generator.Generator;
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
        if (!hasPermission(player)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length < 2) {
            Messages.sendMessage(player, "worlds_import_usage");
            return;
        }

        WorldManager worldManager = plugin.getWorldManager();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld != null) {
            Messages.sendMessage(player, "worlds_import_world_is_imported");
            return;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), args[1]);
        File levelFile = new File(worldFolder.getAbsolutePath() + File.separator + "level.dat");
        if (!worldFolder.isDirectory() || !levelFile.exists()) {
            Messages.sendMessage(player, "worlds_import_unknown_world");
            return;
        }

        switch (args.length) {
            case 2:
                worldManager.importWorld(player, args[1], Generator.VOID, null);
                break;
            case 4:
                if (!args[2].equalsIgnoreCase("-g")) {
                    Messages.sendMessage(player, "worlds_import_usage");
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
                Messages.sendMessage(player, "worlds_import_usage");
                break;
        }
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.IMPORT;
    }
}