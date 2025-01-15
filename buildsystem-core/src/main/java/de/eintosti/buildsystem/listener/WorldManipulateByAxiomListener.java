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

import com.moulberry.axiom.event.AxiomModifyWorldEvent;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.event.EventDispatcher;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Only register if axiom is available
 */
public class WorldManipulateByAxiomListener implements Listener {

    private final EventDispatcher dispatcher;


    /**
     * @param plugin plugin to register.
     */
    public WorldManipulateByAxiomListener(@NotNull BuildSystem plugin) {
        this.dispatcher = new EventDispatcher(plugin.getWorldManager());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler()
    public void onWorldModification(AxiomModifyWorldEvent event) {
        // I don't know if it is possible. Just to be safe.
        if (!event.getPlayer().getWorld().equals(event.getWorld())) {
            event.setCancelled(true);
            throw new IllegalStateException("Player modifies a world in which he is not present! The event got cancelled for safety reasons.");
        }
        dispatcher.dispatchManipulationEventIfPlayerInBuildWorld(event.getPlayer(), event);
    }
}
