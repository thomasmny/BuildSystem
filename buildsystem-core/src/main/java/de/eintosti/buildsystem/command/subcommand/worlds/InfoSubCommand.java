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
import de.eintosti.buildsystem.command.subcommand.AbstractSubCommand;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.util.color.ColorAPI;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import java.util.List;
import java.util.Map;
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
                Map.entry("%world%", buildWorld.getName()),
                Map.entry("%uuid%", buildWorld.getUniqueId().toString()),
                Map.entry("%creator%", getCreator(builders)),
                Map.entry("%item%", worldData.getMaterial().name()),
                Map.entry("%type%", messages.getString(Messages.getMessageKey(buildWorld.getType()), player)),
                Map.entry("%private%", worldData.getVisibility().isPrivate()),
                Map.entry("%builders_enabled%", worldData.isBuildersEnabled()),
                Map.entry("%builders%", builders.asPlaceholder(player)),
                Map.entry("%block_breaking%", worldData.isBlockBreaking()),
                Map.entry("%block_placement%", worldData.isBlockPlacement()),
                Map.entry("%status%", ColorAPI.process(worldData.getStatus().getStyledName())),
                Map.entry("%project%", worldData.getProject()),
                Map.entry("%permission%", worldData.getPermission()),
                Map.entry("%time%", buildWorld.getWorldTime()),
                Map.entry("%creation%", messages.formatDate(buildWorld.getCreation())),
                Map.entry("%physics%", worldData.isPhysics()),
                Map.entry("%explosions%", worldData.isExplosions()),
                Map.entry("%mobai%", worldData.isMobAi()),
                Map.entry("%custom_spawn%", getCustomSpawn(buildWorld)),
                Map.entry("%lastedited%", messages.formatDate(worldData.getLastEdited())),
                Map.entry("%lastloaded%", messages.formatDate(worldData.getLastLoaded())),
                Map.entry("%lastunloaded%", messages.formatDate(worldData.getLastUnloaded())));
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
        WorldStorage ws = worldService.getWorldStorage();
        return WorldsCompletions.permittedWorldNames(player, ws, getArgument().getPermission(), args[1]);
    }

    @Override
    public Argument getArgument() {
        return WorldsArgument.INFO;
    }
}
