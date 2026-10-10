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
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.command.Completions;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.WorldSubCommand;
import de.eintosti.buildsystem.command.subcommand.WorldTarget;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.WorldPrompts;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class AddBuilderSubCommand extends WorldSubCommand {

    private final WorldPrompts worldPrompts;

    public AddBuilderSubCommand(Messages messages, WorldServiceImpl worldService, WorldPrompts worldPrompts) {
        super(messages, worldService, WorldTarget.current("worlds_addbuilder_unknown_world"));
        this.worldPrompts = worldPrompts;
    }

    @Override
    protected void execute(Player player, BuildWorld buildWorld, String[] args) {
        switch (args.length) {
            case 1 -> worldPrompts.promptAddBuilder(player, buildWorld, player::closeInventory);
            case 2 -> worldPrompts.addBuilder(player, buildWorld, args[1], player::closeInventory);
            default -> messages.sendMessage(player, "worlds_addbuilder_usage");
        }
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
