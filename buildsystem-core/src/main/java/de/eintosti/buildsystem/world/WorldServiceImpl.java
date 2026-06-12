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
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import de.eintosti.buildsystem.world.lifecycle.WorldRenamer;
import de.eintosti.buildsystem.world.lifecycle.WorldUnloaderImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
        this.worldStorage = new YamlWorldStorage(plugin, this);
        this.folderStorage = new YamlFolderStorage(plugin, this.worldStorage);
    }

    public void init() {
        this.folderStorage.loadFolders();
        this.worldStorage.loadWorlds();
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
        new PlayerChatInput(plugin, player, "enter_world_name", input -> {
            if (StringCleaner.hasInvalidNameCharacters(
                    input, plugin.getConfigService().current().world().invalidCharacters())) {
                plugin.getMessages().sendMessage(player, "worlds_world_creation_invalid_characters");
            }

            String worldName = StringCleaner.sanitize(
                    input, plugin.getConfigService().current().world().invalidCharacters());
            if (worldName.isEmpty()) {
                plugin.getMessages().sendMessage(player, "worlds_world_creation_name_bank");
                return;
            }

            if (worldType == BuildWorldType.CUSTOM) {
                startCustomGeneratorInput(player, worldName, template, privateWorld, folder);
            } else {
                BuildWorld world = newWorld(worldName)
                        .type(worldType)
                        .template(template)
                        .privateWorld(privateWorld)
                        .folder(folder)
                        .creator(Builder.of(player))
                        .notify(player)
                        .build();
                if (world != null) {
                    world.getTeleporter().teleport(player);
                }
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

            BuildWorld world = newWorld(worldName)
                    .type(BuildWorldType.CUSTOM)
                    .template(template)
                    .privateWorld(privateWorld)
                    .customGenerator(customGenerator)
                    .folder(folder)
                    .creator(Builder.of(player))
                    .notify(player)
                    .build();
            if (world != null) {
                world.getTeleporter().teleport(player);
            }
        });
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
        int worlds = worldList.length;
        int delay = plugin.getConfigService().current().world().importAllDelay();

        plugin.getMessages()
                .sendMessage(player, "worlds_importall_started", Map.entry("%amount%", String.valueOf(worlds)));
        plugin.getMessages().sendMessage(player, "worlds_importall_delay", Map.entry("%delay%", String.valueOf(delay)));
        importingAllWorlds.set(true);

        AtomicInteger worldsImported = new AtomicInteger(0);
        new BukkitRunnable() {
            @Override
            public void run() {
                int i = worldsImported.getAndIncrement();
                if (i >= worlds) {
                    this.cancel();
                    importingAllWorlds.set(false);
                    plugin.getMessages().sendMessage(player, "worlds_importall_finished");
                    return;
                }

                String worldName = worldList[i];
                if (worldStorage.worldExists(worldName)) {
                    plugin.getMessages()
                            .sendMessage(
                                    player, "worlds_importall_world_already_imported", Map.entry("%world%", worldName));
                    return;
                }

                String invalidChar = StringCleaner.firstInvalidChar(
                        worldName, plugin.getConfigService().current().world().invalidCharacters());
                if (invalidChar != null) {
                    plugin.getMessages()
                            .sendMessage(
                                    player,
                                    "worlds_importall_invalid_character",
                                    Map.entry("%world%", worldName),
                                    Map.entry("%char%", invalidChar));
                    return;
                }

                if (importWorld(player, worldName, creator, BuildWorldType.IMPORTED, generator, "void", false)) {
                    plugin.getMessages()
                            .sendMessage(player, "worlds_importall_world_imported", Map.entry("%world%", worldName));
                }
            }
        }.runTaskTimer(plugin, 0, 20L * delay);
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

        int delay = plugin.getConfigService().current().world().importAllDelay();
        CompletableFuture<Integer> result = new CompletableFuture<>();
        AtomicInteger index = new AtomicInteger(0);
        AtomicInteger imported = new AtomicInteger(0);

        new BukkitRunnable() {
            @Override
            public void run() {
                int i = index.getAndIncrement();
                if (i >= directories.length) {
                    this.cancel();
                    importingAllWorlds.set(false);
                    result.complete(imported.get());
                    return;
                }

                String worldName = directories[i];
                if (worldStorage.worldExists(worldName)) {
                    return;
                }
                if (StringCleaner.firstInvalidChar(
                                worldName,
                                plugin.getConfigService().current().world().invalidCharacters())
                        != null) {
                    return;
                }

                if (importWorld(worldName).build() != null) {
                    imported.incrementAndGet();
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
                // Spawn exists -> always teleport to it
                spawnService.teleport(player);
                teleported = true;
            } else if (!fallbackWorld.equals(worldToRemove)) {
                // Spawn doesn't exist, the fallback world is usable -> teleport
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
