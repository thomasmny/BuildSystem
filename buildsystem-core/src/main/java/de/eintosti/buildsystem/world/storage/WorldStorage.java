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
package de.eintosti.buildsystem.world.storage;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.BuildWorldCreator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public abstract class WorldStorage implements Storage<BuildWorld> {

    protected final BuildSystem plugin;
    protected final Logger logger;

    private final Map<String, BuildWorld> buildWorlds;

    public WorldStorage(BuildSystem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        this.buildWorlds = load().stream().collect(Collectors.toMap(BuildWorld::getName, Function.identity()));
        this.loadWorlds();
    }

    /**
     * Gets the {@link BuildWorld} by the given name.
     *
     * @param name The name of the world
     * @return The world object if one was found, {@code null} otherwise
     */
    @Nullable
    public BuildWorld getBuildWorld(String name) {
        return buildWorlds.get(name);
    }

    /**
     * Gets the {@link BuildWorld} by the given {@link World}.
     *
     * @param world The bukkit world object
     * @return The world object if one was found, {@code null} otherwise
     */
    public BuildWorld getBuildWorld(World world) {
        return getBuildWorld(world.getName());
    }

    /**
     * Gets a list of all {@link BuildWorld}s.
     *
     * @return An unmodifiable list of all worlds
     */
    @Unmodifiable
    public Collection<BuildWorld> getBuildWorlds() {
        return Collections.unmodifiableCollection(buildWorlds.values());
    }

    /**
     * Adds a {@link BuildWorld} to the {@link BuildWorld} map.
     *
     * @param buildWorld The {@link BuildWorld} to add
     */
    public void addBuildWorld(BuildWorld buildWorld) {
        buildWorlds.put(buildWorld.getName(), buildWorld);
    }

    /**
     * Removes a {@link BuildWorld} from the {@link BuildWorld} map.
     *
     * @param buildWorld The {@link BuildWorld} to remove
     */
    public void removeBuildWorld(BuildWorld buildWorld) {
        removeBuildWorld(buildWorld.getName());
    }

    /**
     * Removes a {@link BuildWorld} from the {@link BuildWorld} map.
     *
     * @param name The name of the {@link BuildWorld} to remove
     */
    public void removeBuildWorld(String name) {
        buildWorlds.remove(name);
    }

    /**
     * Checks if a {@link BuildWorld} with the given name (case-insensitive) exists.
     *
     * @param worldName The name of the world to check
     * @return {@code true} if the world exists, {@code false} otherwise
     */
    public boolean worldExists(String worldName) {
        return worldExists(worldName, false);
    }

    /**
     * Checks if a {@link BuildWorld} with the given name exists.
     *
     * @param worldName     The name of the world to check
     * @param caseSensitive Whether to check the name case-sensitive or not
     * @return {@code true} if the world exists, {@code false} otherwise
     */
    public boolean worldExists(String worldName, boolean caseSensitive) {
        if (caseSensitive) {
            return buildWorlds.containsKey(worldName);
        } else {
            return buildWorlds.keySet().stream().anyMatch(name -> name.equalsIgnoreCase(worldName));
        }
    }

    /**
     * Checks if a world exists in the {@link BuildWorld} map and optionally checks if the world folder exists on disk.
     *
     * @param worldName The name of the world to check
     * @return {@code true} if the world exists in the map or on disk, {@code false} otherwise
     */
    public boolean worldAndFolderExist(String worldName) {
        boolean worldExists = this.buildWorlds.containsKey(worldName);
        if (!worldExists) {
            return false;
        }

        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        return worldFile.exists();
    }

    public void loadWorlds() {
        boolean loadAllWorlds = !plugin.getConfigValues().isUnloadWorlds();
        if (loadAllWorlds) {
            logger.info("*** All worlds will be loaded now ***");
        } else {
            logger.info("*** 'Unload worlds' has been enabled in the config ***");
            logger.info("*** Therefore, worlds will not be pre-loaded ***");
        }

        List<BuildWorld> notLoaded = new ArrayList<>();
        getBuildWorlds().forEach(buildWorld -> {
            if (loadWorld(buildWorld, loadAllWorlds) == LoadResult.FAILED) {
                notLoaded.add(buildWorld);
            }
        });
        notLoaded.forEach(this::removeBuildWorld);

        if (loadAllWorlds) {
            logger.info("*** All worlds have been loaded ***");
        }
    }

    /**
     * Attempts to load the {@link World} with the given {@link BuildWorld}.
     *
     * @param buildWorld The world to load
     * @param alwaysLoad Whether the world should always be loaded, regardless of being blacklisted
     * @return The result of the load attempt
     */
    private LoadResult loadWorld(BuildWorld buildWorld, boolean alwaysLoad) {
        String worldName = buildWorld.getName();
        if (!alwaysLoad && !plugin.getConfigValues().getBlackListedWorldsToUnload().contains(worldName)) {
            return LoadResult.NOT_BLACKLISTED;
        }

        World world = new BuildWorldCreator(plugin, buildWorld).generateBukkitWorld();
        if (world == null) {
            return LoadResult.FAILED;
        }

        buildWorld.getData().lastLoaded().set(System.currentTimeMillis());
        logger.info("✔ World loaded: " + worldName);
        return LoadResult.LOADED;
    }

    private enum LoadResult {

        /**
         * The world was loaded
         */
        LOADED,

        /**
         * The world was unable to be loaded
         */
        FAILED,

        /**
         * {@link ConfigValues#isUnloadWorlds()} is enabled and the world is not blacklisted to unload
         */
        NOT_BLACKLISTED
    }
}
