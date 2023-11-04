/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.navigator.settings.NavigatorType;
import de.eintosti.buildsystem.player.PlayerManager;
import de.eintosti.buildsystem.settings.Settings;
import de.eintosti.buildsystem.settings.SettingsManager;
import de.eintosti.buildsystem.world.BuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final BuildSystem     plugin;
    private final PlayerManager   playerManager;
    private final SettingsManager settingsManager;

    public PlayerMoveListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.settingsManager = plugin.getSettingsManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        checkForSky(player, event.getTo());
        if (!playerManager.getOpenNavigator().contains(player)) {
            return;
        }

        Settings settings = settingsManager.getSettings(player);
        if (!settings.getNavigatorType().equals(NavigatorType.NEW)) {
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        Location from = event.getFrom();
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> playerManager.closeNavigator(player), 5L);
        }
    }

    /**
     * The checkForSky function checks if the player is below a certain height, and if so, teleports them to spawn.
     *
     * @param player   player Get the player's world
     *                 private location extracted(player player) {
     *                 return new location(player
     * @param location location Get the y value of the player
     *                 private void checkforsky(player player, location location) {
     *                 configvalues configvalues = plugin
     */
    private void checkForSky(Player player, Location location) {
        ConfigValues configValues = plugin.getConfigValues();

        // Is feature enabled
        if (!configValues.isSaveFromDeath()) return;

        // Is port pack to spawn enabled or use reset height to 100 on y
        boolean  teleportToMapSpawn = configValues.isTeleportToMapSpawn();

        // Location field
        Location teleportLoc;

        // Check if situation is true
        if (location.getY() < configValues.getMinYHeight()) {
            if (teleportToMapSpawn) {

                BuildWorld buildWorld = plugin.getWorldManager().getBuildWorld(player.getWorld());

                // Idk if this could be null but case handled :)
                if (buildWorld == null) {
                    teleportLoc = extracted(player);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                    player.teleport(teleportLoc);
                    return;
                }

                boolean customSpawnExist = buildWorld.getData().customSpawn().get() != null;

                if (customSpawnExist) {
                    String   customSpawn = buildWorld.getData().customSpawn().get();
                    String[] spawnString = customSpawn.split(";");
                    teleportLoc = new Location(
                        player.getWorld(), Double.parseDouble(spawnString[0]), Double.parseDouble(spawnString[1]),
                        Double.parseDouble(spawnString[2]), Float.parseFloat(spawnString[3]),
                        Float.parseFloat(spawnString[4])
                    );
                } else {
                    teleportLoc = extracted(player);
                }

                XSound.ENTITY_CHICKEN_EGG.play(player);
                player.teleport(teleportLoc);
            } else {
                teleportLoc = extracted(player);
                XSound.ENTITY_CHICKEN_EGG.play(player);
                player.teleport(teleportLoc);
            }
        }
    }

    /**
     * The extracted function takes a player and returns the location of that player,
     * but with the Y coordinate increased by 100.
     *
     * @param player player Get the location of the player
     *
     * @return A location object, so you can use it as a return value
     */
    private Location extracted(Player player) {
        Location clone = player.getLocation().clone();
        clone.setY(player.getLocation().getY() + 100);
        return clone;
    }
}