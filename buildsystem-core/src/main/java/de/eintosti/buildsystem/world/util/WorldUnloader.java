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
package de.eintosti.buildsystem.world.util;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.event.world.BuildWorldPostUnloadEvent;
import de.eintosti.buildsystem.event.world.BuildWorldUnloadEvent;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class WorldUnloader {

    private final BuildSystem plugin;
    private final ConfigValues configValues;
    private final BuildWorld buildWorld;

    private final long seconds;
    private BukkitTask unloadTask;

    private WorldUnloader(BuildWorld buildWorld) {
        this.plugin = JavaPlugin.getPlugin(BuildSystem.class);
        this.configValues = plugin.getConfigValues();
        this.buildWorld = buildWorld;

        this.seconds = configValues.getTimeUntilUnload();
    }

    public static WorldUnloader of(BuildWorld buildWorld) {
        return new WorldUnloader(buildWorld);
    }

    public void manageUnload() {
        if (!configValues.isUnloadWorlds()) {
            buildWorld.setLoaded(true);
            return;
        }

        buildWorld.setLoaded(buildWorld.getWorld() != null);
        startUnloadTask();
    }

    public void startUnloadTask() {
        if (!configValues.isUnloadWorlds()) {
            return;
        }

        this.unloadTask = Bukkit.getScheduler().runTaskLater(plugin, this::unload, 20L * seconds);
    }

    public void resetUnloadTask() {
        if (this.unloadTask != null) {
            this.unloadTask.cancel();
        }

        startUnloadTask();
    }

    private void unload() {
        World bukkitWorld = buildWorld.getWorld();
        if (bukkitWorld == null) {
            return;
        }

        if (!bukkitWorld.getPlayers().isEmpty()) {
            resetUnloadTask();
            return;
        }

        if (configValues.getBlackListedWorldsToUnload().contains(buildWorld.getName()) || isSpawnWorld(bukkitWorld)) {
            return;
        }

        forceUnload(true);
    }

    public void forceUnload(boolean save) {
        World bukkitWorld = buildWorld.getWorld();
        if (bukkitWorld == null) {
            return;
        }

        BuildWorldUnloadEvent unloadEvent = new BuildWorldUnloadEvent(buildWorld);
        Bukkit.getServer().getPluginManager().callEvent(unloadEvent);
        if (unloadEvent.isCancelled()) {
            return;
        }

        if (save) {
            bukkitWorld.save();
        }

        for (Chunk chunk : bukkitWorld.getLoadedChunks()) {
            chunk.unload(save);
        }
        Bukkit.unloadWorld(bukkitWorld, save);
        Bukkit.getWorlds().remove(bukkitWorld);

        buildWorld.getData().lastUnloaded().set(System.currentTimeMillis());
        buildWorld.setLoaded(false);
        this.unloadTask = null;

        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPostUnloadEvent(buildWorld));
        plugin.getLogger().info("*** Unloaded world \"" + buildWorld.getName() + "\" ***");
    }

    private boolean isSpawnWorld(World bukkitWorld) {
        SpawnManager spawnManager = plugin.getSpawnManager();
        if (!spawnManager.spawnExists()) {
            return false;
        }

        return spawnManager.getSpawn().getWorld().equals(bukkitWorld);
    }
} 