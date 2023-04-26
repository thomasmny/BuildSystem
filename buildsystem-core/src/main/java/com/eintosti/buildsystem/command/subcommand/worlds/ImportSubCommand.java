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
import com.eintosti.buildsystem.util.ArgumentParser;
import com.eintosti.buildsystem.util.UUIDFetcher;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.Builder;
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.generator.Generator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.AbstractMap;
import java.util.UUID;

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

        for (String charString : worldName.split("")) {
            if (charString.matches("[^A-Za-z\\d/_-]")) {
                Messages.sendMessage(player, "worlds_import_invalid_character",
                        new AbstractMap.SimpleEntry<>("%world%", worldName),
                        new AbstractMap.SimpleEntry<>("%char%", charString)
                );
                return;
            }
        }

        Generator generator = Generator.VOID;
        Builder builder = new Builder(null, "-");

        if (args.length == 2) {
            worldManager.importWorld(player, args[1], builder, generator, null, true);
            return;
        }

        ArgumentParser parser = new ArgumentParser(args);

        if (parser.isArgument("g")) {
            String generatorArg = parser.getValue("g");
            if (generatorArg == null) {
                Messages.sendMessage(player, "worlds_import_usage");
                return;
            }
            try {
                generator = Generator.valueOf(generatorArg.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (parser.isArgument("c")) {
            String creatorArg = parser.getValue("c");
            if (creatorArg == null) {
                Messages.sendMessage(player, "worlds_import_usage");
                return;
            }
            UUID creatorId = UUIDFetcher.getUUID(creatorArg);
            if (creatorId == null) {
                Messages.sendMessage(player, "worlds_import_player_not_found");
                return;
            }
            builder = new Builder(creatorId, creatorArg);
        }

        worldManager.importWorld(player, args[1], builder, generator, args[3], true);
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.IMPORT;
    }
}