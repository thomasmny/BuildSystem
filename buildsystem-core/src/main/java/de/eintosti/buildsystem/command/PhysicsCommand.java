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
package de.eintosti.buildsystem.command;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.WorldData;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PhysicsCommand extends CommandBase {

    private final WorldStorageImpl worldStorage;

    public PhysicsCommand(Messages messages, Logger logger, WorldStorageImpl worldStorage) {
        super(messages, logger, true);
        this.worldStorage = worldStorage;
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        String worldName = worldNameFromArgs(player, args, 0);
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld != null && !buildWorld.getPermissions().canPerformCommand(player, "buildsystem.physics")) {
            messages.sendPermissionError(player);
            return;
        }

        switch (args.length) {
            case 0 -> togglePhysics(player, player.getWorld());
            case 1 -> {
                // TODO: Check each world for permission individually?
                if (args[0].equalsIgnoreCase("all") && !worldStorage.worldExists("all")) {
                    worldStorage
                            .getBuildWorlds()
                            .forEach(world -> world.getData().set(WorldDataKey.PHYSICS, true));
                    messages.sendMessage(player, "physics_activated_all");
                } else {
                    togglePhysics(player, Bukkit.getWorld(args[0]));
                }
            }
            default -> messages.sendMessage(player, "physics_usage");
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            worldStorage.getBuildWorlds().stream()
                    .filter(world -> world.getPermissions().canPerformCommand(player, "buildsystem.physics"))
                    .forEach(world -> addArgument(args[0], world.getName(), list));
        }
        return list;
    }

    private void togglePhysics(Player player, @Nullable World world) {
        if (world == null) {
            messages.sendMessage(player, "physics_unknown_world");
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(world.getName());
        if (buildWorld == null) {
            messages.sendMessage(player, "physics_world_not_imported");
            return;
        }

        WorldData worldData = buildWorld.getData();
        if (!worldData.get(WorldDataKey.PHYSICS)) {
            worldData.set(WorldDataKey.PHYSICS, true);
            messages.sendMessage(player, "physics_activated", Map.entry("%world%", buildWorld.getName()));
        } else {
            worldData.set(WorldDataKey.PHYSICS, false);
            messages.sendMessage(player, "physics_deactivated", Map.entry("%world%", buildWorld.getName()));
        }
    }
}
