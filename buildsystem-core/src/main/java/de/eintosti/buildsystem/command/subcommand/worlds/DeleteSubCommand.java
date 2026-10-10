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
import de.eintosti.buildsystem.command.Completions;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.WorldSubCommand;
import de.eintosti.buildsystem.command.subcommand.WorldTarget;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DeleteSubCommand extends WorldSubCommand {

    private final ConfigService configService;
    private final Menus menus;

    public DeleteSubCommand(
            Messages messages, WorldServiceImpl worldService, ConfigService configService, Menus menus) {
        super(messages, worldService, WorldTarget.argument(2, "worlds_delete"));
        this.configService = configService;
        this.menus = menus;
    }

    @Override
    protected void execute(Player player, BuildWorld buildWorld, String[] args) {
        if (configService.current().world().deletionBlacklist().contains(WorldNames.id(buildWorld.getName()))) {
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

        // Offers only the worlds /worlds delete would accept: permitted and not on the deletion blacklist.
        Set<String> blacklist = configService.current().world().deletionBlacklist();
        List<String> result = new ArrayList<>();
        Completions.addWorldNames(
                args[1],
                worldService.getWorldStorage(),
                world -> !blacklist.contains(WorldNames.id(world.getName()))
                        && world.getPermissions().canPerformCommand(player, Permissions.DELETE),
                result);
        return result;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.DELETE;
    }
}
