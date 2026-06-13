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
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.world.creation.BukkitWorldFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

/**
 * Loads persisted {@link BuildWorld}s at startup: reads them from storage, assigns them to their folders, and
 * pre-loads the backing Bukkit worlds that should be loaded immediately.
 */
@NullMarked
public class WorldLoadBootstrap {

    private final BuildSystemPlugin plugin;
    private final WorldStorageImpl worldStorage;
    private final FolderStorageImpl folderStorage;

    public WorldLoadBootstrap(
            BuildSystemPlugin plugin, WorldStorageImpl worldStorage, FolderStorageImpl folderStorage) {
        this.plugin = plugin;
        this.worldStorage = worldStorage;
        this.folderStorage = folderStorage;
    }

    public void loadWorlds() {
        this.worldStorage
                .load()
                .thenAccept(worlds -> Bukkit.getScheduler().runTask(plugin, () -> {
                    worlds.forEach(worldStorage::addBuildWorld);
                    assignWorldsToFolders();

                    boolean loadAllWorlds = !plugin.getConfigService()
                            .current()
                            .world()
                            .unload()
                            .enabled();
                    if (loadAllWorlds) {
                        plugin.getLogger().info("*** All worlds will be loaded now ***");
                    }

                    List<BuildWorld> notLoaded = new ArrayList<>();
                    worldStorage.getBuildWorlds().forEach(buildWorld -> {
                        if (preLoadWorld(buildWorld, loadAllWorlds) == LoadResult.FAILED) {
                            notLoaded.add(buildWorld);
                        }
                    });
                    notLoaded.forEach(worldStorage::removeBuildWorld);

                    plugin.getLogger().info("Loaded " + worlds.size() + " worlds from storage");
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load worlds from storage", throwable);
                    return null;
                });
    }

    /**
     * Assigns all {@link BuildWorld}s to their respective {@link Folder}s, dropping references to worlds that no longer
     * exist.
     *
     * <p>Must be called after all folders and worlds have been loaded.
     */
    private void assignWorldsToFolders() {
        folderStorage.getFolders().forEach(folder -> {
            List<UUID> invalidWorlds = new ArrayList<>();
            folder.getWorldUUIDs().stream()
                    .map(worldUUID -> {
                        BuildWorld buildWorld = worldStorage.getBuildWorld(worldUUID);
                        if (buildWorld == null) {
                            invalidWorlds.add(worldUUID);
                            plugin.getLogger()
                                    .warning("World with UUID " + worldUUID + " does not exist. Removing from folder: "
                                            + folder.getName());
                        }
                        return buildWorld;
                    })
                    .filter(Objects::nonNull)
                    .forEach(buildWorld -> buildWorld.setFolder(folder));
            invalidWorlds.forEach(folder::removeWorld);
        });
    }

    /**
     * Attempts to load the Bukkit world backing the given {@link BuildWorld} at startup.
     *
     * @param buildWorld The world to load
     * @param alwaysLoad Whether the world should always be loaded, regardless of being blacklisted
     * @return The result of the load attempt
     */
    private LoadResult preLoadWorld(BuildWorld buildWorld, boolean alwaysLoad) {
        String worldName = buildWorld.getName();
        boolean shouldPreLoad = alwaysLoad
                || plugin.getConfigService()
                        .current()
                        .world()
                        .unload()
                        .blacklistedWorlds()
                        .contains(worldName);
        if (!shouldPreLoad) {
            return LoadResult.NOT_LOADED;
        }

        World world = new BukkitWorldFactory(plugin, buildWorld).generate(BukkitWorldFactory.VersionCheck.REQUIRED);
        if (world == null) {
            return LoadResult.FAILED;
        }

        buildWorld.getData().lastLoaded().set(System.currentTimeMillis());
        plugin.getLogger().info("✔ World loaded: " + worldName);
        return LoadResult.LOADED;
    }

    private enum LoadResult {

        /**
         * The {@link BuildWorld} was successfully loaded.
         */
        LOADED,

        /**
         * The {@link BuildWorld} was attempted to be loaded, but failed.
         */
        FAILED,

        /**
         * Not attempted: unloading is enabled and the world is not blacklisted, so it may stay unloaded.
         */
        NOT_LOADED
    }
}
