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
package de.eintosti.buildsystem.world.util;

import com.cryptomorin.xseries.messages.Titles;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.world.BuildWorld;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldTeleporter {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private final BuildWorld buildWorld;

    public WorldTeleporter(BuildWorld buildWorld) {
        this.plugin = JavaPlugin.getPlugin(BuildSystem.class);
        this.configValues = plugin.getConfigValues();

        this.buildWorld = buildWorld;
    }

    public void teleport(Player player) {
        boolean hadToLoad = false;
        if (configValues.isUnloadWorlds() && !buildWorld.isLoaded()) {
            buildWorld.load(player);
            hadToLoad = true;
        }

        World bukkitWorld = Bukkit.getServer().getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            Messages.sendMessage(player, "worlds_tp_unknown_world");
            return;
        }

        Location location = bukkitWorld.getSpawnLocation().add(0.5, 0, 0.5);
        Location customSpawn = buildWorld.getData().getCustomSpawnLocation();
        if (customSpawn != null) {
            location = customSpawn;
        } else {
            switch (buildWorld.getType()) {
                case NETHER:
                case END:
                    Location blockLocation = null;
                    for (int y = 0; y < bukkitWorld.getMaxHeight(); y++) {
                        Block block = bukkitWorld.getBlockAt(location.getBlockX(), y, location.getBlockZ());
                        if (isSafeLocation(block.getLocation())) {
                            blockLocation = block.getLocation();
                            break;
                        }
                    }
                    if (blockLocation != null) {
                        location = new Location(
                                bukkitWorld,
                                blockLocation.getBlockX() + 0.5,
                                blockLocation.getBlockY() + 1,
                                blockLocation.getBlockZ() + 0.5
                        );
                    }
                    break;
                default:
                    break;
            }
        }

        Location finalLocation = location;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PaperLib.teleportAsync(player, finalLocation).whenComplete((completed, throwable) -> {
                if (!completed) {
                    return;
                }

                Titles.clearTitle(player);
                player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                if (!finalLocation.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                    player.setFlying(player.getAllowFlight());
                }
            });
        }, hadToLoad ? 20L : 0L);
    }

    public boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        if (feet.getType() != Material.AIR && feet.getLocation().add(0, 1, 0).getBlock().getType() != Material.AIR) {
            return false;
        }

        Block head = feet.getRelative(BlockFace.UP);
        if (head.getType() != Material.AIR) {
            return false;
        }

        Block ground = feet.getRelative(BlockFace.DOWN);
        return ground.getType().isSolid();
    }
}
