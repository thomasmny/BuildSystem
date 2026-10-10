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
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.command.Completions;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.WorldSubCommand;
import de.eintosti.buildsystem.command.subcommand.WorldTarget;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class RemoveBuilderSubCommand extends WorldSubCommand {

    private final PlayerLookupService playerLookupService;
    private final Prompts prompts;

    public RemoveBuilderSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            PlayerLookupService playerLookupService,
            Prompts prompts) {
        super(messages, worldService, WorldTarget.current("worlds_removebuilder_unknown_world"));
        this.playerLookupService = playerLookupService;
        this.prompts = prompts;
    }

    @Override
    protected void execute(Player player, BuildWorld buildWorld, String[] args) {
        switch (args.length) {
            case 1 -> getRemoveBuilderInput(player, buildWorld);
            case 2 -> removeBuilder(player, buildWorld, args[1]);
            default -> messages.sendMessage(player, "worlds_removebuilder_usage");
        }
    }

    private void removeBuilder(Player player, BuildWorld buildWorld, String builderName) {
        resolvePlayer(
                playerLookupService,
                player,
                builderName,
                "worlds_removebuilder_player_not_found",
                builder -> applyRemove(player, buildWorld, builder));
    }

    private void applyRemove(Player player, BuildWorld buildWorld, Builder builder) {
        UUID builderId = builder.getUniqueId();
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
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        messages.sendMessage(player, "worlds_removebuilder_removed", Placeholders.of("%builder%", builder.getName()));

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

        BuildWorld buildWorld = worldService.getWorldStorage().getBuildWorld(player.getWorld());
        if (buildWorld == null) {
            return List.of();
        }

        Builders builders = buildWorld.getBuilders();
        if (!builders.isCreator(player)) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        builders.getBuilderNames().forEach(name -> Completions.addMatching(args[1], name, result));
        return result;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.REMOVE_BUILDER;
    }
}
