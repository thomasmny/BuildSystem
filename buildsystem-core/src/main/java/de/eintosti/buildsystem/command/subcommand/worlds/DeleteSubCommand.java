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

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DeleteSubCommand extends AbstractSubCommand {

    private final ConfigService configService;
    private final Menus menus;

    public DeleteSubCommand(
            Messages messages, WorldServiceImpl worldService, ConfigService configService, Menus menus) {
        super(messages, worldService);
        this.configService = configService;
        this.menus = menus;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = requireWorld(player, worldName, args, 2, "worlds_delete");
        if (buildWorld == null) {
            return;
        }

        if (configService
                .current()
                .world()
                .deletionBlacklist()
                .contains(buildWorld.getName().toLowerCase())) {
            messages.sendMessage(player, "worlds_delete_forbidden");
            return;
        }

        menus.openDelete(buildWorld, player);
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        return WorldsCompletions.deletableWorldNames(player, worldService.getWorldStorage(), args[1]);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.DELETE;
    }
}
