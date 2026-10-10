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
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.command.Completions;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.WorldSubCommand;
import de.eintosti.buildsystem.command.subcommand.WorldTarget;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AddBuilderSubCommand extends WorldSubCommand {

    private final Menus menus;
    private final PlayerLookupService playerLookupService;
    private final Prompts prompts;

    public AddBuilderSubCommand(
            Messages messages,
            WorldServiceImpl worldService,
            Menus menus,
            PlayerLookupService playerLookupService,
            Prompts prompts) {
        super(messages, worldService, WorldTarget.current("worlds_addbuilder_unknown_world"));
        this.menus = menus;
        this.playerLookupService = playerLookupService;
        this.prompts = prompts;
    }

    @Override
    protected void execute(Player player, BuildWorld buildWorld, String[] args) {
        switch (args.length) {
            case 1 -> promptForBuilder(player, buildWorld, true);
            case 2 -> addBuilder(player, buildWorld, args[1], true);
            default -> messages.sendMessage(player, "worlds_addbuilder_usage");
        }
    }

    private void addBuilder(Player player, BuildWorld buildWorld, String builderName, boolean closeInventory) {
        resolvePlayer(
                playerLookupService,
                player,
                builderName,
                "worlds_addbuilder_player_not_found",
                builder -> applyBuilder(player, buildWorld, builder, closeInventory));
    }

    private void applyBuilder(Player player, BuildWorld buildWorld, Builder builder, boolean closeInventory) {
        UUID builderId = builder.getUniqueId();
        Builders builders = buildWorld.getBuilders();
        if (builderId.equals(player.getUniqueId()) && builders.isCreator(player)) {
            messages.sendMessage(player, "worlds_addbuilder_already_creator");
            player.closeInventory();
            return;
        }

        if (builders.isBuilder(builderId)) {
            messages.sendMessage(player, "worlds_addbuilder_already_added");
            player.closeInventory();
            return;
        }

        builders.addBuilder(builder);
        XSound.ENTITY_PLAYER_LEVELUP.play(player);
        messages.sendMessage(player, "worlds_addbuilder_added", Placeholders.of("%builder%", builder.getName()));

        if (closeInventory) {
            player.closeInventory();
        } else {
            menus.openBuilder(buildWorld, player);
        }
    }

    /**
     * Asks for a builder's name from the editor menu, which reaches this without the command's permission check.
     */
    public void getAddBuilderInput(Player player, BuildWorld buildWorld, boolean closeInventory) {
        if (!buildWorld.getPermissions().canPerformCommand(player, getArgument().getPermission())) {
            messages.sendPermissionError(player);
            return;
        }
        promptForBuilder(player, buildWorld, closeInventory);
    }

    private void promptForBuilder(Player player, BuildWorld buildWorld, boolean closeInventory) {
        prompts.prompt(player).title("enter_player_name").request(input -> {
            String builderName = input.trim();
            addBuilder(player, buildWorld, builderName, closeInventory);
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

        List<String> result = new ArrayList<>();
        Builders builders = buildWorld.getBuilders();
        Bukkit.getOnlinePlayers().stream()
                .filter(pl -> !builders.isBuilder(pl) && !builders.isCreator(pl))
                .forEach(pl -> Completions.addMatching(args[1], pl.getName(), result));
        return result;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.ADD_BUILDER;
    }
}
