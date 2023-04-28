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
import com.eintosti.buildsystem.world.Builder;
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.generator.Generator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author einTosti
 */
public class ImportAllSubCommand implements SubCommand {

    private final BuildSystem plugin;

    public ImportAllSubCommand(BuildSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!hasPermission(player)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length != 1) {
            Messages.sendMessage(player, "worlds_importall_usage");
            return;
        }

        WorldManager worldManager = plugin.getWorldManager();
        if (worldManager.isImportingAllWorlds()) {
            Messages.sendMessage(player, "worlds_importall_already_started");
            return;
        }

        File worldContainer = Bukkit.getWorldContainer();
        String[] directories = worldContainer.list((dir, name) -> {
            File worldFolder = new File(dir, name);
            if (!worldFolder.isDirectory()) {
                return false;
            }

            if (!new File(worldFolder, "level.dat").exists()) {
                return false;
            }

            return worldManager.getBuildWorld(name) == null;
        });

        if (directories == null || directories.length == 0) {
            Messages.sendMessage(player, "worlds_importall_no_worlds");
            return;
        }

        ArgumentParser parser = new ArgumentParser(args);
        Generator generator = Generator.VOID;
        Builder builder = new Builder(null, "-");

        if (parser.isArgument("g")) {
            String generatorArg = parser.getValue("g");
            if (generatorArg == null) {
                Messages.sendMessage(player, "worlds_importall_usage");
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
                Messages.sendMessage(player, "worlds_importall_usage");
                return;
            }
            UUID creatorId = UUIDFetcher.getUUID(creatorArg);
            if (creatorId == null) {
                Messages.sendMessage(player, "worlds_importall_player_not_found");
                return;
            }
            builder = new Builder(creatorId, creatorArg);
        }

        worldManager.importWorlds(player, directories, generator, builder);
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.IMPORT_ALL;
    }
}