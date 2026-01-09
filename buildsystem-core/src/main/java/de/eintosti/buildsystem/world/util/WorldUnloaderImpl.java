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
package de.eintosti.buildsystem.world.util;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.event.world.BuildWorldPostUnloadEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldUnloadEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.util.WorldUnloader;
import de.eintosti.buildsystem.config.Config.World.Unload;
import java.util.Arrays;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldUnloaderImpl implements WorldUnloader {

    private final BuildSystemPlugin plugin;
    private final BuildWorld buildWorld;

    private final long secondsUntilUnload;
    @Nullable
    private BukkitTask unloadTask;

    private WorldUnloaderImpl(BuildWorld buildWorld) {
        this.plugin = BuildSystemPlugin.get();
        this.buildWorld = buildWorld;

        this.secondsUntilUnload = calculateSecondsUntilUnload(Unload.timeUntilUnload);
    }

    private long calculateSecondsUntilUnload(String timeString) {
        String[] timeArray = timeString.split(":");
        int hours = Integer.parseInt(timeArray[0]);
        int minutes = Integer.parseInt(timeArray[1]);
        int seconds = Integer.parseInt(timeArray[2]);
        return hours * 3600L + minutes * 60L + seconds;
    }

    @Contract("_ -> new")
    public static WorldUnloaderImpl of(BuildWorld buildWorld) {
        return new WorldUnloaderImpl(buildWorld);
    }

    @Override
    public void manageUnload() {
        if (!Unload.enabled) {
            buildWorld.setLoaded(true);
            return;
        }

        buildWorld.setLoaded(buildWorld.getWorld() != null);
        startUnloadTask();
    }

    @Override
    public void startUnloadTask() {
        if (!Unload.enabled) {
            return;
        }

        this.unloadTask = Bukkit.getScheduler().runTaskLater(plugin, this::unload, 20L * secondsUntilUnload);
    }

    @Override
    public void resetUnloadTask() {
        if (this.unloadTask != null) {
            this.unloadTask.cancel();
        }

        startUnloadTask();
    }

    @Override
    public void unload() {
        World bukkitWorld = buildWorld.getWorld();
        if (bukkitWorld == null) {
            return;
        }

        if (!bukkitWorld.getPlayers().isEmpty()) {
            resetUnloadTask();
            return;
        }

        if (Unload.blacklistedWorlds.contains(buildWorld.getName()) || isSpawnWorld(bukkitWorld)) {
            return;
        }

        forceUnload(true);
    }

    @Override
    public void forceUnload(boolean save) {
        BuildWorldUnloadEvent unloadEvent = new BuildWorldUnloadEvent(buildWorld);
        Bukkit.getServer().getPluginManager().callEvent(unloadEvent);
        if (unloadEvent.isCancelled()) {
            return;
        }

        this.buildWorld.getData().lastUnloaded().set(System.currentTimeMillis());
        this.buildWorld.setLoaded(false);
        this.unloadTask = null;

        World bukkitWorld = this.buildWorld.getWorld();
        if (bukkitWorld == null) {
            return;
        }

        if (save) {
            Arrays.stream(bukkitWorld.getLoadedChunks()).forEach(Chunk::unload);
            bukkitWorld.save();
        }

        if (!Bukkit.unloadWorld(bukkitWorld, save)) {
            plugin.getLogger().warning("Failed to unload world \"" + this.buildWorld.getName() + "\". It may still be loaded in memory.");
            return;
        }

        Bukkit.getWorlds().remove(bukkitWorld);

        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPostUnloadEvent(this.buildWorld));
        plugin.getLogger().info("*** Unloaded world \"" + this.buildWorld.getName() + "\" ***");
    }

    private boolean isSpawnWorld(World bukkitWorld) {
        Location spawn = plugin.getSpawnManager().getSpawn();
        if (spawn == null) {
            return false;
        }
        return Objects.equals(spawn.getWorld(), bukkitWorld);
    }
} 