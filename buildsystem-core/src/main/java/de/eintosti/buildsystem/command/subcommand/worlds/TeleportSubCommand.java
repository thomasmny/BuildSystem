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

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeleportSubCommand implements SubCommand {

    private final BuildSystem plugin;

    public TeleportSubCommand(BuildSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!hasPermission(player)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length != 2) {
            Messages.sendMessage(player, "worlds_tp_usage");
            return;
        }

        WorldManager worldManager = plugin.getWorldManager();
        BuildWorld buildWorld = worldManager.getBuildWorld(args[1]);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_tp_unknown_world");
            return;
        }

        World bukkitWorld = Bukkit.getServer().getWorld(args[1]);
        if (buildWorld.isLoaded() && bukkitWorld == null) {
            Messages.sendMessage(player, "worlds_tp_unknown_world");
            return;
        }

        if (!worldManager.canEnter(player, buildWorld)) {
            Messages.sendMessage(player, "worlds_tp_entry_forbidden");
            return;
        }

        worldManager.teleport(player, buildWorld);
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.TP;
    }
}