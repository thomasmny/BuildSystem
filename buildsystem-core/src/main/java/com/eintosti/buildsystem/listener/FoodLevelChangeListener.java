/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.CraftBuildWorld;
import com.eintosti.buildsystem.api.world.WorldStatus;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * @author einTosti
 */
public class FoodLevelChangeListener implements Listener {

    private final WorldManager worldManager;

    public FoodLevelChangeListener(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        World bukkitWorld = player.getWorld();
        CraftBuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());

        if (buildWorld != null && buildWorld.getStatus() == WorldStatus.ARCHIVE) {
            event.setCancelled(true);
        }
    }
}
