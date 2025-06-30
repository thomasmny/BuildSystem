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
package de.eintosti.buildsystem.world;

import com.cryptomorin.xseries.XSound;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.api.world.BuildWorld;
import de.eintosti.buildsystem.api.world.WorldService;
import de.eintosti.buildsystem.api.world.builder.Builder;
import de.eintosti.buildsystem.api.world.creation.generator.Generator;
import de.eintosti.buildsystem.api.world.data.BuildWorldType;
import de.eintosti.buildsystem.api.world.display.Folder;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.storage.FolderStorageImpl;
import de.eintosti.buildsystem.storage.WorldStorageImpl;
import de.eintosti.buildsystem.storage.factory.FolderStorageFactory;
import de.eintosti.buildsystem.storage.factory.WorldStorageFactory;
import de.eintosti.buildsystem.util.FileUtils;
import de.eintosti.buildsystem.util.PlayerChatInput;
import de.eintosti.buildsystem.util.StringCleaner;
import de.eintosti.buildsystem.world.creation.BuildWorldCreatorImpl;
import de.eintosti.buildsystem.world.creation.generator.CustomGeneratorImpl;
import io.papermc.lib.PaperLib;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class WorldServiceImpl implements WorldService {

    private static boolean importingAllWorlds = false;

    private final BuildSystemPlugin plugin;
    private final WorldStorageImpl worldStorage;
    private final FolderStorageImpl folderStorage;

    public WorldServiceImpl(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.worldStorage = new WorldStorageFactory(plugin).createStorage();
        this.folderStorage = new FolderStorageFactory(plugin).createStorage();
    }

    public void init() {
        this.worldStorage.loadWorlds();
        this.folderStorage.loadFolders();
    }

    @Override
    public WorldStorageImpl getWorldStorage() {
        return worldStorage;
    }

    @Override
    public FolderStorageImpl getFolderStorage() {
        return folderStorage;
    }

    public void startWorldNameInput(Player player, BuildWorldType worldType, @Nullable String template, boolean privateWorld, @Nullable Folder folder) {
        player.closeInventory();
        new PlayerChatInput(plugin, player, "enter_world_name", input -> {
            if (StringCleaner.hasInvalidNameCharacters(input)) {
                Messages.sendMessage(player, "worlds_world_creation_invalid_characters");
            }

            String worldName = StringCleaner.sanitize(input);
            if (worldName.isEmpty()) {
                Messages.sendMessage(player, "worlds_world_creation_name_bank");
                return;
            }

            if (worldType == BuildWorldType.CUSTOM) {
                startCustomGeneratorInput(player, worldName, template, privateWorld, folder);
            } else {
                worldStorage.createBuildWorld(worldName)
                        .setType(worldType)
                        .setTemplate(template)
                        .setPrivate(privateWorld)
                        .setFolder(folder)
                        .createWorld(player);
            }
        });
    }

    private void startCustomGeneratorInput(Player player, String worldName, @Nullable String template, boolean privateWorld, @Nullable Folder folder) {
        new PlayerChatInput(plugin, player, "enter_generator_name", input -> {
            String[] generatorInfo = input.split(":");
            if (generatorInfo.length == 1) {
                generatorInfo = new String[]{generatorInfo[0], generatorInfo[0]};
            }

            ChunkGenerator chunkGenerator = getChunkGenerator(generatorInfo[0], generatorInfo[1], worldName);
            if (chunkGenerator == null) {
                Messages.sendMessage(player, "worlds_import_unknown_generator");
                XSound.ENTITY_ITEM_BREAK.play(player);
                return;
            }

            CustomGeneratorImpl customGenerator = new CustomGeneratorImpl(generatorInfo[0], chunkGenerator);
            worldStorage.createBuildWorld(worldName)
                    .setType(BuildWorldType.CUSTOM)
                    .setTemplate(template)
                    .setPrivate(privateWorld)
                    .setCustomGenerator(customGenerator)
                    .setFolder(folder)
                    .createWorld(player);
        });
    }

    @Nullable
    public ChunkGenerator getChunkGenerator(@Nullable String generator, String generatorId, String worldName) {
        if (generator == null) {
            return null;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(generator);
        if (plugin == null) {
            return null;
        }

        return plugin.getDefaultWorldGenerator(worldName, generatorId);
    }

    public boolean importWorld(Player player, String worldName, @Nullable Builder creator, BuildWorldType worldType, Generator generator, String generatorName, boolean single) {
        ChunkGenerator chunkGenerator = null;
        if (generator == Generator.CUSTOM) {
            String[] generatorInfo = generatorName.split(":");
            if (generatorInfo.length == 1) {
                generatorInfo = new String[]{generatorInfo[0], generatorInfo[0]};
            }

            chunkGenerator = getChunkGenerator(generatorInfo[0], generatorInfo[1], worldName);
            if (chunkGenerator == null) {
                Messages.sendMessage(player, "worlds_import_unknown_generator");
                return false;
            }
        }

        BuildWorldCreatorImpl worldCreator = worldStorage.createBuildWorld(worldName)
                .setType(worldType)
                .setCreator(creator)
                .setCustomGenerator(new CustomGeneratorImpl(generatorName, chunkGenerator))
                .setPrivate(false)
                .setCreationDate(FileUtils.getDirectoryCreation(new File(Bukkit.getWorldContainer(), worldName)));

        if (worldCreator.isDataVersionTooHigh()) {
            String key = single ? "import" : "importall";
            Messages.sendMessage(player, "worlds_" + key + "_newer_version",
                    Map.entry("%world%", worldName)
            );
            return false;
        }

        worldCreator.importWorld(player, single);
        return true;
    }

    public void importWorlds(Player player, String[] worldList, Generator generator, @Nullable Builder creator) {
        int worlds = worldList.length;
        int delay = Config.World.importAllDelay;

        Messages.sendMessage(player, "worlds_importall_started",
                Map.entry("%amount%", String.valueOf(worlds))
        );
        Messages.sendMessage(player, "worlds_importall_delay",
                Map.entry("%delay%", String.valueOf(delay))
        );
        importingAllWorlds = true;

        AtomicInteger worldsImported = new AtomicInteger(0);
        new BukkitRunnable() {
            @Override
            public void run() {
                int i = worldsImported.getAndIncrement();
                if (i >= worlds) {
                    this.cancel();
                    importingAllWorlds = false;
                    Messages.sendMessage(player, "worlds_importall_finished");
                    return;
                }

                String worldName = worldList[i];
                if (worldStorage.worldExists(worldName)) {
                    Messages.sendMessage(player, "worlds_importall_world_already_imported",
                            Map.entry("%world%", worldName)
                    );
                    return;
                }

                String invalidChar = StringCleaner.firstInvalidChar(worldName);
                if (invalidChar != null) {
                    Messages.sendMessage(player, "worlds_importall_invalid_character",
                            Map.entry("%world%", worldName),
                            Map.entry("%char%", invalidChar)
                    );
                    return;
                }

                if (importWorld(player, worldName, creator, BuildWorldType.IMPORTED, generator, "void", false)) {
                    Messages.sendMessage(player, "worlds_importall_world_imported", Map.entry("%world%", worldName));
                }
            }
        }.runTaskTimer(plugin, 0, 20L * delay);
    }

    public boolean isImportingAllWorlds() {
        return importingAllWorlds;
    }

    /**
     * Unimport an existing {@link BuildWorld}. In comparison to {@link #deleteWorld(Player, BuildWorld)}, unimporting a world does not delete the world's directory.
     *
     * @param buildWorld The build world object
     * @param save       Whether to save the world before unloading
     */
    public CompletableFuture<Void> unimportWorld(BuildWorld buildWorld, boolean save) {
        buildWorld.getUnloader().forceUnload(save);
        this.worldStorage.removeBuildWorld(buildWorld);
        removePlayersFromWorld(buildWorld.getName(), "worlds_unimport_players_world");
        return this.worldStorage.delete(buildWorld);
    }

    /**
     * Delete an existing {@link BuildWorld}. In comparison to {@link #unimportWorld(BuildWorld, boolean)}, deleting a world deletes the world's directory.
     *
     * @param player     The player who issued the deletion
     * @param buildWorld The world to be deleted
     */
    public CompletableFuture<Void> deleteWorld(Player player, BuildWorld buildWorld) {
        if (!this.worldStorage.worldExists(buildWorld.getName())) {
            Messages.sendMessage(player, "worlds_delete_unknown_world");
            return CompletableFuture.completedFuture(null);
        }

        String worldName = buildWorld.getName();
        File deleteFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!deleteFolder.exists()) {
            Messages.sendMessage(player, "worlds_delete_unknown_directory");
            return CompletableFuture.completedFuture(null);
        }

        Folder assignedFolder = this.folderStorage.getAssignedFolder(buildWorld);
        if (assignedFolder != null) {
            assignedFolder.removeWorld(buildWorld);
        }

        Messages.sendMessage(player, "worlds_delete_started", Map.entry("%world%", worldName));
        removePlayersFromWorld(worldName, "worlds_delete_players_world");
        return CompletableFuture.allOf(
                unimportWorld(buildWorld, false),
                CompletableFuture.runAsync(() -> FileUtils.deleteDirectory(deleteFolder))
        );
    }

    /**
     * Change the name of a {@link BuildWorld} to a given name.
     *
     * @param player     The player who issued the world renaming
     * @param buildWorld The build world object
     * @param newName    The name the world should be renamed to
     */
    public void renameWorld(Player player, BuildWorld buildWorld, String newName) {
        player.closeInventory();
        if (worldStorage.worldAndFolderExist(newName)) {
            Messages.sendMessage(player, "worlds_world_exists");
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        String oldName = buildWorld.getName();
        if (oldName.equalsIgnoreCase(newName)) {
            Messages.sendMessage(player, "worlds_rename_same_name");
            return;
        }

        if (StringCleaner.hasInvalidNameCharacters(newName)) {
            Messages.sendMessage(player, "worlds_world_creation_invalid_characters");
        }
        String sanitizedNewName = StringCleaner.sanitize(newName);
        if (sanitizedNewName.isEmpty()) {
            Messages.sendMessage(player, "worlds_world_creation_name_bank");
            return;
        }

        if (Bukkit.getWorld(oldName) == null && !buildWorld.isLoaded()) {
            buildWorld.getLoader().load();
        }

        World oldWorld = Bukkit.getWorld(oldName);
        if (oldWorld == null) {
            Messages.sendMessage(player, "worlds_rename_unknown_world");
            return;
        }

        List<@Nullable Player> removedPlayers = removePlayersFromWorld(oldName, "worlds_rename_players_world");
        for (Chunk chunk : oldWorld.getLoadedChunks()) {
            chunk.unload(true);
        }
        Bukkit.unloadWorld(oldWorld, true);
        Bukkit.getWorlds().remove(oldWorld);
        this.worldStorage.removeBuildWorld(buildWorld);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> this.worldStorage.delete(oldName));

        File oldWorldFile = new File(Bukkit.getWorldContainer(), oldName);
        File newWorldFile = new File(Bukkit.getWorldContainer(), sanitizedNewName);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                worldStorage.delete(oldName).join(); // ensure deletion is complete before renaming
                FileUtils.copy(oldWorldFile, newWorldFile);
                FileUtils.deleteDirectory(oldWorldFile);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to rename world directory", e);
            }
        }).thenRunAsync(() -> {
            buildWorld.setName(sanitizedNewName);
            worldStorage.addBuildWorld(buildWorld);
            worldStorage.save(buildWorld);
        }).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
            World newWorld = new BuildWorldCreatorImpl(plugin, buildWorld).generateBukkitWorld(false);
            Location spawnLocation = oldWorld.getSpawnLocation();
            spawnLocation.setWorld(newWorld);

            removedPlayers.stream()
                    .filter(Objects::nonNull)
                    .forEach(pl -> PaperLib.teleportAsync(pl, spawnLocation.clone().add(0.5, 0, 0.5)));

            SpawnManager spawnManager = plugin.getSpawnManager();
            if (spawnManager.spawnExists() && Objects.equals(spawnManager.getSpawnWorld(), oldWorld)) {
                Location oldSpawn = spawnManager.getSpawn();
                Location newSpawn = new Location(
                        newWorld,
                        oldSpawn.getX(),
                        oldSpawn.getY(),
                        oldSpawn.getZ(),
                        oldSpawn.getYaw(),
                        oldSpawn.getPitch()
                );
                spawnManager.set(newSpawn, sanitizedNewName);
            }

            Messages.sendMessage(player, "worlds_rename_set",
                    Map.entry("%oldName%", oldName),
                    Map.entry("%newName%", sanitizedNewName)
            );
        }));
    }

    public List<Player> removePlayersFromWorld(String worldName, String messageKey) {
        List<Player> affectedPlayers = new ArrayList<>();

        World worldToRemove = Bukkit.getWorld(worldName);
        if (worldToRemove == null) {
            plugin.getLogger().warning("Cannot remove players from world '" + worldName + "' because the world does not exist.");
            return affectedPlayers;
        }

        World fallbackWorld = Bukkit.getWorlds().getFirst();
        Location fallbackSpawn = fallbackWorld.getHighestBlockAt(fallbackWorld.getSpawnLocation()).getLocation().add(0.5, 1, 0.5);

        SpawnManager spawnManager = plugin.getSpawnManager();

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!player.getWorld().equals(worldToRemove)) {
                return;
            }

            boolean teleported = false;

            if (spawnManager.spawnExists()) {
                // Spawn exists -> always teleport to it
                spawnManager.teleport(player);
                teleported = true;
            } else if (!fallbackWorld.equals(worldToRemove)) {
                // Spawn doesn't exist, the fallback world is usable -> teleport
                player.teleport(fallbackSpawn);
                teleported = true;
            }

            if (!teleported) {
                // No valid spawn and fallback world is the one being deleted -> kick
                spawnManager.remove();
                player.kickPlayer(Messages.getString(messageKey, player));
                return;
            }

            Messages.sendMessage(player, messageKey);
            affectedPlayers.add(player);
        });

        return affectedPlayers;
    }

    public void save() {
        this.worldStorage
                .save(this.worldStorage.getBuildWorlds())
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save world data", e);
                    throw new CompletionException("Failed to save world data", e);
                });

        this.folderStorage
                .save(this.folderStorage.getFolders())
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save folder data", e);
                    throw new CompletionException("Failed to save folder data", e);
                });
    }
}