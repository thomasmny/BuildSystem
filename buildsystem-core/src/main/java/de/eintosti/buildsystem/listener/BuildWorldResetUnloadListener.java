/*
 * Copyright (c) 2018-2026, Thomas Meaney
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

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildWorldResetUnloadListener implements Listener {

    private final WorldStorageImpl worldStorage;

    public BuildWorldResetUnloadListener(BuildSystemPlugin plugin) {
        this.worldStorage = plugin.getWorldService().getWorldStorage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        resetUnloadTask(event.getFrom());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        resetUnloadTask(event.getPlayer().getWorld());
    }

    /**
     * Resets the unload task for the given world.
     *
     * @param world The world for which the unload task should be reset
     */
    private void resetUnloadTask(World world) {
        BuildWorld buildWorld = worldStorage.getBuildWorld(world.getName());
        if (buildWorld != null) {
            buildWorld.getUnloader().resetUnloadTask();
        }
    }
}