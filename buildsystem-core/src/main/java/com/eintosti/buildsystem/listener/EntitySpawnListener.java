/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.listener;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.util.EntityAIManager;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * @author einTosti
 */
public class EntitySpawnListener implements Listener {

    private final WorldManager worldManager;

    public EntitySpawnListener(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        World bukkitWorld = event.getLocation().getWorld();
        if (bukkitWorld == null) {
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());
        if (buildWorld == null || buildWorld.isMobAI()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity) {
            EntityAIManager.setAIEnabled(entity, false);
        }
    }
}