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
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.world.lifecycle.WorldPermissionsImpl;
import de.eintosti.buildsystem.world.menu.BuilderMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AddBuilderSubCommand extends AbstractSubCommand {

    public AddBuilderSubCommand(BuildSystemPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = plugin.getWorldService()
                .getWorldStorage()
                .getBuildWorld(player.getWorld().getName());
        var permissions = WorldPermissionsImpl.of(plugin, buildWorld);
        if (!permissions.canPerformCommand(player, getArgument().getPermission())) {
            messages.sendPermissionError(player);
            return;
        }

        if (buildWorld == null) {
            messages.sendMessage(player, "worlds_addbuilder_unknown_world");
            return;
        }

        switch (args.length) {
            case 1 -> getAddBuilderInput(player, buildWorld, true);
            case 2 -> addBuilder(player, buildWorld, args[1], true);
            default -> messages.sendMessage(player, "worlds_addbuilder_usage");
        }
    }

    private void addBuilder(Player player, BuildWorld buildWorld, String builderName, boolean closeInventory) {
        Player builderPlayer = Bukkit.getPlayerExact(builderName);
        if (builderPlayer != null) {
            applyBuilder(
                    player,
                    buildWorld,
                    Builder.of(builderPlayer),
                    builderPlayer.getUniqueId(),
                    builderName,
                    closeInventory);
            return;
        }

        plugin.getPlayerLookupService()
                .lookupUniqueId(builderName)
                .thenAccept(builderId -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (builderId == null) {
                        messages.sendMessage(player, "worlds_addbuilder_player_not_found");
                        player.closeInventory();
                        return;
                    }
                    applyBuilder(
                            player,
                            buildWorld,
                            Builder.of(builderId, builderName),
                            builderId,
                            builderName,
                            closeInventory);
                }));
    }

    private void applyBuilder(
            Player player,
            BuildWorld buildWorld,
            Builder builder,
            UUID builderId,
            String builderName,
            boolean closeInventory) {
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
        messages.sendMessage(player, "worlds_addbuilder_added", Map.entry("%builder%", builderName));

        if (closeInventory) {
            player.closeInventory();
        } else {
            new BuilderMenu(plugin, buildWorld, player).open(player);
        }
    }

    public void getAddBuilderInput(Player player, BuildWorld buildWorld, boolean closeInventory) {
        new PlayerChatInput(plugin, player, "enter_player_name", input -> {
            String builderName = input.trim();
            addBuilder(player, buildWorld, builderName, closeInventory);
        });
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }
        BuildWorld buildWorld = plugin.getWorldService()
                .getWorldStorage()
                .getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Builders builders = buildWorld.getBuilders();
        Bukkit.getOnlinePlayers().stream()
                .filter(pl -> !builders.isBuilder(pl) && !builders.isCreator(pl))
                .forEach(pl -> WorldsCompletions.addIfStartsWith(args[1], pl.getName(), result));
        return result;
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.ADD_BUILDER;
    }
}
