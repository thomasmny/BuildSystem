/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.WorldManager;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.object.world.WorldStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

/**
 * @author einTosti
 */
public class PlayerArmorStandManipulateListener implements Listener {
    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public PlayerArmorStandManipulateListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();

        BuildWorld buildWorld = worldManager.getBuildWorld(player.getWorld().getName());
        if (buildWorld == null) return;

        if (disableArchivedWorlds(buildWorld, player, event)) return;
        if (checkWorldSettings(buildWorld, player, event)) return;
        if (checkBuilders(buildWorld, player, event)) return;
    }

    private boolean disableArchivedWorlds(BuildWorld buildWorld, Player player, PlayerArmorStandManipulateEvent event) {
        if (!plugin.canBypass(player) && buildWorld.getStatus() == WorldStatus.ARCHIVE) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    private boolean checkWorldSettings(BuildWorld buildWorld, Player player, PlayerArmorStandManipulateEvent event) {
        if (!plugin.canBypass(player) && buildWorld.isBlockPlacement()) {
            event.setCancelled(true);
            return true;
        }
        return false;
    }

    private boolean checkBuilders(BuildWorld buildWorld, Player player, PlayerArmorStandManipulateEvent event) {
        if (plugin.canBypass(player)) return false;
        if (plugin.isCreatorIsBuilder() && buildWorld.getCreatorId() != null && buildWorld.getCreatorId().equals(player.getUniqueId())) {
            return false;
        }

        if (buildWorld.isBuilders() && !buildWorld.isBuilder(player)) {
            event.setCancelled(true);
            return true;
        }

        return false;
    }
}