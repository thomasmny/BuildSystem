/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.tabcomplete.WorldsTabCompleter.WorldsArgument;
import de.eintosti.buildsystem.world.navigator.inventory.ArchivedWorldsInventory;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ArchiveSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;

    public ArchiveSubCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!hasPermission(player)) {
            Messages.sendPermissionError(player);
            return;
        }

        new ArchivedWorldsInventory(plugin, player).openInventory();
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.ARCHIVE;
    }
}