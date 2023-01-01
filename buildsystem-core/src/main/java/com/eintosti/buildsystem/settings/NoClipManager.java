/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.settings;

import com.eintosti.buildsystem.BuildSystem;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author einTosti
 */
public class NoClipManager {

    private final BuildSystem plugin;
    private final Map<UUID, GameMode> previousGameMode;
    private final Set<UUID> noClipPlayers;

    public NoClipManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.noClipPlayers = new HashSet<>();
        this.previousGameMode = new HashMap<>();
        runBlockCheckTask();
    }

    private void runBlockCheckTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkForBlocks, 0L, 4L);
    }

    private void checkForBlocks() {
        List<UUID> toRemove = new ArrayList<>();

        for (UUID uuid : noClipPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                toRemove.add(uuid);
                continue;
            }

            GameMode playerGameMode = player.getGameMode();
            if (playerGameMode != GameMode.SPECTATOR) {
                previousGameMode.put(uuid, playerGameMode);
            }

            GameMode gameMode;
            if (checkNoClip(player)) {
                gameMode = GameMode.SPECTATOR;
            } else {
                gameMode = previousGameMode.getOrDefault(uuid, GameMode.CREATIVE);
                previousGameMode.remove(uuid);
            }

            if (player.getGameMode() == gameMode) {
                continue;
            }

            GameMode finalGameMode = gameMode;
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setGameMode(finalGameMode);
                if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
                    if (player.getAllowFlight()) {
                        player.setFlying(true);
                    }
                }
            });
        }

        toRemove.forEach(this::stopNoClip);
    }

    private boolean checkNoClip(Player player) {
        Location playerLocation = player.getLocation();

        Set<Location> locations = Sets.newHashSet(
                playerLocation.clone().add(0.4, 0, 0),
                playerLocation.clone().add(-0.4, 0, 0),
                playerLocation.clone().add(0.4, 1, 0),
                playerLocation.clone().add(-0.4, 1, 0),
                playerLocation.clone().add(0, 0, 0.4),
                playerLocation.clone().add(0, 0, -0.4),
                playerLocation.clone().add(0, 1, 0.4),
                playerLocation.clone().add(0, 1, -0.4),
                playerLocation.clone().add(0, 1.9, 0)
        );

        return locations.stream().anyMatch(location -> isSolidBlock(location.getBlock()))
                || (player.isSneaking() && isSolidBlock(playerLocation.clone().add(0, -0.1, 0).getBlock()));
    }

    private boolean isSolidBlock(Block block) {
        return block.getType() != Material.AIR;
    }

    public boolean isNoClip(UUID uuid) {
        return noClipPlayers.contains(uuid);
    }

    /**
     * Only add a player to the list of No-Clip players if {@link Settings#isNoClip} is equal to {@code true}.
     *
     * @param player   The player to add
     * @param settings The player's settings
     */
    public void startNoClip(Player player, Settings settings) {
        if (!settings.isNoClip()) {
            noClipPlayers.remove(player.getUniqueId());
            return;
        }

        startNoClip(player);
    }

    /**
     * Forcefully add a player to the list of NoClip players.
     *
     * @param player The player to add
     */
    public void startNoClip(Player player) {
        noClipPlayers.add(player.getUniqueId());
    }

    /**
     * Only remove a player from the list of No-Clip players if said player has
     * No-Clip enabled, i.e. {@link NoClipManager#isNoClip} is equal to {@code true}.
     * <p>
     * Will also set the player to their previous {@link GameMode}.
     *
     * @param uuid The uuid of the player to remove
     */
    public void stopNoClip(UUID uuid) {
        if (!isNoClip(uuid)) {
            return;
        }

        this.noClipPlayers.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }

        player.setGameMode(previousGameMode.getOrDefault(uuid, GameMode.CREATIVE));
        previousGameMode.remove(uuid);

        if (player.getAllowFlight()) {
            player.setFlying(true);
        }
    }
}