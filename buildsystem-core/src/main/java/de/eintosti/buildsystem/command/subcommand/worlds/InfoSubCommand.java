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

import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class InfoSubCommand extends AbstractSubCommand {

    public InfoSubCommand(Messages messages, WorldServiceImpl worldService) {
        super(messages, worldService);
    }

    @Override
    public void execute(Player player, String worldName, String[] args) {
        BuildWorld buildWorld = requireWorld(player, worldName, args, 2, "worlds_info");
        if (buildWorld == null) {
            return;
        }

        // TODO: Print information about the custom generator?
        Builders builders = buildWorld.getBuilders();
        WorldData worldData = buildWorld.getData();
        messages.sendMessage(
                player,
                "world_info",
                Placeholders.of()
                        .add("%world%", buildWorld.getName())
                        .add("%uuid%", buildWorld.getUniqueId().toString())
                        .add("%creator%", getCreator(builders))
                        .add("%item%", worldData.get(WorldDataKey.MATERIAL).name())
                        .add("%type%", messages.getString(Messages.getMessageKey(buildWorld.getType()), player))
                        .add("%private%", worldData.get(WorldDataKey.VISIBILITY).isPrivate())
                        .add("%builders_enabled%", worldData.get(WorldDataKey.BUILDERS_ENABLED))
                        .add("%builders%", builders.asPlaceholder(player))
                        .add("%block_breaking%", worldData.get(WorldDataKey.BLOCK_BREAKING))
                        .add("%block_placement%", worldData.get(WorldDataKey.BLOCK_PLACEMENT))
                        .add(
                                "%status%",
                                ColorAPI.process(
                                        worldData.get(WorldDataKey.STATUS).getStyledName()))
                        .add("%project%", worldData.get(WorldDataKey.PROJECT))
                        .add("%permission%", worldData.get(WorldDataKey.PERMISSION))
                        .add("%time%", buildWorld.getWorldTime())
                        .add("%creation%", messages.formatDate(buildWorld.getCreation()))
                        .add("%physics%", worldData.get(WorldDataKey.PHYSICS))
                        .add("%explosions%", worldData.get(WorldDataKey.EXPLOSIONS))
                        .add("%mobai%", worldData.get(WorldDataKey.MOB_AI))
                        .add("%custom_spawn%", getCustomSpawn(buildWorld))
                        .add("%lastedited%", messages.formatDate(worldData.get(WorldDataKey.LAST_EDITED)))
                        .add("%lastloaded%", messages.formatDate(worldData.get(WorldDataKey.LAST_LOADED)))
                        .add("%lastunloaded%", messages.formatDate(worldData.get(WorldDataKey.LAST_UNLOADED)))
                        .build());
    }

    private String getCreator(Builders builders) {
        Builder creator = builders.getCreator();
        if (creator == null) {
            return "-";
        }
        return creator.getName();
    }

    private String getCustomSpawn(BuildWorld buildWorld) {
        WorldData worldData = buildWorld.getData();
        Location spawn = worldData.getCustomSpawnLocation();
        if (spawn == null) {
            return "-";
        }
        return "XYZ: " + round(spawn.getX()) + " / " + round(spawn.getY()) + " / " + round(spawn.getZ());
    }

    private double round(double value) {
        int scale = (int) Math.pow(10, 2);
        return (double) Math.round(value * scale) / scale;
    }

    @Override
    public List<String> complete(Player player, String[] args) {
        if (args.length != 2) {
            return List.of();
        }

        WorldStorage worldStorage = worldService.getWorldStorage();
        return WorldsCompletions.permittedWorldNames(
                player, worldStorage, getArgument().getPermission(), args[1]);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.INFO;
    }
}
