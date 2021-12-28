/*
 * Copyright (c) 2021, Thomas Meaney
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
import com.eintosti.buildsystem.object.world.WorldType;
import com.eintosti.buildsystem.object.world.*;
import com.eintosti.buildsystem.util.FileUtils;
import com.eintosti.buildsystem.util.config.WorldConfig;
import com.eintosti.buildsystem.util.external.PlayerChatInput;
import com.eintosti.buildsystem.util.external.UUIDFetcher;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class WorldManager {

    private final BuildSystem plugin;
    private final WorldConfig worldConfig;
    private final List<BuildWorld> buildWorlds = new ArrayList<>();

    public HashSet<Player> createPrivateWorldPlayers;

    public WorldManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldConfig = new WorldConfig(plugin);
        this.createPrivateWorldPlayers = new HashSet<>();
    }

    public BuildWorld getBuildWorld(String worldName) {
        return this.buildWorlds.stream()
                .filter(buildWorld -> buildWorld.getName().equalsIgnoreCase(worldName))
                .findFirst()
                .orElse(null);
    }

    public List<BuildWorld> getBuildWorlds() {
        return buildWorlds;
    }

    /**
     * Gets the name of the {@link BuildWorld} the player is trying to create and removes all illegal characters.
     * If the world is going to be a private world, its name will be equal to the player's name.
     *
     * @param player       The player who is creating the world
     * @param worldType    The world type
     * @param template     The name of the template world, if any, otherwise `null`
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

            player.closeInventory();
            String worldName = input.replaceAll("[^A-Za-z0-9/_-]", "").replace(" ", "_").trim();
            manageWorldType(player, worldName, worldType, template, false);
        });
    }

    /**
     * Depending on the {@link BuildWorld}'s {@link WorldType}, the corresponding {@link World} will be generated in a different way.
     *
     * @param player       The player who is creating the world
     * @param worldName    The name of the world
     * @param worldType    The world type
     * @param template     The name of the template world. Only if the world is being created with a template, otherwise ``null
     * @param privateWorld Is world going to be a private world?
     */
    private void manageWorldType(Player player, String worldName, WorldType worldType, @Nullable String template, boolean privateWorld) {
        switch (worldType) {
            default:
                createWorld(player, worldName, worldType, privateWorld);
                break;
            case CUSTOM:
                createCustomWorld(player, worldName, privateWorld);
                break;
            case TEMPLATE:
                createTemplateWorld(player, worldName, ChatColor.stripColor(template), privateWorld);
                break;
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
     */
    public void createWorld(Player player, String worldName, WorldType worldType, boolean privateWorld) {
        if (worldExists(player, worldName)) {
            return;
        }

        BuildWorld buildWorld = new BuildWorld(plugin, worldName, player.getName(), player.getUniqueId(), worldType, System.currentTimeMillis(), privateWorld);
        buildWorlds.add(buildWorld);

        player.sendMessage(plugin.getString("worlds_world_creation_started")
                .replace("%world%", buildWorld.getName())
                .replace("%type%", buildWorld.getTypeName()));
        finishPreparationsAndGenerate(buildWorld);
        player.sendMessage(plugin.getString("worlds_creation_finished"));
    }

    /**
     * Generate a {@link BuildWorld} with a custom generator.
     *
     * @param player       The player who is creating the world
     * @param worldName    The name of the world
     * @param privateWorld Is world going to be a private world?
     * @author Ein_Jojo
     */
    public void createCustomWorld(Player player, String worldName, boolean privateWorld) {
        if (worldExists(player, worldName)) {
            return;
        }

        //Get Generator
        new PlayerChatInput(plugin, player, "enter_generator_name", input -> {
            List<String> genArray = Arrays.asList(input.split(":"));
            if (genArray.size() < 2) {
                genArray.add("");
            }

            ChunkGenerator chunkGenerator = getChunkGenerator(genArray.get(0), genArray.get(1), worldName);
            if (chunkGenerator == null) {
                player.sendMessage(plugin.getString("worlds_import_unknown_generator"));
                XSound.ENTITY_ITEM_BREAK.play(player);
                return;
            } else {
                plugin.getLogger().log(Level.INFO, "Using custom world generator: " + input);
            }

            BuildWorld buildWorld = new BuildWorld(plugin, worldName, player.getName(), player.getUniqueId(), WorldType.CUSTOM, System.currentTimeMillis(), privateWorld, input);
            buildWorlds.add(buildWorld);

            player.sendMessage(plugin.getString("worlds_world_creation_started")
                    .replace("%world%", buildWorld.getName())
                    .replace("%type%", buildWorld.getTypeName()));
            generateBukkitWorld(worldName, buildWorld.getType(), chunkGenerator);
            player.sendMessage(plugin.getString("worlds_creation_finished"));
        });
    }

    /**
     * Generate a {@link BuildWorld} with a template.
     *
     * @param player       The player who is creating the world
     * @param worldName    The name of the world
     * @param template     The name of the template world
     * @param privateWorld Is world going to be a private world?
     */
    private void createTemplateWorld(Player player, String worldName, String template, boolean privateWorld) {
        boolean worldExists = getBuildWorld(worldName) != null;
        File worldFile = new File(Bukkit.getWorldContainer(), worldName);
        if (worldExists || worldFile.exists()) {
            player.sendMessage(plugin.getString("worlds_world_exists"));
            return;
        }

        File templateFile = new File(plugin.getDataFolder() + File.separator + "templates" + File.separator + template);
        if (!templateFile.exists()) {
            player.sendMessage(plugin.getString("worlds_template_does_not_exist"));
            return;
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
                if (plugin.isVoidBlock()) {
                    bukkitWorld.getBlockAt(0, 64, 0).setType(Material.GOLD_BLOCK);
                }
                bukkitWorld.setSpawnLocation(0, 65, 0);
                break;
            case FLAT:
                int y = XMaterial.supports(18) ? -60 : 4;
                bukkitWorld.setSpawnLocation(0, y, 0);
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
                if (XMaterial.supports(13)) {
                    worldCreator.generator(new ChunkGenerator() {
                        @Override
                        @SuppressWarnings("deprecation")
                        public @NotNull ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull BiomeGrid biome) {
                            return createChunkData(world);
                        }
                    });
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
                worldCreator.environment(org.bukkit.World.Environment.NETHER);
                break;
            case END:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(org.bukkit.World.Environment.THE_END);
                break;
            default:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(org.bukkit.World.Environment.NORMAL);
                break;
        }
        worldCreator.type(bukkitWorldType);

        if (chunkGenerators != null && chunkGenerators.length > 0) {
            worldCreator.generator(chunkGenerators[0]);
        }

        World bukkitWorld = Bukkit.createWorld(worldCreator);

        if (bukkitWorld != null) {
            bukkitWorld.setDifficulty(Difficulty.valueOf(plugin.getWorldDifficulty()));
            bukkitWorld.setTime(plugin.getNoonTime());
            bukkitWorld.getWorldBorder().setSize(plugin.getWorldBorderSize());
            plugin.getDefaultGameRules().forEach(bukkitWorld::setGameRuleValue);
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
                player.sendMessage(plugin.getString("worlds_import_invalid_character").replace("%world%", worldName).replace("%char%", charString));
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
            List<String> genArray = Arrays.asList(generatorName[0].split(":"));
            if (genArray.size() < 2) {
                genArray.add("");
            }

            chunkGenerator = getChunkGenerator(genArray.get(0), genArray.get(1), worldName);
            if (chunkGenerator == null) {
                player.sendMessage(plugin.getString("worlds_import_unknown_generator"));
                return;
            }
        }

        player.sendMessage(plugin.getString("worlds_import_started").replace("%world%", worldName));
        buildWorlds.add(new BuildWorld(plugin, worldName, "-", null, WorldType.IMPORTED, FileUtils.getDirectoryCreation(file), false));

        if (chunkGenerator == null) {
            generateBukkitWorld(worldName, generator.getWorldType());
        } else {
            generateBukkitWorld(worldName, generator.getWorldType(), chunkGenerator);
        }

        player.sendMessage(plugin.getString("worlds_import_finished"));
    }

    /**
     * Import all {@link BuildWorld} from a given list of world names.
     *
     * @param player    The player who is creating the world
     * @param worldList The list of world to be imported
     */
    public void importWorlds(Player player, String[] worldList) {
        int worlds = worldList.length;
        int delay = plugin.getImportDelay();

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
     * @param buildWorld The build world object
     */
    public void deleteWorld(Player player, BuildWorld buildWorld) {
        if (!buildWorlds.contains(buildWorld)) {
            player.sendMessage(plugin.getString("worlds_delete_unknown_world"));
            return;
        }

        unimportWorld(buildWorld);

        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld != null) {
            removePlayersFromWorld(buildWorld.getName(), plugin.getString("worlds_delete_players_world"));
            Bukkit.getServer().unloadWorld(bukkitWorld, false);
            Bukkit.getWorlds().remove(bukkitWorld);
        }

        File deleteFolder = new File(Bukkit.getWorldContainer(), buildWorld.getName());
        if (!deleteFolder.exists()) {
            player.sendMessage(plugin.getString("worlds_delete_unknown_directory"));
            return;
        }

        player.sendMessage(plugin.getString("worlds_delete_started").replace("%world%", buildWorld.getName()));
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

        Bukkit.getOnlinePlayers().forEach(player -> {
            World playerWorld = player.getWorld();
            if (!playerWorld.equals(bukkitWorld)) {
                return;
            }

            Location spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5);
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
        if (plugin.isUnloadWorlds() && !buildWorld.isLoaded()) {
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
        ArrayList<Builder> builders = parseBuilders(configuration, worldName);
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
        try {
            String itemString = configuration.getString("worlds." + worldName + ".item", XMaterial.BEDROCK.toString());
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(itemString);
            if (xMaterial.isPresent()) {
                return xMaterial.get();
            } else {
                plugin.getLogger().log(Level.WARNING, "[BuildSystem] Unknown material found for \"" + worldName + "\" (" + configuration.getString("worlds." + worldName + ".item").split(":")[0] + ").");
                plugin.getLogger().log(Level.WARNING, "[BuildSystem] Material changed to BEDROCK.");
                return XMaterial.BEDROCK;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "[BuildSystem] Unknown material found for \"" + worldName + "\" (" + configuration.getString("worlds." + worldName + ".item").split(":")[0] + ").");
            plugin.getLogger().log(Level.WARNING, "[BuildSystem] Material changed to BEDROCK.");
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

    private ArrayList<Builder> parseBuilders(FileConfiguration configuration, String worldName) {
        ArrayList<Builder> builders = new ArrayList<>();

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
                List<String> genArray = new ArrayList<>(Arrays.asList(generator.split(":")));
                if (genArray.size() < 2) {
                    genArray.add("");
                }
                chunkGenerator = getChunkGenerator(genArray.get(0), genArray.get(1), worldName);
            }
        }
        return chunkGenerator;
    }
}
