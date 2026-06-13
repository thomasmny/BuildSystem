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
import de.eintosti.buildsystem.api.world.lifecycle.SaveBehavior;
import de.eintosti.buildsystem.api.world.lifecycle.WorldUnloader;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
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
    private final BuildWorldImpl buildWorld;

    private final long secondsUntilUnload;

    private @Nullable BukkitTask unloadTask;

    private WorldUnloaderImpl(BuildSystemPlugin plugin, BuildWorldImpl buildWorld) {
        this.plugin = plugin;
        this.buildWorld = buildWorld;

        this.secondsUntilUnload = calculateSecondsUntilUnload(
                plugin.getConfigService().current().world().unload().timeUntilUnload());
    }

    private static final long DEFAULT_SECONDS_UNTIL_UNLOAD = 3600;

    /**
     * Parses the configured {@code HH:mm:ss} unload delay. A malformed value falls back to one hour instead of
     * throwing, because this runs during world construction — an exception here would abort loading every world.
     */
    private long calculateSecondsUntilUnload(String timeString) {
        String[] timeArray = timeString.split(":");
        if (timeArray.length != 3) {
            return warnAndFallBack(timeString);
        }

        try {
            int hours = Integer.parseInt(timeArray[0]);
            int minutes = Integer.parseInt(timeArray[1]);
            int seconds = Integer.parseInt(timeArray[2]);
            return hours * 3600L + minutes * 60L + seconds;
        } catch (NumberFormatException e) {
            return warnAndFallBack(timeString);
        }
    }

    private long warnAndFallBack(String timeString) {
        plugin.getLogger()
                .warning("Invalid world.unload.time-until-unload value \"" + timeString
                        + "\" (expected HH:mm:ss). Falling back to 01:00:00.");
        return DEFAULT_SECONDS_UNTIL_UNLOAD;
    }

    @Contract("_, _ -> new")
    public static WorldUnloaderImpl of(BuildSystemPlugin plugin, BuildWorldImpl buildWorld) {
        return new WorldUnloaderImpl(plugin, buildWorld);
    }

    @Override
    public void manageUnload() {
        if (!plugin.getConfigService().current().world().unload().enabled()) {
            buildWorld.setLoaded(true);
            return;
        }

        buildWorld.setLoaded(buildWorld.getWorld().isPresent());
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
        Optional<World> optionalWorld = buildWorld.getWorld();
        if (optionalWorld.isEmpty()) {
            return;
        }
        World bukkitWorld = optionalWorld.get();

        if (!bukkitWorld.getPlayers().isEmpty()) {
            resetUnloadTask();
            return;
        }

        if (plugin.getConfigService()
                        .current()
                        .world()
                        .unload()
                        .blacklistedWorlds()
                        .contains(buildWorld.getName())
                || isSpawnWorld(bukkitWorld)) {
            return;
        }

        forceUnload(SaveBehavior.SAVE);
    }

    @Override
    public void forceUnload(SaveBehavior saveBehavior) {
        boolean save = saveBehavior.savesToDisk();
        BuildWorldUnloadEvent unloadEvent = new BuildWorldUnloadEvent(buildWorld);
        Bukkit.getServer().getPluginManager().callEvent(unloadEvent);
        if (unloadEvent.isCancelled()) {
            return;
        }

        this.buildWorld.getData().setLastUnloaded(System.currentTimeMillis());
        this.buildWorld.setLoaded(false);
        this.unloadTask = null;

        Optional<World> optionalWorld = this.buildWorld.getWorld();
        if (optionalWorld.isEmpty()) {
            return;
        }
        World bukkitWorld = optionalWorld.get();

        if (save) {
            Arrays.stream(bukkitWorld.getLoadedChunks()).forEach(Chunk::unload);
            bukkitWorld.save();
        }

        if (!Bukkit.unloadWorld(bukkitWorld, save)) {
            plugin.getLogger()
                    .warning("Failed to unload world \"" + this.buildWorld.getName()
                            + "\". It may still be loaded in memory.");
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
