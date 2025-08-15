/*
 * Copyright (c) 2018-2025, Thomas Meaney
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
package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.util.EntityAIManager;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.WorldService;
import de.eintosti.buildsystem.world.storage.WorldStorage;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EntitySpawnListener implements Listener {

    private final WorldStorage worldStorage;

    public EntitySpawnListener(BuildSystem plugin) {
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        World bukkitWorld = event.getLocation().getWorld();
        if (bukkitWorld == null) {
            return;
        }

        BuildWorld buildWorld = worldStorage.getBuildWorld(bukkitWorld.getName());
        if (buildWorld == null || buildWorld.getData().mobAi().get()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity) {
            EntityAIManager.setAIEnabled((LivingEntity) entity, false);
        }
    }
}