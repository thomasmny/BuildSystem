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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class RemoveBuilderSubCommand extends AbstractSubCommand {

    private final PlayerLookupService playerLookupService;
    private final Prompts prompts;
    private final TaskScheduler scheduler;

    public RemoveBuilderSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            PlayerLookupService playerLookupService,
            Prompts prompts,
            TaskScheduler scheduler) {
        super(messages, worldService);
        this.playerLookupService = playerLookupService;
        this.prompts = prompts;
        this.scheduler = scheduler;
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld =
                worldService.getWorldStorage().getBuildWorld(player.getWorld().getName());
        if (buildWorld != null
                && !buildWorld
                        .getPermissions()
                        .canPerformCommand(player, getArgument().getPermission())) {
            messages.sendPermissionError(player);
            return;
        }

        if (buildWorld == null) {
            messages.sendMessage(player, "worlds_removebuilder_unknown_world");
            return;
        }

        switch (args.length) {
            case 1 -> getRemoveBuilderInput(player, buildWorld);
            case 2 -> removeBuilder(player, buildWorld, args[1]);
            default -> messages.sendMessage(player, "worlds_removebuilder_usage");
        }
    }

    private void removeBuilder(Player player, BuildWorld buildWorld, String builderName) {
        Player builderPlayer = Bukkit.getPlayerExact(builderName);
        if (builderPlayer != null) {
            applyRemove(player, buildWorld, builderPlayer.getUniqueId(), builderName);
            return;
        }

        playerLookupService
                .lookupUniqueId(builderName)
                .thenAccept(builderId -> scheduler.run(() -> {
                    if (builderId == null) {
                        messages.sendMessage(player, "worlds_removebuilder_player_not_found");
                        player.closeInventory();
                        return;
                    }
                    applyRemove(player, buildWorld, builderId, builderName);
                }));
    }

    private void applyRemove(Player player, BuildWorld buildWorld, UUID builderId, String builderName) {
        Builders builders = buildWorld.getBuilders();
        if (builderId.equals(player.getUniqueId()) && builders.isCreator(player)) {
            messages.sendMessage(player, "worlds_removebuilder_not_yourself");
            player.closeInventory();
            return;
        }

        if (!builders.isBuilder(builderId)) {
            messages.sendMessage(player, "worlds_removebuilder_not_builder");
            player.closeInventory();
            return;
        }

        builders.removeBuilder(builderId);
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
        messages.sendMessage(player, "worlds_removebuilder_removed", Map.entry("%builder%", builderName));

        player.closeInventory();
    }

    private void getRemoveBuilderInput(Player player, BuildWorld buildWorld) {
        prompts.prompt(player).title("enter_player_name").request(input -> {
            String builderName = input.trim();
            removeBuilder(player, buildWorld, builderName);
        });
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        BuildWorld buildWorld =
                worldService.getWorldStorage().getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) {
            return List.of();
        }
        Builders builders = buildWorld.getBuilders();
        if (!builders.isCreator(player)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        builders.getBuilderNames().forEach(name -> WorldsCompletions.addIfStartsWith(args[1], name, result));
        return result;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.REMOVE_BUILDER;
    }
}
