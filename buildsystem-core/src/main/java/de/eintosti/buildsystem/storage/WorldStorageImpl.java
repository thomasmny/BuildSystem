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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.storage.WorldStorage;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.data.Visibility;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.config.Config.World.Unload;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.creation.BuildWorldCreatorImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class WorldStorageImpl implements WorldStorage {

    protected final Logger logger;
    protected final BuildSystemPlugin plugin;
    protected final WorldServiceImpl worldService;

    private final Map<UUID, BuildWorld> buildWorldsByUuid;
    private final BiMap<UUID, String> worldIdentifiers;

    public WorldStorageImpl(BuildSystemPlugin plugin, WorldServiceImpl worldService) {
        this.logger = plugin.getLogger();
        this.plugin = plugin;
        this.worldService = worldService;

        this.buildWorldsByUuid = new HashMap<>();
        this.worldIdentifiers = HashBiMap.create();
    }

    @Override
    @Nullable
    @Contract("null -> null")
    public BuildWorld getBuildWorld(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        UUID uuid = this.worldIdentifiers.inverse().get(name.toLowerCase());
        if (uuid != null) {
            return this.buildWorldsByUuid.get(uuid);
        }

        return null;
    }

    @Override
    @Nullable
    public BuildWorld getBuildWorld(World world) {
        return getBuildWorld(world.getName());
    }

    @Override
    @Nullable
    public BuildWorld getBuildWorld(UUID uuid) {
        return this.buildWorldsByUuid.get(uuid);
    }

    @Override
    @Unmodifiable
    public Collection<BuildWorld> getBuildWorlds() {
        return Collections.unmodifiableCollection(buildWorldsByUuid.values());
    }

    public void addBuildWorld(BuildWorld buildWorld) {
        this.buildWorldsByUuid.put(buildWorld.getUniqueId(), buildWorld);
        this.worldIdentifiers.put(buildWorld.getUniqueId(), buildWorld.getName().toLowerCase());
    }

    public void removeBuildWorld(BuildWorld buildWorld) {
        UUID worldId = buildWorld.getUniqueId();
        this.buildWorldsByUuid.remove(worldId);
        this.worldIdentifiers.remove(worldId);

        // Also remove world from any folder it may be in
        Folder assignedFolder = buildWorld.getFolder();
        if (assignedFolder != null) {
            assignedFolder.removeWorld(buildWorld);
        }
    }

    @Override
    public boolean worldExists(String worldName) {
        return getBuildWorld(worldName) != null;
    }

    @Override
    public boolean worldAndFolderExist(String worldName) {
        boolean worldExists = worldExists(worldName);
        if (!worldExists) {
            return false;
        }

        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        return worldFile.exists();
    }

    @Override
    @Unmodifiable
    public List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player) {
        return getBuildWorlds().stream()
                .filter(buildWorld -> buildWorld.getBuilders().isCreator(player))
                .toList();
    }

    @Override
    @Unmodifiable
    public List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player, Visibility visibility) {
        return getBuildWorldsCreatedByPlayer(player).stream()
                .filter(buildWorld -> isCorrectVisibility(buildWorld.getData().privateWorld().get(), visibility))
                .toList();
    }

    /**
     * Gets if a {@link BuildWorld}'s visibility is equal to the given visibility.
     *
     * @param privateWorld Whether the world is private
     * @param visibility   The visibility the world should have
     * @return {@code true} if the world's visibility is equal to the given visibility, otherwise {@code false}
     */
    public boolean isCorrectVisibility(boolean privateWorld, Visibility visibility) {
        return switch (visibility) {
            case PRIVATE -> privateWorld;
            case PUBLIC -> !privateWorld;
            case IGNORE -> true;
        };
    }

    public void loadWorlds() {
        load().thenAccept(worlds -> Bukkit.getScheduler().runTask(plugin, () -> {
            worlds.forEach(this::addBuildWorld);
            assignWorldsToFolders();

            boolean loadAllWorlds = !Unload.enabled;
            if (loadAllWorlds) {
                logger.info("*** All worlds will be loaded now ***");
            }

            List<BuildWorld> notLoaded = new ArrayList<>();
            getBuildWorlds().forEach(buildWorld -> {
                LoadResult loadResult = loadWorld(buildWorld, loadAllWorlds);
                if (loadResult == LoadResult.FAILED) {
                    notLoaded.add(buildWorld);
                }
            });
            notLoaded.forEach(this::removeBuildWorld);

            logger.info("Loaded " + worlds.size() + " worlds from storage");
        })).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "Failed to load worlds from storage", throwable);
            return null;
        });
    }

    /**
     * Assigns all {@link BuildWorld}s to their respective {@link Folder}s.
     * <p>
     * Must be called after all folders and worlds have been loaded.
     */
    private void assignWorldsToFolders() {
        worldService.getFolderStorage().getFolders().forEach(folder -> {
                    List<UUID> invalidWorlds = new ArrayList<>();
                    folder.getWorldUUIDs().stream()
                            .map(worldUUID -> {
                                BuildWorld buildWorld = getBuildWorld(worldUUID);
                                if (buildWorld == null) {
                                    invalidWorlds.add(worldUUID);
                                    logger.warning("World with UUID " + worldUUID + " does not exist. Removing from folder: " + folder.getName());
                                }
                                return buildWorld;
                            })
                            .filter(Objects::nonNull)
                            .forEach(buildWorld -> buildWorld.setFolder(folder));
                    invalidWorlds.forEach(folder::removeWorld);
                }
        );
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
        boolean shouldPreLoad = alwaysLoad || Unload.blacklistedWorlds.contains(worldName);
        if (!shouldPreLoad) {
            return LoadResult.NOT_LOADED;
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
         * The {@link BuildWorld} was successfully loaded.
         */
        LOADED,

        /**
         * The {@link BuildWorld} was attempted to be loaded, but failed.
         */
        FAILED,

        /**
         * The {@link BuildWorld} was not attempted to be loaded because:
         * <ul>
         *   <li>{@link Unload#enabled} is set to {@code true}, and</li>
         *   <li>{@link Unload#blacklistedWorlds} does not contain the world's name (therefore, it can remain unloaded)</li>
         * </ul>
         */
        NOT_LOADED
    }
}
