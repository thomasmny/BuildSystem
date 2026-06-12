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
package de.eintosti.buildsystem.world.lifecycle;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.event.world.BuildWorldPostUnloadEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldUnloadEvent;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.lifecycle.WorldUnloader;
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

    private WorldUnloaderImpl(BuildSystemPlugin plugin, BuildWorld buildWorld) {
        this.plugin = plugin;
        this.buildWorld = buildWorld;

        this.secondsUntilUnload = calculateSecondsUntilUnload(plugin.getConfigService().current().world().unload().timeUntilUnload());
    }

    private long calculateSecondsUntilUnload(String timeString) {
        String[] timeArray = timeString.split(":");
        int hours = Integer.parseInt(timeArray[0]);
        int minutes = Integer.parseInt(timeArray[1]);
        int seconds = Integer.parseInt(timeArray[2]);
        return hours * 3600L + minutes * 60L + seconds;
    }

    @Contract("_, _ -> new")
    public static WorldUnloaderImpl of(BuildSystemPlugin plugin, BuildWorld buildWorld) {
        return new WorldUnloaderImpl(plugin, buildWorld);
    }

    @Override
    public void manageUnload() {
        if (!plugin.getConfigService().current().world().unload().enabled()) {
            buildWorld.setLoaded(true);
            return;
        }

        buildWorld.setLoaded(buildWorld.getWorld() != null);
        startUnloadTask();
    }

    @Override
    public void startUnloadTask() {
        if (!plugin.getConfigService().current().world().unload().enabled()) {
            return;
        }

        this.unloadTask = Bukkit.getScheduler().runTaskLater(plugin, this::unload, 20L * secondsUntilUnload);
    }

    @Override
    public void resetUnloadTask() {
        cancelScheduledTask();
        startUnloadTask();
    }

    public void cancelScheduledTask() {
        if (this.unloadTask != null) {
            this.unloadTask.cancel();
            this.unloadTask = null;
        }
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

        if (plugin.getConfigService().current().world().unload().blacklistedWorlds().contains(buildWorld.getName()) || isSpawnWorld(bukkitWorld)) {
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
        Location spawn = plugin.getSpawnService().getSpawn();
        if (spawn == null) {
            return false;
        }
        return Objects.equals(spawn.getWorld(), bukkitWorld);
    }
} 