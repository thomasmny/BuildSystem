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
import de.eintosti.buildsystem.i18n.Placeholders;
import de.eintosti.buildsystem.world.WorldNames;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The shared steps of the operations that take a world away from the server for a while: delete, unimport, rename,
 * restore, backup and download. Each marks the world busy for its duration, so two of them never work on the same
 * world folder at once, and the ones that need the world unloaded stop when the unload does not go through.
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
     * Runs an operation with the world marked busy and frees the world once the operation's future completes. While
     * another operation holds the world, this one is not started and the future fails with a
     * {@link WorldOperationRefusedException}.
     */
    public <T> CompletableFuture<T> runExclusively(BuildWorld buildWorld, Supplier<CompletableFuture<T>> operation) {
        UUID worldId = buildWorld.getUniqueId();
        if (!busyWorlds.add(worldId)) {
            return CompletableFuture.failedFuture(WorldOperationRefusedException.busy(buildWorld.getName()));
        }

        boolean started = false;
        try {
            CompletableFuture<T> future = Objects.requireNonNull(operation.get(), "The operation returned no future");
            started = true;
            return future.whenComplete((result, throwable) -> busyWorlds.remove(worldId));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        } finally {
            if (!started) {
                busyWorlds.remove(worldId);
            }
        }
    }

    public boolean isBusy(BuildWorld buildWorld) {
        return busyWorlds.contains(buildWorld.getUniqueId());
    }

    /**
     * Moves the players out of the world and unloads it. When the unload does not go through, the players are sent
     * back and the world is left as it was.
     *
     * @param messageKey Sent to each player moved, and used as the kick message when there is nowhere to move them
     * @return The players that were moved, for the caller to bring back once the world is loaded again
     * @throws WorldOperationRefusedException When a listener cancelled the unload or Bukkit refused it
     */
    public List<Player> takeOffline(BuildWorld buildWorld, String messageKey, SaveBehavior saveBehavior) {
        String worldName = buildWorld.getName();
        List<Player> moved = evacuate(worldName, messageKey);
        // forceUnload is void API, so whether the world went away is read back from the server.
        buildWorld.getUnloader().forceUnload(saveBehavior);
        if (WorldNames.bukkitWorld(worldName) != null) {
            moved.forEach(buildWorld.getTeleporter()::teleport);
            throw WorldOperationRefusedException.notUnloaded(worldName);
        }
        return moved;
    }

    /**
     * Tells the player why an operation was refused, when {@code failure} is a {@link WorldOperationRefusedException}.
     *
     * @return {@code true} when it was a refusal, {@code false} when the failure is left to the caller
     */
    public boolean reportRefusal(Player player, String worldName, Throwable failure) {
        WorldOperationRefusedException refused = WorldOperationRefusedException.find(failure);
        if (refused == null) {
            return false;
        }
        messages.sendMessage(player, refused.messageKey(), Placeholders.of("%world%", worldName));
        return true;
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
