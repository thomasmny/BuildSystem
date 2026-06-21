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
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SpawnCommand extends CommandBase {

    private final ConfigService configService;
    private final SpawnService spawnService;
    private final WorldStorageImpl worldStorage;

    public SpawnCommand(
            Messages messages,
            Logger logger,
            ConfigService configService,
            SpawnService spawnService,
            WorldStorageImpl worldStorage) {
        super(messages, logger, true);
        this.configService = configService;
        this.spawnService = spawnService;
        this.worldStorage = worldStorage;
    }

    @Override
    protected void run(Player player, String label, String[] args) {
        switch (args.length) {
            case 0 -> {
                if (!spawnService.teleport(player)) {
                    messages.sendMessage(player, "spawn_unavailable");
                } else if (configService.current().settings().spawnTeleportMessage()) {
                    messages.sendMessage(player, "spawn_teleported");
                }
            }

            case 1 -> {
                if (!player.hasPermission("buildsystem.spawn")) {
                    messages.sendMessage(player, "spawn_usage");
                    return;
                }

                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "set" -> {
                        Location playerLocation = player.getLocation();
                        World bukkitWorld = playerLocation.getWorld();
                        if (bukkitWorld == null) {
                            messages.sendMessage(player, "spawn_world_not_imported");
                            return;
                        }

                        BuildWorld buildWorld = worldStorage.getBuildWorld(bukkitWorld);
                        if (buildWorld == null) {
                            messages.sendMessage(player, "spawn_world_not_imported");
                            return;
                        }

                        spawnService.set(playerLocation, buildWorld.getName());
                        messages.sendMessage(
                                player,
                                "spawn_set",
                                Map.entry("%x%", round(playerLocation.getX())),
                                Map.entry("%y%", round(playerLocation.getY())),
                                Map.entry("%z%", round(playerLocation.getZ())),
                                Map.entry("%world%", playerLocation.getWorld().getName()));
                    }
                    case "remove" -> {
                        spawnService.remove();
                        messages.sendMessage(player, "spawn_remove");
                    }
                    default -> messages.sendMessage(player, "spawn_admin");
                }
            }

            default -> {
                String key = player.hasPermission("buildsystem.spawn") ? "spawn_admin" : "spawn_usage";
                messages.sendMessage(player, key);
            }
        }
    }

    @Override
    protected List<String> complete(Player player, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (player.hasPermission("buildsystem.spawn")) {
            addArgument(args[0], "set", list);
            addArgument(args[0], "remove", list);
        }
        return list;
    }

    private String round(double value) {
        int scale = (int) Math.pow(10, 2);
        return String.valueOf((double) Math.round(value * scale) / scale);
    }
}
