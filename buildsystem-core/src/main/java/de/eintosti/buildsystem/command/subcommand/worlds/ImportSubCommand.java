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

import static java.util.Map.entry;

import com.google.common.collect.Lists;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.generator.Generator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.util.ArgumentParser;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ImportSubCommand extends AbstractSubCommand {

    private final ConfigService configService;
    private final PlayerLookupService playerLookupService;
    private final TaskScheduler scheduler;

    public ImportSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            ConfigService configService,
            PlayerLookupService playerLookupService,
            TaskScheduler scheduler) {
        super(messages, worldService);
        this.configService = configService;
        this.playerLookupService = playerLookupService;
        this.scheduler = scheduler;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        if (!hasPermission(player)) {
            messages.sendPermissionError(player);
            return;
        }

        if (args.length < 2) {
            messages.sendMessage(player, "worlds_import_usage");
            return;
        }

        if (worldService.getWorldStorage().worldExists(worldName)) {
            messages.sendMessage(player, "worlds_import_world_is_imported");
            return;
        }

        // Validate the name before touching the filesystem so invalid input cannot probe directory existence
        String invalidChar = StringCleaner.firstInvalidChar(
                worldName, configService.current().world().invalidCharacters());
        if (invalidChar != null) {
            messages.sendMessage(
                    player,
                    "worlds_import_invalid_character",
                    Map.entry("%world%", worldName),
                    Map.entry("%char%", invalidChar));
            return;
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), args[1]);
        File levelFile = new File(worldFolder, "level.dat");
        if (StringCleaner.isPathEscape(Bukkit.getWorldContainer(), worldFolder)
                || !worldFolder.isDirectory()
                || !levelFile.exists()) {
            messages.sendMessage(player, "worlds_import_unknown_world");
            return;
        }

        Generator generator = Generator.VOID;
        String generatorName = generator.name();
        BuildWorldType worldType = BuildWorldType.IMPORTED;
        String creatorArg = null;

        if (args.length != 2) {
            ArgumentParser parser = new ArgumentParser(args);

            if (parser.isArgument("g")) {
                String generatorArg = parser.getValue("g");
                if (generatorArg == null) {
                    messages.sendMessage(player, "worlds_import_usage");
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
                creatorArg = parser.getValue("c");
                if (creatorArg == null) {
                    messages.sendMessage(player, "worlds_import_usage");
                    return;
                }
            }

            if (parser.isArgument("t")) {
                String worldTypeArg = parser.getValue("t");
                if (worldTypeArg == null) {
                    messages.sendMessage(player, "worlds_import_usage");
                    return;
                }
                try {
                    worldType = BuildWorldType.valueOf(worldTypeArg.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (creatorArg == null) {
            startImport(player, worldName, null, worldType, generator, generatorName);
            return;
        }

        String creatorName = creatorArg;
        Generator resolvedGenerator = generator;
        String resolvedGeneratorName = generatorName;
        BuildWorldType resolvedWorldType = worldType;
        playerLookupService
                .lookupUniqueId(creatorName)
                .thenAccept(creatorId -> scheduler.run(() -> {
                    if (creatorId == null) {
                        messages.sendMessage(player, "worlds_import_player_not_found");
                        return;
                    }
                    startImport(
                            player,
                            worldName,
                            Builder.of(creatorId, creatorName),
                            resolvedWorldType,
                            resolvedGenerator,
                            resolvedGeneratorName);
                }));
    }

    private void startImport(
            Player player,
            String worldName,
            @Nullable Builder creator,
            BuildWorldType worldType,
            Generator generator,
            String generatorName) {
        messages.sendMessage(player, "worlds_import_started", Map.entry("%world%", worldName));
        if (worldService.importWorld(player, worldName, creator, worldType, generator, generatorName, true)) {
            messages.sendMessage(player, "worlds_import_finished");
        }
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 2) {
            String[] directories = Bukkit.getWorldContainer().list((dir, name) -> {
                if (StringCleaner.hasInvalidNameCharacters(
                        name, configService.current().world().invalidCharacters())) {
                    return false;
                }
                File worldFolder = new File(dir, name);
                if (!worldFolder.isDirectory()) {
                    return false;
                }
                if (!new File(worldFolder, "level.dat").exists()) {
                    return false;
                }
                return !worldService.getWorldStorage().worldExists(name);
            });
            if (directories != null) {
                for (String dir : directories) {
                    WorldsCompletions.addIfStartsWith(args[1], dir, result);
                }
            }
            return result;
        }

        Map<String, List<String>> flags = Map.ofEntries(
                entry(
                        "-g",
                        Arrays.stream(Generator.values())
                                .filter(g -> g != Generator.CUSTOM)
                                .map(Enum::name)
                                .toList()),
                entry("-c", List.of()),
                entry(
                        "-t",
                        Arrays.stream(BuildWorldType.values()).map(Enum::name).toList()));

        if (args.length % 2 == 1) {
            flags.keySet().stream()
                    .filter(key -> !Lists.newArrayList(args).contains(key))
                    .forEach(key -> WorldsCompletions.addIfStartsWith(args[args.length - 1], key, result));
        } else {
            List<String> values = flags.get(args[args.length - 2]);
            if (values != null) {
                for (String v : values) {
                    WorldsCompletions.addIfStartsWith(args[args.length - 1], v, result);
                }
            }
        }
        return result;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.IMPORT;
    }
}
