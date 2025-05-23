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
import java.util.AbstractMap;
import org.bukkit.entity.Player;

public class RemoveSpawnSubCommand implements SubCommand {

    private final BuildSystem plugin;

    public RemoveSpawnSubCommand(BuildSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        WorldManager worldManager = plugin.getWorldManager();
        String playerWorldName = player.getWorld().getName();
        if (!worldManager.isPermitted(player, getArgument().getPermission(), playerWorldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_removespawn_world_not_imported");
            return;
        }

        buildWorld.getData().customSpawn().set(null);
        Messages.sendMessage(player, "worlds_removespawn_world_spawn_removed",
                new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName())
        );
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.REMOVE_SPAWN;
    }
}