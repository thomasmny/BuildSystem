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

import de.eintosti.buildsystem.api.event.world.BuildWorldPostUnloadEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldUnloadEvent;
import de.eintosti.buildsystem.api.world.data.WorldDataKey;
import de.eintosti.buildsystem.api.world.lifecycle.SaveBehavior;
import de.eintosti.buildsystem.api.world.lifecycle.WorldUnloader;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.WorldContext;
import de.eintosti.buildsystem.world.WorldNames;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldUnloaderImpl implements WorldUnloader {

    private static final long DEFAULT_SECONDS_UNTIL_UNLOAD = 3600;

    /** The last malformed delay that was warned about, so a bad value is logged once instead of once per world. */
    private static volatile @Nullable String lastWarnedDelay;

    private final WorldContext context;
    private final BuildWorldImpl buildWorld;

    private @Nullable BukkitTask unloadTask;

    private WorldUnloaderImpl(WorldContext context, BuildWorldImpl buildWorld) {
        this.context = context;
        this.buildWorld = buildWorld;
    }

    @Contract("_, _ -> new")
    public static WorldUnloaderImpl of(WorldContext context, BuildWorldImpl buildWorld) {
        return new WorldUnloaderImpl(context, buildWorld);
    }

    private boolean unloadingEnabled() {
        return context.configService().current().world().unload().enabled();
    }

    /**
     * Parses the configured {@code HH:mm:ss} unload delay. It is read each time a task is scheduled, so a reload picks
     * up a changed value. A malformed value falls back to one hour instead of throwing.
     */
    private long secondsUntilUnload() {
        String timeString = context.configService().current().world().unload().timeUntilUnload();
        String[] timeArray = timeString.split(":");
        if (timeArray.length == 3) {
            try {
                int hours = Integer.parseInt(timeArray[0]);
                int minutes = Integer.parseInt(timeArray[1]);
                int seconds = Integer.parseInt(timeArray[2]);
                return hours * 3600L + minutes * 60L + seconds;
            } catch (NumberFormatException ignored) {
                // Falls through to the warning below.
            }
        }

        if (!timeString.equals(lastWarnedDelay)) {
            lastWarnedDelay = timeString;
            context.logger()
                    .warning("Invalid world.unload.time-until-unload value \"" + timeString
                            + "\" (expected HH:mm:ss). Falling back to 01:00:00.");
        }
        return DEFAULT_SECONDS_UNTIL_UNLOAD;
    }

    /**
     * Brings the loaded flag and the unload timer in line with the current config. Safe to call again after a reload:
     * the previous timer is cancelled first. With unloading turned off, every world is kept loaded, as at startup.
     */
    @Override
    public void manageUnload() {
        cancelScheduledTask();
        boolean present = buildWorld.getWorld().isPresent();
        if (!unloadingEnabled()) {
            buildWorld.setLoaded(present);
            if (!present) {
                buildWorld.getLoader().load();
            }
            return;
        }

        buildWorld.setLoaded(present);
        startUnloadTask();
    }

    @Override
    public void startUnloadTask() {
        if (!unloadingEnabled()) {
            return;
        }

        cancelScheduledTask();
        this.unloadTask = context.scheduler().runLater(this::unload, 20L * secondsUntilUnload());
    }

    @Override
    public void resetUnloadTask() {
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
        this.unloadTask = null;
        Optional<World> optionalWorld = buildWorld.getWorld();
        if (optionalWorld.isEmpty()) {
            return;
        }
        World bukkitWorld = optionalWorld.get();

        if (!bukkitWorld.getPlayers().isEmpty()) {
            resetUnloadTask();
            return;
        }

        if (context.configService()
                        .current()
                        .world()
                        .unload()
                        .blacklistedWorlds()
                        .contains(WorldNames.id(buildWorld.getName()))
                || context.spawnService().isIn(buildWorld.getName())) {
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

        this.buildWorld.getData().set(WorldDataKey.LAST_UNLOADED, System.currentTimeMillis());
        this.buildWorld.setLoaded(false);
        this.unloadTask = null;

        Optional<World> optionalWorld = this.buildWorld.getWorld();
        if (optionalWorld.isEmpty()) {
            return;
        }
        World bukkitWorld = optionalWorld.get();

        if (!Bukkit.unloadWorld(bukkitWorld, save)) {
            context.logger()
                    .warning("Failed to unload world \"" + this.buildWorld.getName()
                            + "\". It may still be loaded in memory.");
            return;
        }

        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPostUnloadEvent(this.buildWorld));
        context.logger().info("*** Unloaded world \"" + this.buildWorld.getName() + "\" ***");
    }
}
