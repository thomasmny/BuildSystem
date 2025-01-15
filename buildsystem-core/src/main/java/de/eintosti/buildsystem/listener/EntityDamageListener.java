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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.world.BuildWorld;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageListener implements Listener {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    public EntityDamageListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        // Teleport player up if void damage is taken
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID || !configValues.isSaveFromDeath()) {
            return;
        }

        Player player = (Player) event.getEntity();
        Location teleportLoc = player.getLocation().clone().add(0, 200, 0);

        if (configValues.isTeleportToMapSpawn()) {
            BuildWorld buildWorld = plugin.getWorldManager().getBuildWorld(player.getWorld());
            if (buildWorld != null) {
                Location spawn = buildWorld.getData().getCustomSpawnLocation();
                if (spawn != null) {
                    teleportLoc = spawn;
                }
            }
        }

        event.setCancelled(true);
        player.setFallDistance(0);
        PaperLib.teleportAsync(player, teleportLoc)
                .whenComplete((completed, throwable) -> {
                    if (!completed) {
                        return;
                    }
                    XSound.ENTITY_ZOMBIE_INFECT.play(player);
                });
    }
}
