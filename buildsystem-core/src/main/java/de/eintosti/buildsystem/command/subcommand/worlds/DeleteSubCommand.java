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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DeleteSubCommand extends AbstractSubCommand {

    public DeleteSubCommand(BuildSystemPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = requireWorld(player, worldName, args, 2, "worlds_delete");
        if (buildWorld == null) {
            return;
        }

        if (plugin.getConfigService()
                .current()
                .world()
                .deletionBlacklist()
                .contains(buildWorld.getName().toLowerCase())) {
            messages.sendMessage(player, "worlds_delete_forbidden");
            return;
        }

        plugin.getMenus().openDelete(buildWorld, player);
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        return WorldsCompletions.deletableWorldNames(
                player, plugin.getWorldService().getWorldStorage(), args[1]);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.DELETE;
    }
}
