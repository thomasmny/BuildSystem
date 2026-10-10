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
package de.eintosti.buildsystem.world.lifecycle;

import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.lifecycle.SaveBehavior;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The shared steps of the operations that take a world away from the server for a while: delete, rename, restore and
 * backup. Each marks the world busy for its duration, so two of them never work on the same world folder at once, and
 * the ones that need the world unloaded move its players out first and stop when the unload does not go through.
 */
@NullMarked
public final class WorldOperations {

    private final Messages messages;
    private final SpawnService spawnService;
    private final Set<UUID> busyWorlds = ConcurrentHashMap.newKeySet();

    public WorldOperations(Messages messages, SpawnService spawnService) {
        this.messages = messages;
        this.spawnService = spawnService;
    }

    /**
     * Marks the world busy.
     *
     * @return {@code false} when another operation is already running on it, in which case nothing was marked
     */
    public boolean tryBegin(BuildWorld buildWorld) {
        return busyWorlds.add(buildWorld.getUniqueId());
    }

    /**
     * Marks the world free again. Call exactly once for every successful {@link #tryBegin}.
     */
    public void end(BuildWorld buildWorld) {
        busyWorlds.remove(buildWorld.getUniqueId());
    }

    public boolean isBusy(BuildWorld buildWorld) {
        return busyWorlds.contains(buildWorld.getUniqueId());
    }

    /**
     * Unloads the world without touching its state when the unload is cancelled or fails.
     *
     * @return {@code true} when the world is no longer loaded
     */
    public static boolean tryUnload(BuildWorld buildWorld, SaveBehavior saveBehavior) {
        if (buildWorld.getUnloader() instanceof WorldUnloaderImpl unloader) {
            return unloader.tryUnload(saveBehavior);
        }
        buildWorld.getUnloader().forceUnload(saveBehavior);
        return WorldNames.bukkitWorld(buildWorld.getName()) == null;
    }

    /**
     * Moves every player out of the world so it can be unloaded: to the server spawn when it lies in another loaded
     * world, otherwise to the main world's spawn. When the world is the main world and holds the spawn, there is
     * nowhere to go and the players are kicked. The teleports are synchronous, so the world is empty when this returns,
     * unless a plugin cancelled a teleport.
     *
     * @param worldName The world to empty
     * @param messageKey The message sent to each player moved, and used as the kick message
     * @return The players that were moved
     */
    public List<Player> evacuate(String worldName, String messageKey) {
        World world = WorldNames.bukkitWorld(worldName);
        if (world == null) {
            return List.of();
        }

        Location target = evacuationTarget(worldName, world);
        List<Player> moved = new ArrayList<>();
        for (Player player : List.copyOf(world.getPlayers())) {
            if (target == null) {
                player.kickPlayer(messages.getString(messageKey, player));
                continue;
            }

            player.setFallDistance(0);
            if (player.teleport(target)) {
                messages.sendMessage(player, messageKey);
                moved.add(player);
            }
        }
        return moved;
    }

    private @Nullable Location evacuationTarget(String worldName, World world) {
        if (!spawnService.isIn(worldName)) {
            Location spawn = spawnService.getSpawn();
            if (spawn != null) {
                return spawn;
            }
        }

        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty() || worlds.getFirst().equals(world)) {
            return null;
        }
        World fallback = worlds.getFirst();
        return fallback.getHighestBlockAt(fallback.getSpawnLocation())
                .getLocation()
                .add(0.5, 1, 0.5);
    }
}
