/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.settings.Settings;
import de.eintosti.buildsystem.settings.SettingsManager;
import de.eintosti.buildsystem.world.SpawnManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final SettingsManager settingsManager;
    private final SpawnManager spawnManager;

    public PlayerRespawnListener(BuildSystem plugin) {
        this.settingsManager = plugin.getSettingsManager();
        this.spawnManager = plugin.getSpawnManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Settings settings = settingsManager.getSettings(player);

        if (settings.isSpawnTeleport() && spawnManager.spawnExists()) {
            event.setRespawnLocation(spawnManager.getSpawn());
        }
    }
}