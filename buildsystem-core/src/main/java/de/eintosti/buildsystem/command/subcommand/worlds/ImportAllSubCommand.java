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

import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.generator.Generator;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.util.ArgumentParser;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.io.File;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ImportAllSubCommand extends AbstractSubCommand {

    private final PlayerLookupService playerLookupService;
    private final TaskScheduler scheduler;

    public ImportAllSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            PlayerLookupService playerLookupService,
            TaskScheduler scheduler) {
        super(messages, worldService);
        this.playerLookupService = playerLookupService;
        this.scheduler = scheduler;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        if (!hasPermission(player)) {
            messages.sendPermissionError(player);
            return;
        }

        if (args.length != 1) {
            messages.sendMessage(player, "worlds_importall_usage");
            return;
        }

        if (worldService.isImportingAllWorlds()) {
            messages.sendMessage(player, "worlds_importall_already_started");
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

            return !worldService.getWorldStorage().worldExists(name);
        });

        if (directories == null || directories.length == 0) {
            messages.sendMessage(player, "worlds_importall_no_worlds");
            return;
        }

        ArgumentParser parser = new ArgumentParser(args);
        Generator generator = Generator.VOID;
        String creatorArg = null;

        if (parser.isArgument("g")) {
            String generatorArg = parser.getValue("g");
            if (generatorArg == null) {
                messages.sendMessage(player, "worlds_importall_usage");
                return;
            }
            try {
                generator = Generator.valueOf(generatorArg.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (parser.isArgument("c")) {
            creatorArg = parser.getValue("c");
            if (creatorArg == null) {
                messages.sendMessage(player, "worlds_importall_usage");
                return;
            }
        }

        if (creatorArg == null) {
            worldService.importWorlds(player, directories, generator, null);
            return;
        }

        String creatorName = creatorArg;
        Generator resolvedGenerator = generator;
        playerLookupService
                .lookupUniqueId(creatorName)
                .thenAccept(creatorId -> scheduler.run(() -> {
                    if (creatorId == null) {
                        messages.sendMessage(player, "worlds_importall_player_not_found");
                        return;
                    }
                    worldService.importWorlds(
                            player, directories, resolvedGenerator, Builder.of(creatorId, creatorName));
                }));
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.IMPORT_ALL;
    }
}
