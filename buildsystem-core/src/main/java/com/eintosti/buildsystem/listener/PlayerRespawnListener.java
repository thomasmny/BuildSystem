/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.settings.Settings;
import com.eintosti.buildsystem.settings.SettingsManager;
import com.eintosti.buildsystem.world.SpawnManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * @author einTosti
 */
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