/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.command.subcommand.worlds;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.generator.Generator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.tabcomplete.WorldsTabCompleter.WorldsArgument;
import de.eintosti.buildsystem.util.ArgumentParser;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ImportSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;
    private final String worldName;

    public ImportSubCommand(BuildSystemPlugin plugin, String worldName) {
        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!hasPermission(player)) {
            Messages.sendPermissionError(player);
            return;
        }

        if (args.length < 2) {
            Messages.sendMessage(player, "worlds_import_usage");
            return;
        }

        WorldServiceImpl worldService = plugin.getWorldService();
        if (worldService.getWorldStorage().worldExists(worldName)) {
            Messages.sendMessage(player, "worlds_import_world_is_imported");
            return;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), args[1]);
        File levelFile = new File(worldFolder, "level.dat");
        if (!worldFolder.isDirectory() || !levelFile.exists()) {
            Messages.sendMessage(player, "worlds_import_unknown_world");
            return;
        }

        String invalidChar = StringCleaner.firstInvalidChar(worldName);
        if (invalidChar != null) {
            Messages.sendMessage(player, "worlds_import_invalid_character",
                    Map.entry("%world%", worldName),
                    Map.entry("%char%", invalidChar)
            );
            return;
        }

        Builder creator = null;
        Generator generator = Generator.VOID;
        String generatorName = generator.name();
        BuildWorldType worldType = BuildWorldType.IMPORTED;

        if (args.length != 2) {
            ArgumentParser parser = new ArgumentParser(args);

            if (parser.isArgument("g")) {
                String generatorArg = parser.getValue("g");
                if (generatorArg == null) {
                    Messages.sendMessage(player, "worlds_import_usage");
                    return;
                }
                try {
                    generator = Generator.valueOf(generatorArg.toUpperCase(Locale.ROOT));
                    generatorName = generator.name();
                } catch (IllegalArgumentException ignored) {
                    generator = Generator.CUSTOM;
                    generatorName = generatorArg;
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
                creator = Builder.of(creatorId, creatorArg);
            }

            if (parser.isArgument("t")) {
                String worldTypeArg = parser.getValue("t");
                if (worldTypeArg == null) {
                    Messages.sendMessage(player, "worlds_import_usage");
                    return;
                }
                try {
                    worldType = BuildWorldType.valueOf(worldTypeArg.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        Messages.sendMessage(player, "worlds_import_started",
                Map.entry("%world%", worldName)
        );
        if (worldService.importWorld(player, worldName, creator, worldType, generator, generatorName, true)) {
            Messages.sendMessage(player, "worlds_import_finished");
        }
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.IMPORT;
    }
}