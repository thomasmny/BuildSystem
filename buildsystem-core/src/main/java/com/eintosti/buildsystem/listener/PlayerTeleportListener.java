/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.PlayerManager;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.CraftBuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * @author einTosti
 */
public class PlayerTeleportListener implements Listener {

    private final BuildSystem plugin;
    private final PlayerManager playerManager;
    private final WorldManager worldManager;

    public PlayerTeleportListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            playerManager.getPreviousLocation().put(player.getUniqueId(), event.getFrom());
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        World toWorld = to.getWorld();
        if (toWorld == null) {
            return;
        }

        String worldName = to.getWorld().getName();
        CraftBuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            return;
        }

        if (!Bukkit.getWorlds().get(0).equals(Bukkit.getWorld(worldName))) {
            if (!player.hasPermission(buildWorld.getPermission()) && !buildWorld.getPermission().equalsIgnoreCase("-")) {
                player.sendMessage(plugin.getString("worlds_tp_entry_forbidden"));
                event.setCancelled(true);
            }
        }
    }
}
