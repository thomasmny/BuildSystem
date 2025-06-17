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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.AbstractMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SetSpawnSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;

    public SetSpawnSubCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        BuildWorld buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(player.getWorld());
        if (!WorldPermissionsImpl.of(buildWorld).canPerformCommand(player, getArgument().getPermission())) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_setspawn_world_not_imported");
            return;
        }

        Location playerLocation = player.getLocation();
        buildWorld.getData().customSpawn().set(String.format("%s;%s;%s;%s;%s",
                playerLocation.getX(), playerLocation.getY(), playerLocation.getZ(), playerLocation.getYaw(), playerLocation.getPitch()
        ));
        Messages.sendMessage(player, "worlds_setspawn_world_spawn_set",
                new AbstractMap.SimpleEntry<>("%world%", buildWorld.getName())
        );
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.SET_SPAWN;
    }
}