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
package de.eintosti.buildsystem.storage;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.world.BuildWorldImpl;
import de.eintosti.buildsystem.world.creation.BuildWorldCreatorImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public abstract class WorldStorageImpl implements WorldStorage {

    protected final BuildSystemPlugin plugin;
    protected final Logger logger;

    private final Map<String, BuildWorld> buildWorlds;

    public WorldStorageImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        this.buildWorlds = load().stream().collect(Collectors.toMap(BuildWorld::getName, Function.identity()));
    }

    @Override
    @Nullable
    public BuildWorld getBuildWorld(String name) {
        return buildWorlds.get(name);
    }

    @Override
    @Nullable
    public BuildWorld getBuildWorld(World world) {
        return getBuildWorld(world.getName());
    }

    @Override
    @Nullable
    public BuildWorld getBuildWorld(UUID uuid) {
        return buildWorlds.values().stream()
                .filter(buildWorld -> buildWorld.getUniqueId().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Unmodifiable
    public Collection<BuildWorld> getBuildWorlds() {
        return Collections.unmodifiableCollection(buildWorlds.values());
    }

    @Override
    public void addBuildWorld(BuildWorld buildWorld) {
        buildWorlds.put(buildWorld.getName(), buildWorld);
    }

    @Override
    public void removeBuildWorld(BuildWorld buildWorld) {
        this.buildWorlds.remove(buildWorld.getName());

        // Also remove world from any folder it may be in
        Folder assignedFolder = plugin.getWorldService().getFolderStorage().getAssignedFolder(buildWorld);
        if (assignedFolder != null) {
            assignedFolder.removeWorld(buildWorld);
        }
    }

    @Override
    public boolean worldExists(String worldName) {
        return worldExists(worldName, false);
    }

    @Override
    public boolean worldExists(String worldName, boolean caseSensitive) {
        if (caseSensitive) {
            return this.buildWorlds.containsKey(worldName);
        } else {
            return this.buildWorlds.keySet().stream().anyMatch(name -> name.equalsIgnoreCase(worldName));
        }
    }

    @Override
    public boolean worldAndFolderExist(String worldName) {
        boolean worldExists = this.buildWorlds.containsKey(worldName);
        if (!worldExists) {
            return false;
        }

        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        return worldFile.exists();
    }

    @Override
    public List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player) {
        return getBuildWorlds().stream()
                .filter(buildWorld -> buildWorld.getBuilders().isCreator(player))
                .collect(Collectors.toList());
    }

    @Override
    public List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player, Visibility visibility) {
        return getBuildWorldsCreatedByPlayer(player).stream()
                .filter(buildWorld -> isCorrectVisibility(buildWorld.getData().privateWorld().get(), visibility))
                .collect(Collectors.toList());
    }

    /**
     * Gets if a {@link BuildWorld}'s visibility is equal to the given visibility.
     *
     * @param privateWorld Whether the world is private
     * @param visibility   The visibility the world should have
     * @return {@code true} if the world's visibility is equal to the given visibility, otherwise {@code false}
     */
    public boolean isCorrectVisibility(boolean privateWorld, Visibility visibility) {
        switch (visibility) {
            case PRIVATE:
                return privateWorld;
            case PUBLIC:
                return !privateWorld;
            case IGNORE:
                return true;
            default:
                return false;
        }
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
     * Attempts to load the {@link World} with the given {@link BuildWorldImpl}.
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

        World world = new BuildWorldCreatorImpl(plugin, buildWorld).generateBukkitWorld();
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
