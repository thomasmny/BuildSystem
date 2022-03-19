/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.manager;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.Titles;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.Builder;
import com.eintosti.buildsystem.object.world.data.WorldStatus;
import com.eintosti.buildsystem.object.world.data.WorldType;
import com.eintosti.buildsystem.object.world.generator.DeprecatedVoidGenerator;
import com.eintosti.buildsystem.object.world.generator.Generator;
import com.eintosti.buildsystem.object.world.generator.ModernVoidGenerator;
import com.eintosti.buildsystem.util.ConfigValues;
import com.eintosti.buildsystem.util.FileUtils;
import com.eintosti.buildsystem.util.config.WorldConfig;
import com.eintosti.buildsystem.util.external.PlayerChatInput;
import com.eintosti.buildsystem.util.external.UUIDFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author einTosti
 */
public class WorldManager {

    private final BuildSystem plugin;
    private final ConfigValues configValues;
    private final WorldConfig worldConfig;

    private final List<BuildWorld> buildWorlds;

    public WorldManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();
        this.worldConfig = new WorldConfig(plugin);

        this.buildWorlds = new ArrayList<>();
    }

    /**
     * Gets the {@link BuildWorld} by the given name.
     *
     * @param worldName The name of the world
     * @return The world object if one was found, {@code null} otherwise
     */
    public BuildWorld getBuildWorld(String worldName) {
        return this.buildWorlds.stream()
                .filter(buildWorld -> buildWorld.getName().equalsIgnoreCase(worldName))
                .findFirst()
                .orElse(null);
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
     * @return A list of all worlds
     */
    public List<BuildWorld> getBuildWorlds() {
        return buildWorlds;
    }

    /**
     * Gets a list of all {@link BuildWorld}s created by the given player.
     *
     * @param player The player who created the world
     * @return A list of all worlds created by the given player.
     */
    public List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player) {
        return getBuildWorlds().stream()
                .filter(buildWorld -> buildWorld.getCreatorId() != null)
                .filter(buildWorld -> buildWorld.getCreatorId().equals(player.getUniqueId()))
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of all {@link BuildWorld}s created by the given player.
     *
     * @param player       The player who created the world
     * @param privateWorld {@code true} if to return private worlds, otherwise {@code false}
     * @return A list of all worlds created by the given player.
     */
    public List<BuildWorld> getBuildWorldsCreatedByPlayer(Player player, boolean privateWorld) {
        return getBuildWorldsCreatedByPlayer(player).stream()
                .filter(buildWorld -> isCorrectVisibility(buildWorld, privateWorld))
                .collect(Collectors.toList());
    }

    /**
     * Gets if a {@link BuildWorld}'s visibility is equal to the given visibility.
     *
     * @param buildWorld   The world object
     * @param privateWorld Should the world's visibility be equal to private?
     * @return {@code true} if the world's visibility is equal to the given visibility, otherwise {@code false}
     */
    public boolean isCorrectVisibility(BuildWorld buildWorld, boolean privateWorld) {
        if (privateWorld) {
            return buildWorld.isPrivate();
        } else {
            return !buildWorld.isPrivate();
        }
    }

    /**
     * Gets the name (and in doing so removes all illegal characters) of the {@link BuildWorld} the player is trying to create.
     * If the world is going to be a private world, its name will be equal to the player's name.
     *
     * @param player       The player who is creating the world
     * @param worldType    The world type
     * @param template     The name of the template world, if any, otherwise {@code null}
     * @param privateWorld Is world going to be a private world?
     */
    public void startWorldNameInput(Player player, WorldType worldType, @Nullable String template, boolean privateWorld) {
        if (privateWorld) {
            player.closeInventory();
            manageWorldType(player, player.getName(), worldType, template, true);
            return;
        }

        new PlayerChatInput(plugin, player, "enter_world_name", input -> {
            for (String charString : input.split("")) {
                if (charString.matches("[^A-Za-z0-9/_-]")) {
                    player.sendMessage(plugin.getString("worlds_world_creation_invalid_characters"));
                    break;
                }
            }

            String worldName = input.replaceAll("[^A-Za-z0-9/_-]", "").replace(" ", "_").trim();
            if (worldName.isEmpty()) {
                player.sendMessage(plugin.getString("worlds_world_creation_name_bank"));
                return;
            }

            player.closeInventory();
            manageWorldType(player, worldName, worldType, template, false);
        });
    }

    /**
     * Depending on the {@link BuildWorld}'s {@link WorldType}, the corresponding {@link World} will be generated in a different way.
     * Then, if the creation of the world was successful and the config is set accordingly, the player is teleported to the world.
     *
     * @param player       The player who is creating the world
     * @param worldName    The name of the world
     * @param worldType    The world type
     * @param template     The name of the template world. Only if the world is being created with a template, otherwise {@code null}
     * @param privateWorld Is world going to be a private world?
     */
    private void manageWorldType(Player player, String worldName, WorldType worldType, @Nullable String template, boolean privateWorld) {
        switch (worldType) {
            default:
                if (!createWorld(player, worldName, worldType, privateWorld)) {
                    return;
                }
                break;
            case CUSTOM:
                if (!createCustomWorld(player, worldName, privateWorld)) {
                    return;
                }
                break;
            case TEMPLATE:
                if (!createTemplateWorld(player, worldName, ChatColor.stripColor(template), privateWorld)) {
                    return;
                }
                break;
        }

        if (configValues.isTeleportAfterCreation()) {
            BuildWorld buildWorld = getBuildWorld(worldName);
            if (buildWorld == null) {
                return;
            }
            teleport(player, buildWorld);
        }
    }

    /**
     * Checks if a world with the given name already exists.
     *
     * @param player    The player who is creating the world
     * @param worldName The name of the world
     * @return Whether if a world with the given name already exists
     */
    private boolean worldExists(Player player, String worldName) {
        boolean worldExists = getBuildWorld(worldName) != null;
        File worldFile = new File(Bukkit.getWorldContainer(), worldName);

        if (worldExists || worldFile.exists()) {
            player.sendMessage(plugin.getString("worlds_world_exists"));
            XSound.ENTITY_ITEM_BREAK.play(player);
            return true;
        }

        return false;
    }

    /**
     * Generate a {@link BuildWorld} with a predefined generator.
     *
     * @param player       The player who is creating the world
     * @param worldName    The name of the world
     * @param worldType    The world type
     * @param privateWorld Is world going to be a private world?
     * @return {@code true} if the world was successfully created, {@code false otherwise}
     */
    public boolean createWorld(Player player, String worldName, WorldType worldType, boolean privateWorld) {
        if (worldExists(player, worldName)) {
            return false;
        }

        BuildWorld buildWorld = new BuildWorld(
                plugin,
                worldName,
                player.getName(),
                player.getUniqueId(),
                worldType,
                System.currentTimeMillis(),
                privateWorld
        );
        buildWorlds.add(buildWorld);

        player.sendMessage(plugin.getString("worlds_world_creation_started")
                .replace("%world%", worldName)
                .replace("%type%", buildWorld.getTypeName())
        );
        finishPreparationsAndGenerate(buildWorld);
        player.sendMessage(plugin.getString("worlds_creation_finished"));
        return true;
    }

    /**
     * Generate a {@link BuildWorld} with a custom generator.
     *
     * @param player       The player who is creating the world
     * @param worldName    The name of the world
     * @param privateWorld Is world going to be a private world?
     * @return {@code true} if the world was successfully created, {@code false otherwise}
     * @author Ein_Jojo
     */
    public boolean createCustomWorld(Player player, String worldName, boolean privateWorld) {
        if (worldExists(player, worldName)) {
            return false;
        }

        new PlayerChatInput(plugin, player, "enter_generator_name", input -> {
            List<String> genArray;
            if (input.contains(":")) {
                genArray = Arrays.asList(input.split(":"));
            } else {
                genArray = Arrays.asList(input, input);
            }

            ChunkGenerator chunkGenerator = getChunkGenerator(genArray.get(0), genArray.get(1), worldName);
            if (chunkGenerator == null) {
                player.sendMessage(plugin.getString("worlds_import_unknown_generator"));
                XSound.ENTITY_ITEM_BREAK.play(player);
                return;
            } else {
                plugin.getLogger().info("Using custom world generator: " + input);
            }

            BuildWorld buildWorld = new BuildWorld(plugin, worldName, player.getName(), player.getUniqueId(), WorldType.CUSTOM, System.currentTimeMillis(), privateWorld, input);
            buildWorlds.add(buildWorld);

            player.sendMessage(plugin.getString("worlds_world_creation_started")
                    .replace("%world%", buildWorld.getName())
                    .replace("%type%", buildWorld.getTypeName()));
            generateBukkitWorld(worldName, buildWorld.getType(), chunkGenerator);
            player.sendMessage(plugin.getString("worlds_creation_finished"));
        });
        return true;
    }

    /**
     * Generate a {@link BuildWorld} with a template.
     *
     * @param player       The player who is creating the world
     * @param worldName    The name of the world
     * @param template     The name of the template world
     * @param privateWorld Is world going to be a private world?
     * @return {@code true} if the world was successfully created, {@code false otherwise}
     */
    private boolean createTemplateWorld(Player player, String worldName, String template, boolean privateWorld) {
        boolean worldExists = getBuildWorld(worldName) != null;
        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        if (worldExists || worldFile.exists()) {
            player.sendMessage(plugin.getString("worlds_world_exists"));
            return false;
        }

        File templateFile = new File(plugin.getDataFolder() + File.separator + "templates" + File.separator + template);
        if (!templateFile.exists()) {
            player.sendMessage(plugin.getString("worlds_template_does_not_exist"));
            return false;
        }

        BuildWorld buildWorld = new BuildWorld(plugin, worldName, player.getName(), player.getUniqueId(), WorldType.TEMPLATE, System.currentTimeMillis(), privateWorld);
        buildWorlds.add(buildWorld);

        player.sendMessage(plugin.getString("worlds_template_creation_started")
                .replace("%world%", buildWorld.getName())
                .replace("%template%", template));
        FileUtils.copy(templateFile, worldFile);
        Bukkit.createWorld(WorldCreator.name(worldName)
                .type(org.bukkit.WorldType.FLAT)
                .generateStructures(false));
        player.sendMessage(plugin.getString("worlds_creation_finished"));
        return true;
    }

    /**
     * Certain {@link WorldType}s require modifications to the world after its generation.
     *
     * @param buildWorld The build world object
     */
    private void finishPreparationsAndGenerate(BuildWorld buildWorld) {
        WorldType worldType = buildWorld.getType();
        World bukkitWorld = generateBukkitWorld(buildWorld.getName(), worldType);

        switch (worldType) {
            case VOID:
                if (configValues.isVoidBlock()) {
                    bukkitWorld.getBlockAt(0, 64, 0).setType(Material.GOLD_BLOCK);
                }
                bukkitWorld.setSpawnLocation(0, 65, 0);
                break;
            case FLAT:
                int y = XMaterial.supports(18) ? -60 : 4;
                bukkitWorld.setSpawnLocation(0, y, 0);
                break;
            default:
                break;
        }
    }

    /**
     * Generate the {@link World} linked to a {@link BuildWorld}.
     *
     * @param worldName       The name of the world
     * @param worldType       The world type
     * @param chunkGenerators Custom chunk generator to be used, if any
     * @return The world object
     */
    public World generateBukkitWorld(String worldName, WorldType worldType, ChunkGenerator... chunkGenerators) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        org.bukkit.WorldType bukkitWorldType;

        switch (worldType) {
            case VOID:
                worldCreator.generateStructures(false);
                bukkitWorldType = org.bukkit.WorldType.FLAT;
                if (XMaterial.supports(17)) {
                    worldCreator.generator(new ModernVoidGenerator());
                } else if (XMaterial.supports(13)) {
                    worldCreator.generator(new DeprecatedVoidGenerator());
                } else {
                    worldCreator.generatorSettings("2;0;1");
                }
                break;

            case FLAT:
            case PRIVATE:
                worldCreator.generateStructures(false);
                bukkitWorldType = org.bukkit.WorldType.FLAT;
                break;

            case NETHER:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(Environment.NETHER);
                break;

            case END:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(Environment.THE_END);
                break;

            case CUSTOM:
                if (chunkGenerators != null && chunkGenerators.length > 0) {
                    worldCreator.generator(chunkGenerators[0]);
                }

            default:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(Environment.NORMAL);
                break;
        }
        worldCreator.type(bukkitWorldType);

        World bukkitWorld = Bukkit.createWorld(worldCreator);
        if (bukkitWorld != null) {
            bukkitWorld.setDifficulty(configValues.getWorldDifficulty());
            bukkitWorld.setTime(configValues.getNoonTime());
            bukkitWorld.getWorldBorder().setSize(configValues.getWorldBorderSize());
            bukkitWorld.setKeepSpawnInMemory(configValues.isTeleportAfterCreation());
            configValues.getDefaultGameRules().forEach(bukkitWorld::setGameRuleValue);
        }

        return bukkitWorld;
    }

    /**
     * Parse the {@link ChunkGenerator} for the generation of a {@link BuildWorld} with {@link WorldType#CUSTOM}
     *
     * @param generator   The plugin's (generator) name
     * @param generatorId Unique ID, if any, that was specified to indicate which generator was requested
     * @param worldName   Name of the world that the chunk generator should be applied to.
     */
    public ChunkGenerator getChunkGenerator(String generator, String generatorId, String worldName) {
        if (generator == null) {
            return null;
        }

        Plugin plugin = this.plugin.getServer().getPluginManager().getPlugin(generator);
        if (plugin == null) {
            return null;
        } else {
            return plugin.getDefaultWorldGenerator(worldName, generatorId);
        }
    }

    /**
     * Import a {@link BuildWorld} from a world directory.
     *
     * @param player        The player who is creating the world
     * @param worldName     Name of the world that the chunk generator should be applied to.
     * @param generator     The generator type used by the world
     * @param generatorName The name of the custom generator if generator type is {@link Generator#CUSTOM}
     */
    public void importWorld(Player player, String worldName, Generator generator, String... generatorName) {
        for (String charString : worldName.split("")) {
            if (charString.matches("[^A-Za-z0-9/_-]")) {
                player.sendMessage(plugin.getString("worlds_import_invalid_character")
                        .replace("%world%", worldName)
                        .replace("%char%", charString)
                );
                return;
            }
        }

        File file = new File(Bukkit.getWorldContainer(), worldName);
        if (!file.exists() || !file.isDirectory()) {
            player.sendMessage(plugin.getString("worlds_import_unknown_world"));
            return;
        }

        ChunkGenerator chunkGenerator = null;
        if (generator == Generator.CUSTOM) {
            List<String> genArray;
            if (generatorName[0].contains(":")) {
                genArray = Arrays.asList(generatorName[0].split(":"));
            } else {
                genArray = Arrays.asList(generatorName[0], generatorName[0]);
            }

            chunkGenerator = getChunkGenerator(genArray.get(0), genArray.get(1), worldName);
            if (chunkGenerator == null) {
                player.sendMessage(plugin.getString("worlds_import_unknown_generator"));
                return;
            }
        }

        player.sendMessage(plugin.getString("worlds_import_started").replace("%world%", worldName));
        BuildWorld buildWorld = new BuildWorld(
                plugin,
                worldName,
                "-",
                null,
                WorldType.IMPORTED,
                FileUtils.getDirectoryCreation(file),
                false
        );
        buildWorlds.add(buildWorld);
        generateBukkitWorld(worldName, generator.getWorldType(), chunkGenerator);
        player.sendMessage(plugin.getString("worlds_import_finished"));

        if (configValues.isTeleportAfterCreation()) {
            teleport(player, buildWorld);
        }
    }

    /**
     * Import all {@link BuildWorld} from a given list of world names.
     *
     * @param player    The player who is creating the world
     * @param worldList The list of world to be imported
     */
    public void importWorlds(Player player, String[] worldList) {
        int worlds = worldList.length;
        int delay = configValues.getImportDelay();

        player.sendMessage(plugin.getString("worlds_importall_started").replace("%amount%", String.valueOf(worlds)));
        player.sendMessage(plugin.getString("worlds_importall_delay").replace("%delay%", String.valueOf(delay)));

        AtomicInteger worldsImported = new AtomicInteger(0);
        new BukkitRunnable() {
            @Override
            public void run() {
                int i = worldsImported.getAndIncrement();

                String worldName = worldList[i];
                for (String charString : worldName.split("")) {
                    if (charString.matches("[^A-Za-z0-9/_-]")) {
                        player.sendMessage(plugin.getString("worlds_importall_invalid_character").replace("%world%", worldName).replace("%char%", charString));
                        return;
                    }
                }

                long creation = FileUtils.getDirectoryCreation(new File(Bukkit.getWorldContainer(), worldName));
                buildWorlds.add(new BuildWorld(plugin, worldName, "-", null, WorldType.IMPORTED, creation, false));
                generateBukkitWorld(worldName, WorldType.VOID);
                player.sendMessage(plugin.getString("worlds_importall_world_imported").replace("%world%", worldName));

                if (!(worldsImported.get() < worlds)) {
                    this.cancel();
                    player.sendMessage(plugin.getString("worlds_importall_finished"));
                }
            }
        }.runTaskTimer(plugin, 0, 20L * delay);
    }

    /**
     * Unimport an existing {@link BuildWorld}.
     * In comparison to {@link #deleteWorld(Player, BuildWorld)}, unimporting a world does not delete the world's directory.
     *
     * @param buildWorld The build world object
     */
    public void unimportWorld(BuildWorld buildWorld) {
        this.buildWorlds.remove(buildWorld);

        removePlayersFromWorld(buildWorld.getName(), plugin.getString("worlds_unimport_players_world"));
        buildWorld.forceUnload();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            this.worldConfig.getFile().set("worlds." + buildWorld.getName(), null);
            this.worldConfig.saveFile();
        });
    }

    /**
     * Delete an existing {@link BuildWorld}.
     * In comparison to {@link #unimportWorld(BuildWorld)}, deleting a world deletes the world's directory.
     *
     * @param player     The player who issued the deletion
     * @param buildWorld The world to be deleted
     */
    public void deleteWorld(Player player, BuildWorld buildWorld) {
        if (!buildWorlds.contains(buildWorld)) {
            player.sendMessage(plugin.getString("worlds_delete_unknown_world"));
            return;
        }

        String worldName = buildWorld.getName();
        if (Bukkit.getWorld(worldName) != null) {
            removePlayersFromWorld(worldName, plugin.getString("worlds_delete_players_world"));
        }

        File deleteFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!deleteFolder.exists()) {
            player.sendMessage(plugin.getString("worlds_delete_unknown_directory"));
            return;
        }

        player.sendMessage(plugin.getString("worlds_delete_started").replace("%world%", worldName));
        unimportWorld(buildWorld);
        FileUtils.deleteDirectory(deleteFolder);
        player.sendMessage(plugin.getString("worlds_delete_finished"));
    }

    /**
     * In order to properly unload/rename/delete a world, no players may be present in the {@link World}.
     * Removes all player's from the world to insure proper manipulation.
     *
     * @param worldName The name of the world
     * @param message   The message sent to a player when they are removed from the world
     */
    private void removePlayersFromWorld(String worldName, String message) {
        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            return;
        }

        SpawnManager spawnManager = plugin.getSpawnManager();
        Location spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5);

        Bukkit.getOnlinePlayers().forEach(player -> {
            World playerWorld = player.getWorld();
            if (!playerWorld.equals(bukkitWorld)) {
                return;
            }

            if (spawnManager.spawnExists()) {
                if (!spawnManager.getSpawnWorld().equals(playerWorld)) {
                    spawnManager.teleport(player);
                } else {
                    player.teleport(spawnLocation);
                    spawnManager.remove();
                }
            } else {
                player.teleport(spawnLocation);
            }

            player.sendMessage(message);
        });
    }

    /**
     * Change the name of a {@link BuildWorld} to a given name.
     *
     * @param player     The player who issued the world rename
     * @param buildWorld The build world object
     * @param newName    The name the world should be renamed to
     */
    public void renameWorld(Player player, BuildWorld buildWorld, String newName) {
        String oldName = buildWorld.getName();
        if (oldName.equalsIgnoreCase(newName)) {
            player.sendMessage(plugin.getString("worlds_rename_same_name"));
            return;
        }

        for (String charString : newName.split("")) {
            if (charString.matches("[^A-Za-z0-9/_-]")) {
                player.sendMessage(plugin.getString("worlds_world_creation_invalid_characters"));
                break;
            }
        }

        player.closeInventory();
        newName = newName.replaceAll("[^A-Za-z0-9/_-]", "").replace(" ", "_").trim();
        if (newName.isEmpty()) {
            player.sendMessage(plugin.getString("worlds_world_creation_name_bank"));
            return;
        }

        File oldWorldFile = new File(Bukkit.getWorldContainer(), buildWorld.getName());
        File newWorldFile = new File(Bukkit.getWorldContainer(), newName);
        World oldWorld = Bukkit.getWorld(buildWorld.getName());

        if (oldWorld == null) {
            player.sendMessage(plugin.getString("worlds_rename_unknown_world"));
            return;
        }

        oldWorld.save();
        FileUtils.copy(oldWorldFile, newWorldFile);

        String finalName = newName;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            worldConfig.getFile().set("worlds." + finalName, worldConfig.getFile().getConfigurationSection("worlds." + buildWorld.getName()));
            worldConfig.getFile().set("worlds." + buildWorld.getName(), null);
        });

        List<Player> players = new ArrayList<>();
        SpawnManager spawnManager = plugin.getSpawnManager();
        Location defaultSpawn = Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5);

        oldWorld.getPlayers().forEach(pl -> {
            players.add(pl);
            if (spawnManager.spawnExists()) {
                if (!Objects.equals(spawnManager.getSpawn().getWorld(), pl.getWorld())) {
                    spawnManager.teleport(pl);
                } else {
                    pl.teleport(defaultSpawn);
                }
            } else {
                pl.teleport(defaultSpawn);
            }
            pl.sendMessage(plugin.getString("worlds_rename_players_world"));
        });

        Bukkit.getServer().unloadWorld(oldWorld, false);
        File deleteFolder = oldWorld.getWorldFolder();
        FileUtils.deleteDirectory(deleteFolder);

        buildWorld.setName(newName);
        World newWorld = generateBukkitWorld(buildWorld.getName(), buildWorld.getType());
        Location spawnLocation = oldWorld.getSpawnLocation();
        spawnLocation.setWorld(newWorld);

        players.forEach(pl -> {
            if (pl != null) {
                pl.teleport(spawnLocation.add(0.5, 0, 0.5));
            }
        });
        players.clear();

        if (spawnManager.spawnExists() && Objects.equals(spawnManager.getSpawnWorld(), oldWorld)) {
            Location oldSpawn = spawnManager.getSpawn();
            Location newSpawn = new Location(spawnLocation.getWorld(), oldSpawn.getX(), oldSpawn.getY(), oldSpawn.getZ(), oldSpawn.getYaw(), oldSpawn.getPitch());
            spawnManager.set(newSpawn, newSpawn.getWorld().getName());
        }

        player.sendMessage(plugin.getString("worlds_rename_set")
                .replace("%oldName%", oldName)
                .replace("%newName%", newName));
    }

    /**
     * Teleport a player to a {@link BuildWorld}.
     *
     * @param player     The player to be teleported
     * @param buildWorld The build world object
     */
    public void teleport(Player player, BuildWorld buildWorld) {
        boolean hadToLoad = false;
        if (configValues.isUnloadWorlds() && !buildWorld.isLoaded()) {
            buildWorld.load(player);
            hadToLoad = true;
        }

        World bukkitWorld = Bukkit.getServer().getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            player.sendMessage(plugin.getString("worlds_tp_unknown_world"));
            return;
        }

        Location location = bukkitWorld.getSpawnLocation().add(0.5, 0, 0.5);
        if (buildWorld.getCustomSpawn() == null) {
            switch (buildWorld.getType()) {
                case NETHER:
                case END:
                    Location blockLocation = null;
                    for (int y = 0; y < bukkitWorld.getMaxHeight(); y++) {
                        Block block = bukkitWorld.getBlockAt(location.getBlockX(), y, location.getBlockZ());
                        if (isSafeLocation(block.getLocation())) {
                            blockLocation = block.getLocation();
                            break;
                        }
                    }
                    if (blockLocation != null) {
                        location = new Location(bukkitWorld, blockLocation.getBlockX() + 0.5, blockLocation.getBlockY() + 1, blockLocation.getBlockZ() + 0.5);
                    }
                    break;
                default:
                    break;
            }
        } else {
            String[] spawnString = buildWorld.getCustomSpawn().split(";");
            location = new Location(bukkitWorld, Double.parseDouble(spawnString[0]), Double.parseDouble(spawnString[1]), Double.parseDouble(spawnString[2]), Float.parseFloat(spawnString[3]), Float.parseFloat(spawnString[4]));
        }

        Location finalLocation = location;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.teleport(finalLocation);
            Titles.clearTitle(player);
            XSound.ENTITY_ENDERMAN_TELEPORT.play(player);
        }, hadToLoad ? 20L : 0L);
    }

    /**
     * In order to correctly teleport a player to a {@link Location}, the block underneath the player's feet must be solid.
     *
     * @param location The location the player will be teleported to
     */
    public boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        if (feet.getType() != Material.AIR && feet.getLocation().add(0, 1, 0).getBlock().getType() != Material.AIR) {
            return false;
        }

        Block head = feet.getRelative(BlockFace.UP);
        if (head.getType() != Material.AIR) {
            return false;
        }

        Block ground = feet.getRelative(BlockFace.DOWN);
        return ground.getType().isSolid();
    }

    public void save() {
        buildWorlds.forEach(worldConfig::saveWorld);
    }

    public void load() {
        FileConfiguration configuration = worldConfig.getFile();
        if (configuration == null) {
            return;
        }

        ConfigurationSection configurationSection = configuration.getConfigurationSection("worlds");
        if (configurationSection == null) {
            return;
        }

        Set<String> worlds = configurationSection.getKeys(false);
        if (worlds.isEmpty()) {
            return;
        }

        worlds.forEach(this::loadWorld);
        worldConfig.loadWorlds(this);
    }

    public BuildWorld loadWorld(String worldName) {
        FileConfiguration configuration = worldConfig.getFile();
        if (configuration == null) {
            return null;
        }

        String creator = configuration.isString("worlds." + worldName + ".creator") ? configuration.getString("worlds." + worldName + ".creator") : "-";
        UUID creatorId = parseCreatorId(configuration, worldName, creator);
        WorldType worldType = configuration.isString("worlds." + worldName + ".type") ? WorldType.valueOf(configuration.getString("worlds." + worldName + ".type")) : WorldType.UNKNOWN;
        boolean privateWorld = configuration.isBoolean("worlds." + worldName + ".private") && configuration.getBoolean("worlds." + worldName + ".private");
        XMaterial material = parseMaterial(configuration, worldName);
        WorldStatus worldStatus = WorldStatus.valueOf(configuration.getString("worlds." + worldName + ".status"));
        String project = configuration.getString("worlds." + worldName + ".project");
        String permission = configuration.getString("worlds." + worldName + ".permission");
        long date = configuration.isLong("worlds." + worldName + ".date") ? configuration.getLong("worlds." + worldName + ".date") : -1;
        boolean physics = configuration.getBoolean("worlds." + worldName + ".physics");
        boolean explosions = !configuration.isBoolean("worlds." + worldName + ".explosions") || configuration.getBoolean("worlds." + worldName + ".explosions");
        boolean mobAI = !configuration.isBoolean("worlds." + worldName + ".mobai") || configuration.getBoolean("worlds." + worldName + ".mobai");
        String customSpawn = configuration.getString("worlds." + worldName + ".spawn");
        boolean blockBreaking = !configuration.isBoolean("worlds." + worldName + ".block-breaking") || configuration.getBoolean("worlds." + worldName + ".block-breaking");
        boolean blockPlacement = !configuration.isBoolean("worlds." + worldName + ".block-placement") || configuration.getBoolean("worlds." + worldName + ".block-placement");
        boolean blockInteractions = !configuration.isBoolean("worlds." + worldName + ".block-interactions") || configuration.getBoolean("worlds." + worldName + ".block-interactions");
        boolean buildersEnabled = configuration.isBoolean("worlds." + worldName + ".builders-enabled") && configuration.getBoolean("worlds." + worldName + ".builders-enabled");
        List<Builder> builders = parseBuilders(configuration, worldName);
        String chunkGeneratorString = configuration.getString("worlds." + worldName + ".chunk-generator");
        ChunkGenerator chunkGenerator = parseChunkGenerator(configuration, worldName);

        if (worldType == WorldType.PRIVATE) {
            privateWorld = true;
            worldType = WorldType.FLAT;
        }

        BuildWorld buildWorld = new BuildWorld(
                plugin,
                worldName,
                creator,
                creatorId,
                worldType,
                privateWorld,
                material,
                worldStatus,
                project,
                permission,
                date,
                physics,
                explosions,
                mobAI,
                customSpawn,
                blockBreaking,
                blockPlacement,
                blockInteractions,
                buildersEnabled,
                builders,
                chunkGenerator,
                chunkGeneratorString
        );

        buildWorlds.add(buildWorld);
        return buildWorld;
    }

    private XMaterial parseMaterial(FileConfiguration configuration, String worldName) {
        String itemString = configuration.getString("worlds." + worldName + ".item");
        if (itemString == null) {
            itemString = XMaterial.BEDROCK.name();
            plugin.getLogger().warning("Could not find Material for \"" + worldName + "\".");
            plugin.getLogger().warning("Material changed to BEDROCK.");
        }

        Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(itemString);
        if (xMaterial.isPresent()) {
            return xMaterial.get();
        } else {
            plugin.getLogger().warning("Unknown material found for \"" + worldName + "\" (" + itemString + ").");
            plugin.getLogger().warning("Material changed to BEDROCK.");
            return XMaterial.BEDROCK;
        }
    }

    private UUID parseCreatorId(FileConfiguration configuration, String worldName, String creator) {
        String path = "worlds." + worldName + ".creator-id";
        String id = configuration.isString(path) ? configuration.getString(path) : null;

        if (id == null || id.equalsIgnoreCase("null")) {
            if (!creator.equals("-")) {
                return UUIDFetcher.getUUID(creator);
            } else {
                return null;
            }
        } else {
            return UUID.fromString(id);
        }
    }

    private List<Builder> parseBuilders(FileConfiguration configuration, String worldName) {
        List<Builder> builders = new ArrayList<>();

        if (configuration.isString("worlds." + worldName + ".builders")) {
            String buildersString = configuration.getString("worlds." + worldName + ".builders");
            if (buildersString != null && !buildersString.isEmpty()) {
                String[] splitBuilders = buildersString.split(";");
                for (String builder : splitBuilders) {
                    String[] information = builder.split(",");
                    builders.add(new Builder(UUID.fromString(information[0]), information[1]));
                }
            }
        }

        return builders;
    }

    /**
     * @author Ein_Jojo
     */
    private ChunkGenerator parseChunkGenerator(FileConfiguration configuration, String worldName) {
        ChunkGenerator chunkGenerator = null;
        if (configuration.isString("worlds." + worldName + ".chunk-generator")) {
            String generator = configuration.getString("worlds." + worldName + ".chunk-generator");

            if (generator != null && !generator.isEmpty()) {
                List<String> genArray;
                if (generator.contains(":")) {
                    genArray = Arrays.asList(generator.split(":"));
                } else {
                    genArray = Arrays.asList(generator, generator);
                }

                chunkGenerator = getChunkGenerator(genArray.get(0), genArray.get(1), worldName);
            }
        }
        return chunkGenerator;
    }
}
