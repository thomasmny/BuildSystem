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
package de.eintosti.buildsystem.world;

import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.api.event.world.BuildWorldDeleteEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldPostDeleteEvent;
import de.eintosti.buildsystem.api.event.world.BuildWorldUnimportEvent;
import de.eintosti.buildsystem.api.exception.WorldDeletionCancelledException;
import de.eintosti.buildsystem.api.exception.WorldDeletionException;
import de.eintosti.buildsystem.api.exception.WorldDirectoryNotFoundException;
import de.eintosti.buildsystem.api.exception.WorldNotFoundException;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldService;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.WorldBuilder;
import de.eintosti.buildsystem.api.world.creation.WorldImporter;
import de.eintosti.buildsystem.api.world.creation.generator.CustomGenerator;
import de.eintosti.buildsystem.api.world.creation.generator.Generator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.storage.yaml.YamlFolderStorage;
import de.eintosti.buildsystem.storage.yaml.YamlWorldStorage;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.world.creation.WorldBuilderImpl;
import de.eintosti.buildsystem.world.creation.WorldCreationPrompts;
import de.eintosti.buildsystem.world.creation.WorldImportCoordinator;
import de.eintosti.buildsystem.world.creation.WorldImporterImpl;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldLoadBootstrap;
import de.eintosti.buildsystem.world.lifecycle.WorldRenamer;
import de.eintosti.buildsystem.world.lifecycle.WorldUnloaderImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldServiceImpl implements WorldService {

    private final BuildSystemPlugin plugin;
    private final FolderStorageImpl folderStorage;
    private final WorldStorageImpl worldStorage;

    private final WorldLoadBootstrap loadBootstrap;
    private final WorldCreationPrompts creationPrompts;
    private final WorldImportCoordinator importCoordinator;

    public WorldServiceImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldStorage = new YamlWorldStorage(plugin);
        this.folderStorage = new YamlFolderStorage(plugin, this.worldStorage);
        this.loadBootstrap = new WorldLoadBootstrap(plugin, this.folderStorage, this.worldStorage);
        this.creationPrompts = new WorldCreationPrompts(plugin, this);
        this.importCoordinator = new WorldImportCoordinator(plugin, this, this.worldStorage);
    }

    public void init() {
        this.folderStorage.loadFolders();
        this.loadBootstrap.loadWorlds();
    }

    @Override
    public FolderStorageImpl getFolderStorage() {
        return folderStorage;
    }

    @Override
    public WorldStorageImpl getWorldStorage() {
        return worldStorage;
    }

    @Override
    @Contract("_ -> new")
    public WorldBuilder newWorld(String name) {
        return new WorldBuilderImpl(plugin, name);
    }

    @Override
    @Contract("_ -> new")
    public WorldImporter importWorld(String name) {
        return new WorldImporterImpl(plugin, name);
    }

    public void startWorldNameInput(
            Player player,
            BuildWorldType worldType,
            @Nullable String template,
            boolean privateWorld,
            @Nullable Folder folder) {
        this.creationPrompts.startWorldNameInput(player, worldType, template, privateWorld, folder);
    }

    public boolean importWorld(
            Player player,
            String worldName,
            @Nullable Builder creator,
            BuildWorldType worldType,
            Generator generator,
            String generatorData,
            boolean single) {
        CustomGenerator customGenerator = null;
        if (generator == Generator.CUSTOM) {
            customGenerator = CustomGeneratorImpl.of(generatorData, worldName);
            if (customGenerator == null) {
                plugin.getMessages().sendMessage(player, "worlds_import_unknown_generator");
                return false;
            }
        }

        WorldImporterImpl worldImporter = new WorldImporterImpl(plugin, worldName)
                .type(worldType)
                .creator(creator)
                .customGenerator(
                        customGenerator != null
                                ? customGenerator
                                : new CustomGeneratorImpl("BuildSystem", generatorData, null))
                .privateWorld(false);

        if (worldImporter.isDataVersionTooHigh()) {
            String key = single ? "import" : "importall";
            plugin.getMessages()
                    .sendMessage(player, "worlds_" + key + "_newer_version", Map.entry("%world%", worldName));
            return false;
        }

        BuildWorld world = worldImporter.build();
        if (world == null) {
            return false;
        }
        if (single) {
            world.getTeleporter().teleport(player);
        }
        return true;
    }

    public void importWorlds(Player player, String[] worldList, Generator generator, @Nullable Builder creator) {
        this.importCoordinator.importWorlds(player, worldList, generator, creator);
    }

    public boolean isImportingAllWorlds() {
        return this.importCoordinator.isImportingAllWorlds();
    }

    @Override
    public CompletableFuture<Integer> importWorlds() {
        return this.importCoordinator.importWorlds();
    }

    @Override
    public CompletableFuture<Void> unimportWorld(BuildWorld buildWorld, boolean save) {
        buildWorld.getUnloader().forceUnload(save);
        this.worldStorage.removeBuildWorld(buildWorld);
        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldUnimportEvent(buildWorld));
        removePlayersFromWorld(buildWorld.getName(), "worlds_unimport_players_world");
        return this.worldStorage.delete(buildWorld);
    }

    public void deleteWorld(Player player, BuildWorld buildWorld) {
        String worldName = buildWorld.getName();
        plugin.getMessages().sendMessage(player, "worlds_delete_started", Map.entry("%world%", worldName));
        deleteWorld(buildWorld)
                .thenRun(() -> plugin.getMessages().sendMessage(player, "worlds_delete_finished"))
                .exceptionally(e -> {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    switch (cause) {
                        case WorldNotFoundException ignored ->
                            plugin.getMessages().sendMessage(player, "worlds_delete_unknown_world");
                        case WorldDirectoryNotFoundException ignored ->
                            plugin.getMessages().sendMessage(player, "worlds_delete_unknown_directory");
                        case WorldDeletionCancelledException ignored -> {
                            // The cancelling listener is responsible for messaging the player.
                        }
                        default -> {
                            plugin.getMessages()
                                    .sendMessage(player, "worlds_delete_error", Map.entry("%world%", worldName));
                            plugin.getLogger()
                                    .log(
                                            Level.SEVERE,
                                            "An unexpected error occurred while deleting the world: " + worldName,
                                            cause);
                        }
                    }
                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> deleteWorld(BuildWorld buildWorld) {
        String worldName = buildWorld.getName();
        if (!this.worldStorage.worldExists(worldName)) {
            return CompletableFuture.failedFuture(new WorldNotFoundException(worldName));
        }

        File deleteFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!deleteFolder.exists()) {
            return CompletableFuture.failedFuture(
                    new WorldDirectoryNotFoundException(worldName, deleteFolder.getAbsolutePath()));
        }

        BuildWorldDeleteEvent deleteEvent = new BuildWorldDeleteEvent(buildWorld);
        Bukkit.getServer().getPluginManager().callEvent(deleteEvent);
        if (deleteEvent.isCancelled()) {
            return CompletableFuture.failedFuture(new WorldDeletionCancelledException(
                    "Deletion of world '" + worldName + "' was cancelled by an event listener"));
        }

        buildWorld.setFolder(null);
        removePlayersFromWorld(worldName, "worlds_delete_players_world");

        // Resolve the scheduler on the calling (main) thread so the async stage below does not touch
        // Bukkit statics from a pool thread.
        BukkitScheduler scheduler = Bukkit.getScheduler();

        // Registry removal and metadata persistence are awaited before folder deletion
        // so a crash mid-delete leaves an orphaned folder (re-importable) rather than
        // an orphaned registry entry pointing at a deleted folder.
        return unimportWorld(buildWorld, false).thenRunAsync(() -> {
            try {
                FileUtils.deleteDirectory(deleteFolder);
            } catch (IOException e) {
                throw new CompletionException(new WorldDeletionException(
                        "An unexpected error occurred during directory deletion for world: " + worldName, e));
            }
            scheduler.runTask(
                    plugin,
                    () -> Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPostDeleteEvent(buildWorld)));
        });
    }

    /**
     * Change the name of a {@link BuildWorld} to a given name.
     *
     * @param player The player who issued the world renaming
     * @param buildWorld The build world object
     * @param newName The name the world should be renamed to
     */
    public void renameWorld(Player player, BuildWorld buildWorld, String newName) {
        new WorldRenamer(plugin, this, worldStorage).rename(player, buildWorld, newName);
    }

    public List<Player> removePlayersFromWorld(String worldName, String messageKey) {
        World worldToRemove = Bukkit.getWorld(worldName);
        if (worldToRemove == null) {
            return List.of();
        }

        List<World> serverWorlds = Bukkit.getWorlds();
        if (serverWorlds.isEmpty()) {
            return List.of();
        }
        World fallbackWorld = serverWorlds.getFirst();
        Location fallbackSpawn = fallbackWorld
                .getHighestBlockAt(fallbackWorld.getSpawnLocation())
                .getLocation()
                .add(0.5, 1, 0.5);

        SpawnService spawnService = plugin.getSpawnService();
        List<Player> affectedPlayers = new ArrayList<>();

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!player.getWorld().equals(worldToRemove)) {
                return;
            }

            boolean teleported = false;

            if (spawnService.spawnExists()) {
                spawnService.teleport(player);
                teleported = true;
            } else if (!fallbackWorld.equals(worldToRemove)) {
                player.teleport(fallbackSpawn);
                teleported = true;
            }

            if (!teleported) {
                // No valid spawn and fallback world is the one being deleted -> kick
                spawnService.remove();
                player.kickPlayer(plugin.getMessages().getString(messageKey, player));
                return;
            }

            plugin.getMessages().sendMessage(player, messageKey);
            affectedPlayers.add(player);
        });

        return affectedPlayers;
    }

    public void remanageAllUnloadTasks() {
        worldStorage.getBuildWorlds().forEach(w -> w.getUnloader().manageUnload());
    }

    public void cancelAllUnloadTasks() {
        worldStorage.getBuildWorlds().forEach(w -> {
            if (w.getUnloader() instanceof WorldUnloaderImpl impl) {
                impl.cancelScheduledTask();
            }
        });
    }

    public CompletableFuture<Void> save() {
        CompletableFuture<Void> worldFuture = this.worldStorage
                .save(this.worldStorage.getBuildWorlds())
                .whenComplete((r, e) -> {
                    if (e != null) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to save world data", e);
                    }
                });

        CompletableFuture<Void> folderFuture = this.folderStorage
                .save(this.folderStorage.getFolders())
                .whenComplete((r, e) -> {
                    if (e != null) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to save folder data", e);
                    }
                });

        return CompletableFuture.allOf(worldFuture, folderFuture);
    }
}
