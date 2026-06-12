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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.subcommand.worlds.WorldsArgument;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import java.util.List;
import java.util.ArrayList;

@NullMarked
public class RemoveBuilderSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;

    public RemoveBuilderSubCommand(BuildSystemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(player.getWorld().getName());
        var permissions = WorldPermissionsImpl.of(buildWorld);
        if (!permissions.canPerformCommand(player, getArgument().getPermission())) {
            plugin.getMessages().sendPermissionError(player);
            return;
        }

        if (buildWorld == null) {
            plugin.getMessages().sendMessage(player, "worlds_removebuilder_unknown_world");
            return;
        }

        switch (args.length) {
            case 1 -> getRemoveBuilderInput(player, buildWorld);
            case 2 -> removeBuilder(player, buildWorld, args[1]);
            default -> plugin.getMessages().sendMessage(player, "worlds_removebuilder_usage");
        }
    }

    private void removeBuilder(Player player, BuildWorld buildWorld, String builderName) {
        Player builderPlayer = Bukkit.getPlayerExact(builderName);
        UUID builderId;

        if (builderPlayer == null) {
            builderId = UUIDFetcher.getUUID(builderName);
            if (builderId == null) {
                plugin.getMessages().sendMessage(player, "worlds_removebuilder_player_not_found");
                player.closeInventory();
                return;
            }
        } else {
            builderId = builderPlayer.getUniqueId();
        }

        Builders builders = buildWorld.getBuilders();
        if (builderId.equals(player.getUniqueId()) && builders.isCreator(player)) {
            plugin.getMessages().sendMessage(player, "worlds_removebuilder_not_yourself");
            player.closeInventory();
            return;
        }

        if (!builders.isBuilder(builderId)) {
            plugin.getMessages().sendMessage(player, "worlds_removebuilder_not_builder");
            player.closeInventory();
            return;
        }

        builders.removeBuilder(builderId);
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
        plugin.getMessages().sendMessage(player, "worlds_removebuilder_removed", Map.entry("%builder%", builderName));

        player.closeInventory();
    }

    private void getRemoveBuilderInput(Player player, BuildWorld buildWorld) {
        new PlayerChatInput(plugin, player, "enter_player_name", input -> {
            String builderName = input.trim();
            removeBuilder(player, buildWorld, builderName);
        });
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) return List.of();
        BuildWorld buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) return List.of();
        Builders builders = buildWorld.getBuilders();
        if (!builders.isCreator(player)) return List.of();
        List<String> result = new ArrayList<>();
        builders.getBuilderNames().forEach(name -> WorldsCompletions.addIfStartsWith(args[1], name, result));
        return result;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.REMOVE_BUILDER;
    }
}