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
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.util.WorldPermissions;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.world.builder.BuilderInventory;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class AddBuilderSubCommand implements SubCommand {

    private final BuildSystemPlugin plugin;

    @Nullable
    private final BuildWorld buildWorld;
    private final WorldPermissions permissions;

    public AddBuilderSubCommand(BuildSystemPlugin plugin, String worldName) {
        this.plugin = plugin;
        this.buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(worldName);
        this.permissions = WorldPermissionsImpl.of(buildWorld);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!permissions.canPerformCommand(player, getArgument().getPermission())) {
            Messages.sendPermissionError(player);
            return;
        }

        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_addbuilder_unknown_world");
            return;
        }

        switch (args.length) {
            case 1 -> getAddBuilderInput(player, buildWorld, true);
            case 2 -> addBuilder(player, buildWorld, args[1], true);
            default -> Messages.sendMessage(player, "worlds_addbuilder_usage");
        }
    }

    private void addBuilder(Player player, BuildWorld buildWorld, String builderName, boolean closeInventory) {
        Player builderPlayer = Bukkit.getPlayerExact(builderName);
        Builder builder;
        UUID builderId;

        if (builderPlayer == null) {
            builderId = UUIDFetcher.getUUID(builderName);
            if (builderId == null) {
                Messages.sendMessage(player, "worlds_addbuilder_player_not_found");
                player.closeInventory();
                return;
            }
            builder = Builder.of(builderId, builderName);
        } else {
            builder = Builder.of(builderPlayer);
            builderId = builderPlayer.getUniqueId();
        }

        Builders builders = buildWorld.getBuilders();
        if (builderId.equals(player.getUniqueId()) && builders.isCreator(player)) {
            Messages.sendMessage(player, "worlds_addbuilder_already_creator");
            player.closeInventory();
            return;
        }

        if (builders.isBuilder(builderId)) {
            Messages.sendMessage(player, "worlds_addbuilder_already_added");
            player.closeInventory();
            return;
        }

        builders.addBuilder(builder);
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
        Messages.sendMessage(player, "worlds_addbuilder_added",
                Map.entry("%builder%", builderName)
        );

        if (closeInventory) {
            player.closeInventory();
        } else {
            new BuilderInventory(plugin).openInventory(buildWorld, player);
        }
    }

    public void getAddBuilderInput(Player player, BuildWorld buildWorld, boolean closeInventory) {
        new PlayerChatInput(plugin, player, "enter_player_name", input -> {
            String builderName = input.trim();
            addBuilder(player, buildWorld, builderName, closeInventory);
        });
    }

    @Override
    public Argument getArgument() {
        return WorldsTabComplete.WorldsArgument.ADD_BUILDER;
    }
}