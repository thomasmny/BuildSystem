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
package de.eintosti.buildsystem.player.noclip;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.player.settings.Settings;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class NoClipService {

    private final BuildSystemPlugin plugin;
    private final Map<UUID, GameMode> previousGameMode;
    private final Set<UUID> noClipPlayers;

    public NoClipService(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.noClipPlayers = new HashSet<>();
        this.previousGameMode = new HashMap<>();
        runBlockCheckTask();
    }

    private void runBlockCheckTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkForBlocks, 0L, 4L);
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

            player.setGameMode(gameMode);
            if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
                if (player.getAllowFlight()) {
                    player.setFlying(true);
                }
            }
        }

        toRemove.forEach(this::stopNoClip);
    }

    private boolean checkNoClip(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        // Check the positions around the player hitbox (feet, body, head)
        if (isSolid(world, x + 0.4, y, z)
                || isSolid(world, x - 0.4, y, z)
                || isSolid(world, x + 0.4, y + 1, z)
                || isSolid(world, x - 0.4, y + 1, z)
                || isSolid(world, x, y, z + 0.4)
                || isSolid(world, x, y, z - 0.4)
                || isSolid(world, x, y + 1, z + 0.4)
                || isSolid(world, x, y + 1, z - 0.4)
                || isSolid(world, x, y + 1.9, z)) {
            return true;
        }

        return player.isSneaking() && isSolid(world, x, y - 0.1, z);
    }

    private boolean isSolid(World world, double x, double y, double z) {
        return world.getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))
                        .getType()
                != Material.AIR;
    }

    public boolean isNoClip(UUID uuid) {
        return noClipPlayers.contains(uuid);
    }

    /**
     * Only add a player to the list of No-Clip players if {@link Settings#isNoClip} is equal to {@code true}.
     *
     * @param player The player to add
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
     * Only remove a player from the list of No-Clip players if said player has No-Clip enabled, i.e.
     * {@link NoClipManager#isNoClip} is equal to {@code true}.
     *
     * <p>Will also set the player to their previous {@link GameMode}.
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
