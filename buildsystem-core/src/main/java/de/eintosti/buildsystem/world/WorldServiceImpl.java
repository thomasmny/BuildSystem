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

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
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
import de.eintosti.buildsystem.menu.PlayerChatInput;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.storage.yaml.YamlFolderStorage;
import de.eintosti.buildsystem.storage.yaml.YamlWorldStorage;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.world.creation.BuildWorldCreatorImpl;
import de.eintosti.buildsystem.world.creation.BukkitWorldFactory;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldRenamer;
import de.eintosti.buildsystem.world.lifecycle.WorldUnloaderImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldServiceImpl implements WorldService {

    private final AtomicBoolean importingAllWorlds = new AtomicBoolean(false);

    private final BuildSystemPlugin plugin;
    private final FolderStorageImpl folderStorage;
    private final WorldStorageImpl worldStorage;

    public WorldServiceImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldStorage = new YamlWorldStorage(plugin);
        this.folderStorage = new YamlFolderStorage(plugin, this.worldStorage);
    }

    public void init() {
        this.folderStorage.loadFolders();
        loadWorlds();
    }

    private void loadWorlds() {
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
        return new BuildWorldCreatorImpl(plugin, name);
    }

    @Override
    @Contract("_ -> new")
    public WorldImporter importWorld(String name) {
        return new BuildWorldCreatorImpl(plugin, name, true);
    }

    public void startWorldNameInput(
            Player player,
            BuildWorldType worldType,
            @Nullable String template,
            boolean privateWorld,
            @Nullable Folder folder) {
        player.closeInventory();
        PlayerChatInput.requestSanitizedName(
                plugin,
                player,
                "enter_world_name",
                "worlds_world_creation_invalid_characters",
                "worlds_world_creation_name_bank",
                worldName -> {
                    if (worldType == BuildWorldType.CUSTOM) {
                        startCustomGeneratorInput(player, worldName, template, privateWorld, folder);
                    } else {
                        buildAndTeleport(
                                player,
                                newWorld(worldName)
                                        .type(worldType)
                                        .template(template)
                                        .privateWorld(privateWorld)
                                        .folder(folder));
                    }
                });
    }

    private void startCustomGeneratorInput(
            Player player, String worldName, @Nullable String template, boolean privateWorld, @Nullable Folder folder) {
        new PlayerChatInput(plugin, player, "enter_generator_name", input -> {
            CustomGenerator customGenerator = CustomGeneratorImpl.of(input, worldName);
            if (customGenerator == null) {
                plugin.getMessages().sendMessage(player, "worlds_import_unknown_generator");
                XSound.ENTITY_ITEM_BREAK.play(player);
                return;
            }

            buildAndTeleport(
                    player,
                    newWorld(worldName)
                            .type(BuildWorldType.CUSTOM)
                            .template(template)
                            .privateWorld(privateWorld)
                            .customGenerator(customGenerator)
                            .folder(folder));
        });
    }

    private void buildAndTeleport(Player player, WorldBuilder worldBuilder) {
        BuildWorld world =
                worldBuilder.creator(Builder.of(player)).notify(player).build();
        if (world != null) {
            world.getTeleporter().teleport(player);
        }
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

        BuildWorldCreatorImpl worldCreator = new BuildWorldCreatorImpl(plugin, worldName, true);
        worldCreator
                .type(worldType)
                .creator(creator)
                .customGenerator(
                        customGenerator != null
                                ? customGenerator
                                : new CustomGeneratorImpl("BuildSystem", generatorData, null))
                .privateWorld(false)
                .creationDate(FileUtils.getDirectoryCreation(new File(Bukkit.getWorldContainer(), worldName)));

        if (worldCreator.isDataVersionTooHigh()) {
            String key = single ? "import" : "importall";
            plugin.getMessages()
                    .sendMessage(player, "worlds_" + key + "_newer_version", Map.entry("%world%", worldName));
            return false;
        }

        BuildWorld world = worldCreator.build();
        if (world == null) {
            return false;
        }
        if (single) {
            world.getTeleporter().teleport(player);
        }
        return true;
    }

    public void importWorlds(Player player, String[] worldList, Generator generator, @Nullable Builder creator) {
        int delay = plugin.getConfigService().current().world().importAllDelay();
        plugin.getMessages()
                .sendMessage(
                        player, "worlds_importall_started", Map.entry("%amount%", String.valueOf(worldList.length)));
        plugin.getMessages().sendMessage(player, "worlds_importall_delay", Map.entry("%delay%", String.valueOf(delay)));

        importingAllWorlds.set(true);
        BulkImportListener listener = new BulkImportListener() {
            @Override
            public void skippedExisting(String worldName) {
                plugin.getMessages()
                        .sendMessage(
                                player, "worlds_importall_world_already_imported", Map.entry("%world%", worldName));
            }

            @Override
            public void invalidName(String worldName, String invalidChar) {
                plugin.getMessages()
                        .sendMessage(
                                player,
                                "worlds_importall_invalid_character",
                                Map.entry("%world%", worldName),
                                Map.entry("%char%", invalidChar));
            }

            @Override
            public void imported(String worldName) {
                plugin.getMessages()
                        .sendMessage(player, "worlds_importall_world_imported", Map.entry("%world%", worldName));
            }
        };
        importStaggered(
                        worldList,
                        listener,
                        worldName -> importWorld(
                                player, worldName, creator, BuildWorldType.IMPORTED, generator, "void", false))
                .thenRun(() -> plugin.getMessages().sendMessage(player, "worlds_importall_finished"));
    }

    public boolean isImportingAllWorlds() {
        return importingAllWorlds.get();
    }

    @Override
    public CompletableFuture<Integer> importWorlds() {
        if (!importingAllWorlds.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A bulk import is already in progress"));
        }

        String[] directories = scanImportableDirectories();
        if (directories.length == 0) {
            importingAllWorlds.set(false);
            return CompletableFuture.completedFuture(0);
        }

        return importStaggered(
                directories,
                new BulkImportListener() {},
                worldName -> importWorld(worldName).build() != null);
    }

    /**
     * Per-world progress callbacks for a staggered bulk import.
     */
    private interface BulkImportListener {

        default void skippedExisting(String worldName) {}

        default void invalidName(String worldName, String invalidChar) {}

        default void imported(String worldName) {}
    }

    /**
     * Imports the given worlds one per configured delay interval, skipping names that are already imported or contain
     * invalid characters. Clears {@link #importingAllWorlds} and completes with the import count once all names have
     * been processed.
     */
    private CompletableFuture<Integer> importStaggered(
            String[] worldNames, BulkImportListener listener, Predicate<String> importer) {
        int delay = plugin.getConfigService().current().world().importAllDelay();
        String invalidCharacters = plugin.getConfigService().current().world().invalidCharacters();

        CompletableFuture<Integer> result = new CompletableFuture<>();
        AtomicInteger index = new AtomicInteger(0);
        AtomicInteger imported = new AtomicInteger(0);

        new BukkitRunnable() {
            @Override
            public void run() {
                int i = index.getAndIncrement();
                if (i >= worldNames.length) {
                    this.cancel();
                    importingAllWorlds.set(false);
                    result.complete(imported.get());
                    return;
                }

                String worldName = worldNames[i];
                if (worldStorage.worldExists(worldName)) {
                    listener.skippedExisting(worldName);
                    return;
                }

                String invalidChar = StringCleaner.firstInvalidChar(worldName, invalidCharacters);
                if (invalidChar != null) {
                    listener.invalidName(worldName, invalidChar);
                    return;
                }

                if (importer.test(worldName)) {
                    imported.incrementAndGet();
                    listener.imported(worldName);
                }
            }
        }.runTaskTimer(plugin, 0, 20L * delay);

        return result;
    }

    private String[] scanImportableDirectories() {
        String[] directories = Bukkit.getWorldContainer().list((dir, name) -> {
            File worldFolder = new File(dir, name);
            return worldFolder.isDirectory()
                    && new File(worldFolder, "level.dat").exists()
                    && !worldStorage.worldExists(name);
        });
        return directories != null ? directories : new String[0];
    }

    @Override
    public CompletableFuture<Void> unimportWorld(BuildWorld buildWorld, boolean save) {
        buildWorld.getUnloader().forceUnload(save);
        this.worldStorage.removeBuildWorld(buildWorld);
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

        buildWorld.setFolder(null);
        removePlayersFromWorld(worldName, "worlds_delete_players_world");

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
