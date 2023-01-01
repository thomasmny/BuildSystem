/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.player.PlayerManager;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
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

    private final PlayerManager playerManager;
    private final WorldManager worldManager;

    public PlayerTeleportListener(BuildSystem plugin) {
        this.playerManager = plugin.getPlayerManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            playerManager.getBuildPlayer(player).setPreviousLocation(event.getFrom());
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
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            return;
        }

        if (!Bukkit.getWorlds().get(0).equals(Bukkit.getWorld(worldName))) {
            if (!worldManager.canEnter(player, buildWorld)) {
                Messages.sendMessage(player, "worlds_tp_entry_forbidden");
                event.setCancelled(true);
            }
        }
    }
}