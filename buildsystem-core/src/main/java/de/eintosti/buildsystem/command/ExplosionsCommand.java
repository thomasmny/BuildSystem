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
public class ExplosionsCommand extends CommandBase {

    private final WorldStorageImpl worldStorage;

    public ExplosionsCommand(Messages messages, Logger logger, WorldStorageImpl worldStorage) {
        super(messages, logger, true);
        this.worldStorage = worldStorage;
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        String worldName = worldNameFromArgs(player, args, 0);
        BuildWorld buildWorld = worldStorage.getBuildWorld(worldName);
        if (buildWorld != null && !buildWorld.getPermissions().canPerformCommand(player, "buildsystem.explosions")) {
            messages.sendPermissionError(player);
            return;
        }

        switch (args.length) {
            case 0 -> toggleExplosions(player, player.getWorld());
            case 1 -> toggleExplosions(player, Bukkit.getWorld(args[0]));
            default -> messages.sendMessage(player, "explosions_usage");
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            worldStorage.getBuildWorlds().stream()
                    .filter(world -> world.getPermissions().canPerformCommand(player, "buildsystem.explosions"))
                    .forEach(world -> addArgument(args[0], world.getName(), list));
        }
        return list;
    }

    private void toggleExplosions(Player player, @Nullable World world) {
        if (world == null) {
            messages.sendMessage(player, "explosions_unknown_world");
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(world);
        if (buildWorld == null) {
            messages.sendMessage(player, "explosions_world_not_imported");
            return;
        }

        WorldData worldData = buildWorld.getData();
        if (!worldData.get(WorldDataKey.EXPLOSIONS)) {
            worldData.set(WorldDataKey.EXPLOSIONS, true);
            messages.sendMessage(player, "explosions_activated", Map.entry("%world%", buildWorld.getName()));
        } else {
            worldData.set(WorldDataKey.EXPLOSIONS, false);
            messages.sendMessage(player, "explosions_deactivated", Map.entry("%world%", buildWorld.getName()));
        }
    }
}
