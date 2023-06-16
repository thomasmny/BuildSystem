/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.player.BuildPlayerManager;
import de.eintosti.buildsystem.player.CraftBuildPlayer;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.settings.CraftSettings;
import de.eintosti.buildsystem.world.BuildWorldManager;
import de.eintosti.buildsystem.world.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class AsyncPlayerPreLoginListener implements Listener {

    private final BuildSystemPlugin plugin;
    private final BuildPlayerManager playerManager;
    private final SpawnManager spawnManager;
    private final BuildWorldManager worldManager;

    public AsyncPlayerPreLoginListener(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.spawnManager = plugin.getSpawnManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        CraftBuildPlayer buildPlayer = playerManager.getBuildPlayer(uuid);
        if (buildPlayer == null) {
            return;
        }

        CraftSettings settings = buildPlayer.getSettings();
        if (settings.isSpawnTeleport() && spawnManager.spawnExists()) {
            return;
        }

        LogoutLocation logoutLocation = buildPlayer.getLogoutLocation();
        if (logoutLocation == null) {
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(logoutLocation.getWorldName());
        if (buildWorld == null) {
            buildPlayer.setLogoutLocation(null);
        } else {
            Bukkit.getScheduler().runTask(plugin, buildWorld::load);
        }
    }
}