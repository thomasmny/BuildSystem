/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.world;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.data.WorldType;
import com.eintosti.buildsystem.object.world.generator.DeprecatedVoidGenerator;
import com.eintosti.buildsystem.object.world.generator.ModernVoidGenerator;
import com.eintosti.buildsystem.util.FileUtils;
import com.eintosti.buildsystem.util.external.PlayerChatInput;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author Trichtern
 */
public class BuildWorldCreator {

    private final BuildSystem plugin;
    private final WorldManager worldManager;

    private String worldName;
    private String template = null;
    private WorldType worldType = WorldType.NORMAL;
    private ChunkGenerator generator = null;
    private boolean privateWorld = false;
    private Difficulty difficulty;

    public BuildWorldCreator(BuildSystem plugin, @NotNull String name) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();

        setName(name);
        setDifficulty(plugin.getConfigValues().getWorldDifficulty());
    }

    public BuildWorldCreator(BuildSystem plugin, BuildWorld buildWorld) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();

        setName(buildWorld.getName());
        setType(buildWorld.getType());
        setChunkGenerator(buildWorld.getChunkGenerator());
        setPrivate(buildWorld.isPrivate());
        setDifficulty(buildWorld.getDifficulty());
    }

    public BuildWorldCreator setName(String name) {
        this.worldName = name;
        return this;
    }

    public BuildWorldCreator setTemplate(String template) {
        this.template = ChatColor.stripColor(template);
        return this;
    }

    public BuildWorldCreator setType(WorldType type) {
        this.worldType = type;
        return this;
    }

    public BuildWorldCreator setChunkGenerator(ChunkGenerator generator) {
        this.generator = generator;
        return this;
    }

    public BuildWorldCreator setPrivate(boolean privateWorld) {
        this.privateWorld = privateWorld;
        return this;
    }

    public BuildWorldCreator setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    /**
     * Depending on the {@link BuildWorld}'s {@link WorldType}, the corresponding {@link World} will be generated in a different way.
     * Then, if the creation of the world was successful and the config is set accordingly, the player is teleported to the world.
     *
     * @param player The player who is creating the world
     */
    public void createWorld(Player player) {
        switch (worldType) {
            default:
                if (!createPredefinedWorld(player)) {
                    return;
                }
                break;
            case CUSTOM:
                if (!createCustomWorld(player)) {
                    return;
                }
                break;
            case TEMPLATE:
                if (!createTemplateWorld(player)) {
                    return;
                }
                break;
        }

        if (plugin.getConfigValues().isTeleportAfterCreation()) {
            BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
            if (buildWorld == null) {
                return;
            }
            buildWorld.manageUnload();
            worldManager.teleport(player, buildWorld);
        }
    }

    /**
     * Generate a {@link BuildWorld} with a predefined generator.
     *
     * @param player The player who is creating the world
     * @return {@code true} if the world was successfully created, {@code false otherwise}
     */
    private boolean createPredefinedWorld(Player player) {
        if (worldManager.worldExists(player, worldName)) {
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
        worldManager.getBuildWorlds().add(buildWorld);

        player.sendMessage(plugin.getString("worlds_world_creation_started")
                .replace("%world%", worldName)
                .replace("%type%", worldType.getName())
        );
        finishPreparationsAndGenerate(buildWorld);
        player.sendMessage(plugin.getString("worlds_creation_finished"));
        return true;
    }

    /**
     * Generate a {@link BuildWorld} with a custom generator.
     *
     * @param player The player who is creating the world
     * @return {@code true} if the world was successfully created, {@code false otherwise}
     * @author Ein_Jojo
     */
    private boolean createCustomWorld(Player player) {
        if (worldManager.worldExists(player, worldName)) {
            return false;
        }

        new PlayerChatInput(plugin, player, "enter_generator_name", input -> {
            List<String> genArray;
            if (input.contains(":")) {
                genArray = Arrays.asList(input.split(":"));
            } else {
                genArray = Arrays.asList(input, input);
            }

            ChunkGenerator chunkGenerator = worldManager.getChunkGenerator(genArray.get(0), genArray.get(1), worldName);
            if (chunkGenerator == null) {
                player.sendMessage(plugin.getString("worlds_import_unknown_generator"));
                XSound.ENTITY_ITEM_BREAK.play(player);
                return;
            } else {
                plugin.getLogger().info("Using custom world generator: " + input);
            }

            BuildWorld buildWorld = new BuildWorld(
                    plugin,
                    worldName,
                    player.getName(),
                    player.getUniqueId(),
                    WorldType.CUSTOM,
                    System.currentTimeMillis(),
                    privateWorld,
                    input
            );
            worldManager.getBuildWorlds().add(buildWorld);

            player.sendMessage(plugin.getString("worlds_world_creation_started")
                    .replace("%world%", worldName)
                    .replace("%type%", worldType.getName()));
            generateBukkitWorld();
            player.sendMessage(plugin.getString("worlds_creation_finished"));
        });
        return true;
    }

    /**
     * Generate a {@link BuildWorld} with a template.
     *
     * @param player The player who is creating the world
     * @return {@code true} if the world was successfully created, {@code false otherwise}
     */
    private boolean createTemplateWorld(Player player) {
        boolean worldExists = worldManager.getBuildWorld(worldName) != null;
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

        BuildWorld buildWorld = new BuildWorld(
                plugin,
                worldName,
                player.getName(),
                player.getUniqueId(),
                WorldType.TEMPLATE,
                System.currentTimeMillis(),
                privateWorld
        );
        worldManager.getBuildWorlds().add(buildWorld);

        player.sendMessage(plugin.getString("worlds_template_creation_started")
                .replace("%world%", worldName)
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
        World bukkitWorld = generateBukkitWorld();

        switch (worldType) {
            case VOID:
                if (plugin.getConfigValues().isVoidBlock()) {
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
     * @return The world object
     */
    public World generateBukkitWorld() {
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
                worldCreator.environment(World.Environment.NETHER);
                break;

            case END:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(World.Environment.THE_END);
                break;

            case CUSTOM:
                if (generator != null) {
                    worldCreator.generator(generator);
                }

            default:
                worldCreator.generateStructures(true);
                bukkitWorldType = org.bukkit.WorldType.NORMAL;
                worldCreator.environment(World.Environment.NORMAL);
                break;
        }
        worldCreator.type(bukkitWorldType);

        World bukkitWorld = Bukkit.createWorld(worldCreator);
        if (bukkitWorld != null) {
            ConfigValues configValues = plugin.getConfigValues();
            bukkitWorld.setDifficulty(difficulty);
            bukkitWorld.setTime(configValues.getNoonTime());
            bukkitWorld.getWorldBorder().setSize(configValues.getWorldBorderSize());
            bukkitWorld.setKeepSpawnInMemory(configValues.isTeleportAfterCreation());
            configValues.getDefaultGameRules().forEach(bukkitWorld::setGameRuleValue);
        }

        return bukkitWorld;
    }
}