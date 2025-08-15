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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.builder.Builders;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.command.subcommand.Argument;
import de.eintosti.buildsystem.command.subcommand.SubCommand;
import de.eintosti.buildsystem.command.tabcomplete.WorldsTabCompleter.WorldsArgument;
import de.eintosti.buildsystem.world.util.WorldPermissionsImpl;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class InfoSubCommand implements SubCommand {

    @Nullable
    private final BuildWorld buildWorld;

    public InfoSubCommand(BuildSystemPlugin plugin, String worldName) {
        this.buildWorld = plugin.getWorldService().getWorldStorage().getBuildWorld(worldName);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!WorldPermissionsImpl.of(buildWorld).canPerformCommand(player, getArgument().getPermission())) {
            Messages.sendPermissionError(player);
            return;
        }

        if (args.length > 2) {
            Messages.sendMessage(player, "worlds_info_usage");
            return;
        }

        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_info_unknown_world");
            return;
        }

        //TODO: Print information about the custom generator?
        Builders builders = buildWorld.getBuilders();
        WorldData worldData = buildWorld.getData();
        Messages.sendMessage(player, "world_info",
                Map.entry("%world%", buildWorld.getName()),
                Map.entry("%uuid%", buildWorld.getUniqueId().toString()),
                Map.entry("%creator%", getCreator(builders)),
                Map.entry("%item%", worldData.material().get().name()),
                Map.entry("%type%", Messages.getString(Messages.getMessageKey(buildWorld.getType()), player)),
                Map.entry("%private%", worldData.privateWorld().get()),
                Map.entry("%builders_enabled%", worldData.buildersEnabled().get()),
                Map.entry("%builders%", builders.asPlaceholder(player)),
                Map.entry("%block_breaking%", worldData.blockBreaking().get()),
                Map.entry("%block_placement%", worldData.blockPlacement().get()),
                Map.entry("%status%", Messages.getString(Messages.getMessageKey(worldData.status().get()), player)),
                Map.entry("%project%", worldData.project().get()),
                Map.entry("%permission%", worldData.permission().get()),
                Map.entry("%time%", buildWorld.getWorldTime()),
                Map.entry("%creation%", Messages.formatDate(buildWorld.getCreation())),
                Map.entry("%physics%", worldData.physics().get()),
                Map.entry("%explosions%", worldData.explosions().get()),
                Map.entry("%mobai%", worldData.mobAi().get()),
                Map.entry("%custom_spawn%", getCustomSpawn(buildWorld)),
                Map.entry("%lastedited%", Messages.formatDate(worldData.lastEdited().get())),
                Map.entry("%lastloaded%", Messages.formatDate(worldData.lastLoaded().get())),
                Map.entry("%lastunloaded%", Messages.formatDate(worldData.lastUnloaded().get()))
        );
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
    public Argument getArgument() {
        return WorldsArgument.INFO;
    }
}