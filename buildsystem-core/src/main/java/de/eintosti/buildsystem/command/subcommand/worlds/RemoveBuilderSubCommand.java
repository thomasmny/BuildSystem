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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldManager;
import java.util.AbstractMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RemoveBuilderSubCommand implements SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public RemoveBuilderSubCommand(BuildSystem plugin, String worldName) {
        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        WorldManager worldManager = plugin.getWorldManager();
        if (!worldManager.isPermitted(player, getArgument().getPermission(), worldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_removebuilder_unknown_world");
            return;
        }

        switch (args.length) {
            case 1:
                getRemoveBuilderInput(player, buildWorld);
                break;
            case 2:
                removeBuilder(player, buildWorld, args[1]);
                break;
            default:
                Messages.sendMessage(player, "worlds_removebuilder_usage");
                break;
        }
    }

    private void removeBuilder(Player player, BuildWorld buildWorld, String builderName) {
        Player builderPlayer = Bukkit.getPlayerExact(builderName);
        UUID builderId;

        if (builderPlayer == null) {
            builderId = UUIDFetcher.getUUID(builderName);
            if (builderId == null) {
                Messages.sendMessage(player, "worlds_removebuilder_player_not_found");
                player.closeInventory();
                return;
            }
        } else {
            builderId = builderPlayer.getUniqueId();
        }

        if (builderId.equals(player.getUniqueId()) && buildWorld.isCreator(player)) {
            Messages.sendMessage(player, "worlds_removebuilder_not_yourself");
            player.closeInventory();
            return;
        }

        if (!buildWorld.isBuilder(builderId)) {
            Messages.sendMessage(player, "worlds_removebuilder_not_builder");
            player.closeInventory();
            return;
        }

        buildWorld.removeBuilder(builderId);
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
        Messages.sendMessage(player, "worlds_removebuilder_removed", new AbstractMap.SimpleEntry<>("%builder%", builderName));

        player.closeInventory();
    }

    private void getRemoveBuilderInput(Player player, BuildWorld buildWorld) {
        new PlayerChatInput(plugin, player, "enter_player_name", input -> {
            String builderName = input.trim();
            removeBuilder(player, buildWorld, builderName);
        });
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.REMOVE_BUILDER;
    }
}