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

import de.eintosti.buildsystem.api.world.creation.generator.Generator;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.util.ArgumentParser;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ImportAllSubCommand extends AbstractSubCommand {

    private final PlayerLookupService playerLookupService;

    public ImportAllSubCommand(
            Messages messages, WorldServiceImpl worldService, PlayerLookupService playerLookupService) {
        super(messages, worldService);
        this.playerLookupService = playerLookupService;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        if (!hasPermission(player)) {
            messages.sendPermissionError(player);
            return;
        }

        if (!isValidShape(args)) {
            messages.sendMessage(player, "worlds_importall_usage");
            return;
        }

        if (worldService.isImportingAllWorlds()) {
            messages.sendMessage(player, "worlds_importall_already_started");
            return;
        }

        String[] directories =
                worldService.getWorldStorage().unimportedWorldNames().toArray(String[]::new);

        if (directories.length == 0) {
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
            } catch (IllegalArgumentException e) {
                // Unlike /worlds import, a bulk import has no per-world generator data, so an unknown name cannot be
                // taken as a custom generator; silently falling back to VOID imported every world with the wrong one.
                messages.sendMessage(player, "worlds_import_unknown_generator");
                return;
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

        Generator resolvedGenerator = generator;
        resolvePlayer(
                playerLookupService,
                player,
                creatorArg,
                "worlds_importall_player_not_found",
                creator -> worldService.importWorlds(player, directories, resolvedGenerator, creator));
    }

    /**
     * {@return whether the arguments are {@code importAll} followed by at most one {@code -g <generator>} and at most
     * one {@code -c <creator>}, in either order}
     */
    private static boolean isValidShape(String[] args) {
        if (args.length > 5 || args.length % 2 == 0) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        for (int i = 1; i < args.length; i += 2) {
            String flag = args[i].toLowerCase(Locale.ROOT);
            if (!(flag.equals("-g") || flag.equals("-c")) || !seen.add(flag) || args[i + 1].startsWith("-")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.IMPORT_ALL;
    }
}
