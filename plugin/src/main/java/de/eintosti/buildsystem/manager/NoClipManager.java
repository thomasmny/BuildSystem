/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.manager;

import de.eintosti.buildsystem.BuildSystem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author einTosti
 */
public class NoClipManager {
    private final BuildSystem plugin;
    private final HashMap<UUID, GameMode> previousGameMode;
    private final HashSet<UUID> noClipPlayers;

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

            if (player.getGameMode() == gameMode) continue;

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

        if (player.isSneaking() && isValidBlock(playerLocation.add(0, -0.1, 0).getBlock())) {
            return true;
        } else if (isValidBlock(playerLocation.add(0.4, 0, 0).getBlock())) {
            return true;
        } else if (isValidBlock(playerLocation.add(-0.4, 0, 0).getBlock())) {
            return true;
        } else if (isValidBlock(playerLocation.add(0.4, 1, 0).getBlock())) {
            return true;
        } else if (isValidBlock(playerLocation.add(-0.4, 1, 0).getBlock())) {
            return true;
        } else if (isValidBlock(playerLocation.add(0, 0, 0.4).getBlock())) {
            return true;
        } else if (isValidBlock(playerLocation.add(0, 0, -0.4).getBlock())) {
            return true;
        } else if (isValidBlock(playerLocation.add(0, 1, 0.4).getBlock())) {
            return true;
        } else if (isValidBlock(playerLocation.add(0, 1, -0.4).getBlock())) {
            return true;
        } else return isValidBlock(playerLocation.add(0, 1.9, 0).getBlock());
    }

    private boolean isValidBlock(Block block) {
        return block.getType() != Material.AIR;
    }

    public boolean isNoClip(UUID uuid) {
        return noClipPlayers.contains(uuid);
    }

    public void startNoClip(Player player) {
        noClipPlayers.add(player.getUniqueId());
    }

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
