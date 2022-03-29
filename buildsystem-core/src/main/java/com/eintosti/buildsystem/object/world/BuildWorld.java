/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.world;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.messages.Titles;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.SpawnManager;
import com.eintosti.buildsystem.object.world.data.WorldStatus;
import com.eintosti.buildsystem.object.world.data.WorldType;
import com.eintosti.buildsystem.util.UUIDFetcher;
import com.eintosti.buildsystem.util.exception.UnexpectedEnumValueException;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author einTosti
 */
public class BuildWorld implements ConfigurationSerializable {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private String name;
    private String creator;
    private UUID creatorId;
    private final WorldType worldType;
    private final List<Builder> builders;
    private final long creationDate;

    private XMaterial material;
    private boolean privateWorld;
    private WorldStatus worldStatus;
    private String project;
    private String permission;
    private String customSpawn;

    private ChunkGenerator chunkGenerator;
    private final String chunkGeneratorName;

    private boolean physics;
    private boolean explosions;
    private boolean mobAI;
    private boolean blockBreaking;
    private boolean blockPlacement;
    private boolean blockInteractions;
    private boolean buildersEnabled;

    private long seconds;
    private boolean loaded;
    private BukkitTask unloadTask;

    public BuildWorld(
            BuildSystem plugin,
            String name,
            String creator,
            UUID creatorId,
            WorldType worldType,
            long creationDate,
            boolean privateWorld,
            String... chunkGeneratorName
    ) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.name = name;
        this.creator = creator;
        this.creatorId = creatorId;
        this.worldType = worldType;
        this.privateWorld = privateWorld;
        this.worldStatus = WorldStatus.NOT_STARTED;
        this.project = "-";
        this.permission = "-";
        this.customSpawn = null;
        this.builders = new ArrayList<>();
        this.creationDate = creationDate;

        this.physics = configValues.isWorldPhysics();
        this.explosions = configValues.isWorldExplosions();
        this.mobAI = configValues.isWorldMobAi();
        this.blockBreaking = configValues.isWorldBlockBreaking();
        this.blockPlacement = configValues.isWorldBlockPlacement();
        this.blockInteractions = configValues.isWorldBlockInteractions();
        this.buildersEnabled = isPrivate();
        this.chunkGeneratorName = (chunkGeneratorName != null && chunkGeneratorName.length > 0) ? chunkGeneratorName[0] : null;

        InventoryManager inventoryManager = plugin.getInventoryManager();
        switch (worldType) {
            case NORMAL:
                this.material = inventoryManager.getDefaultItem(WorldType.NORMAL);
                break;
            case FLAT:
                this.material = inventoryManager.getDefaultItem(WorldType.FLAT);
                break;
            case NETHER:
                this.material = inventoryManager.getDefaultItem(WorldType.NETHER);
                break;
            case END:
                this.material = inventoryManager.getDefaultItem(WorldType.END);
                break;
            case VOID:
                this.material = inventoryManager.getDefaultItem(WorldType.VOID);
                break;
            case CUSTOM:
            case TEMPLATE:
                this.material = XMaterial.FILLED_MAP;
                break;
            case IMPORTED:
                this.material = inventoryManager.getDefaultItem(WorldType.IMPORTED);
                break;
            default:
                try {
                    throw new UnexpectedEnumValueException(worldType.name());
                } catch (UnexpectedEnumValueException e) {
                    e.printStackTrace();
                }
                break;
        }

        if (privateWorld) {
            this.material = XMaterial.PLAYER_HEAD;
        }

        manageUnload();
    }

    public BuildWorld(
            BuildSystem plugin,
            String name,
            String creator,
            UUID creatorId,
            WorldType worldType,
            boolean privateWorld,
            XMaterial material,
            WorldStatus worldStatus,
            String project,
            String permission,
            long creationDate,
            boolean physics,
            boolean explosions,
            boolean mobAI,
            String customSpawn,
            boolean blockBreaking,
            boolean blockPlacement,
            boolean blockInteractions,
            boolean buildersEnabled,
            List<Builder> builders,
            ChunkGenerator chunkGenerator,
            String chunkGeneratorName
    ) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.name = name;
        this.creator = creator;
        this.creatorId = creatorId;
        this.worldType = worldType;
        this.privateWorld = privateWorld;
        this.material = material;
        this.worldStatus = worldStatus;
        this.project = project;
        this.permission = permission;
        this.creationDate = creationDate;
        this.physics = physics;
        this.explosions = explosions;
        this.mobAI = mobAI;
        this.customSpawn = customSpawn;
        this.blockBreaking = blockBreaking;
        this.blockPlacement = blockPlacement;
        this.blockInteractions = blockInteractions;
        this.buildersEnabled = buildersEnabled;
        this.builders = builders;
        this.chunkGenerator = chunkGenerator;
        this.chunkGeneratorName = chunkGeneratorName;

        manageUnload();
    }

    /**
     * Get the name of the world.
     *
     * @return The world's name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the world.
     *
     * @param name The name to set to
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the name of the player who created the world.
     * <p>
     * In older versions of the plugin, the creator was not saved which is why {@code null} can be returned.
     *
     * @return The name of the player who created the world
     */
    @Nullable
    public String getCreator() {
        return creator;
    }

    /**
     * Set the name of the creator.
     *
     * @param creator The name of the creator
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * Get the unique-id of the player who created the world.
     * <p>
     * In older versions of the plugin, the creator was not saved which is why {@code null} can be returned.
     *
     * @return The unique-id of the player who created the world
     */
    @Nullable
    public UUID getCreatorId() {
        return creatorId;
    }

    /**
     * Set the unique-id of the creator.
     *
     * @param creatorId The unique-id of the creator
     */
    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    /**
     * Save the creator's unique-id in a string which is suitable to be stored.
     *
     * @return The creator's unique-id as a string
     */
    @Nullable
    private String saveCreatorId() {
        if (creatorId != null) {
            return String.valueOf(getCreatorId());
        }

        String creator = getCreator();
        if (creator != null && !creator.equalsIgnoreCase("-")) {
            return String.valueOf(UUIDFetcher.getUUID(creator));
        } else {
            return null;
        }
    }

    /**
     * Get world's type.
     *
     * @return The {@link WorldType} of the world
     */
    public WorldType getType() {
        return worldType;
    }

    /**
     * Get the display name of a {@link WorldType}.
     *
     * @return the type's display name
     * @see BuildWorld#getType()
     */
    public String getTypeName() {
        switch (worldType) {
            case NORMAL:
                return plugin.getString("type_normal");
            case FLAT:
                return plugin.getString("type_flat");
            case NETHER:
                return plugin.getString("type_nether");
            case END:
                return plugin.getString("type_end");
            case VOID:
                return plugin.getString("type_void");
            case CUSTOM:
                return plugin.getString("type_custom");
            case TEMPLATE:
                return plugin.getString("type_template");
            case PRIVATE:
                return plugin.getString("type_private");
            default:
                return "-";
        }
    }

    /**
     * Get whether the world is a private world.
     * <p>
     * By default, private worlds cannot be modified by any player except for the creator.
     *
     * @return {@code true} if the world's visibility is set to private, otherwise {@code false}
     */
    public boolean isPrivate() {
        return privateWorld;
    }

    /**
     * Set the world's visibility.
     *
     * @param privateWorld {@code true} to make the world private, {@code false} to make the world public
     */
    public void setPrivate(boolean privateWorld) {
        this.privateWorld = privateWorld;
    }

    /**
     * Gets the material which represents the world in the navigator.
     *
     * @return The material which represents the world
     */
    public XMaterial getMaterial() {
        return material;
    }

    /**
     * Set the material which represents the world in the navigator.
     *
     * @param material The material
     */
    public void setMaterial(XMaterial material) {
        this.material = material;
    }

    /**
     * Get the world's current status.
     *
     * @return The world's status
     */
    public WorldStatus getStatus() {
        return worldStatus;
    }

    /**
     * Set the world's current status
     *
     * @param worldStatus The status to switch to
     */
    public void setStatus(WorldStatus worldStatus) {
        this.worldStatus = worldStatus;
    }

    /**
     * Get the display name of a {@link WorldStatus}.
     *
     * @return the status's display name
     * @see BuildWorld#getStatus() ()
     */
    public String getStatusName() {
        switch (worldStatus) {
            case NOT_STARTED:
                return plugin.getString("status_not_started");
            case IN_PROGRESS:
                return plugin.getString("status_in_progress");
            case ALMOST_FINISHED:
                return plugin.getString("status_almost_finished");
            case FINISHED:
                return plugin.getString("status_finished");
            case ARCHIVE:
                return plugin.getString("status_archive");
            case HIDDEN:
                return plugin.getString("status_hidden");
            default:
                return "-";
        }
    }

    /**
     * Get the short descriptive text which describes what the world is about.
     *
     * @return The world's current project
     */
    public String getProject() {
        return project;
    }

    /**
     * Set the world's short descriptive text which describes what the world is about.
     *
     * @param project The world's project
     */
    public void setProject(String project) {
        this.project = project;
    }

    /**
     * Get the permission which is required to view the world in the navigator and to enter it.
     *
     * @return The required permission
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Set the permission which is required to view the world in the navigator and to enter it.
     *
     * @param permission The required permission
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * Get the creation date of the world.
     *
     * @return The amount of milliseconds that have passed since {@code January 1, 1970 UTC}, until the world was created.
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Get the creation date in the format provided by the config.
     *
     * @return The formatted creation date
     * @see BuildWorld#getCreationDate()
     */
    public String getFormattedCreationDate() {
        return creationDate > 0 ? new SimpleDateFormat(configValues.getDateFormat()).format(creationDate) : "-";
    }

    /**
     * Get the chunk generator used to generate the world.
     *
     * @return The chunk generator used to generate the world.
     */
    public ChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }

    /**
     * Get the name of the {@link ChunkGenerator} which is used to generate the world.
     *
     * @return The generator name
     * @see BuildWorld#getChunkGenerator()
     */
    public String getChunkGeneratorName() {
        return chunkGeneratorName;
    }

    /**
     * Get whether block physics are activated in the world.
     *
     * @return {@code true} if world physics are currently enabled, otherwise {@code false}
     */
    public boolean isPhysics() {
        return physics;
    }

    /**
     * Set whether block physics are activated in the world.
     *
     * @param physics {@code true} to make activate block physics, {@code false} to disable
     */
    public void setPhysics(boolean physics) {
        this.physics = physics;
    }

    /**
     * Get whether explosions are enabled in the world.
     *
     * @return {@code true} if explosions are currently enabled, otherwise {@code false}
     */
    public boolean isExplosions() {
        return explosions;
    }

    /**
     * Set whether explosions are enabled in the world.
     *
     * @param explosions {@code true} to enable explosions, {@code false} to disable
     */
    public void setExplosions(boolean explosions) {
        this.explosions = explosions;
    }

    /**
     * Get whether mobs have their AI enabled in the world.
     *
     * @return {@code true} if all mob AIs are enabled, otherwise {@code false}
     */
    public boolean isMobAI() {
        return mobAI;
    }

    /**
     * Set whether mobs have their AI enabled in the world.
     *
     * @param mobAI {@code true} to enable mob AIs, {@code false} to disable
     */
    public void setMobAI(boolean mobAI) {
        this.mobAI = mobAI;
    }

    /**
     * Get the location where a player spawns in the world.
     *
     * @return The location as a string.
     */
    @Nullable
    public String getCustomSpawn() {
        return customSpawn;
    }

    /**
     * Set the location where a player spawns in the world.
     *
     * @param customSpawn The location object
     */
    public void setCustomSpawn(Location customSpawn) {
        this.customSpawn = customSpawn.getX() + ";" + customSpawn.getY() + ";" + customSpawn.getZ() + ";" +
                customSpawn.getYaw() + ";" + customSpawn.getPitch();
    }

    /**
     * Remove the world's custom spawn.
     */
    public void removeCustomSpawn() {
        this.customSpawn = null;
    }

    /**
     * Get whether blocks can be broken in the world.
     *
     * @return {@code true} if blocks can currently be broken, otherwise {@code false}
     */
    public boolean isBlockBreaking() {
        return blockBreaking;
    }

    /**
     * Set whether blocks can be broken in the world.
     *
     * @param blockBreaking {@code true} to enable block breaking, {@code false} to disable
     */
    public void setBlockBreaking(boolean blockBreaking) {
        this.blockBreaking = blockBreaking;
    }

    /**
     * Get whether blocks can be placed in the world.
     *
     * @return {@code true} if blocks can currently be placed, otherwise {@code false}
     */
    public boolean isBlockPlacement() {
        return blockPlacement;
    }

    /**
     * Set whether blocks can be placed in the world.
     *
     * @param blockPlacement {@code true} to enable block placement, {@code false} to disable
     */
    public void setBlockPlacement(boolean blockPlacement) {
        this.blockPlacement = blockPlacement;
    }

    /**
     * Get whether block can be interacted with in the world.
     *
     * @return {@code true} if blocks can be interacted with, otherwise {@code false}
     */
    public boolean isBlockInteractions() {
        return blockInteractions;
    }

    /**
     * Set whether blocks can be interacted with in the world.
     *
     * @param blockInteractions {@code true} to enable block interactions, {@code false} to disable
     */
    public void setBlockInteractions(boolean blockInteractions) {
        this.blockInteractions = blockInteractions;
    }

    /**
     * If enabled, only {@link Builder}s can break and place blocks in the world.
     *
     * @return {@code true} if builders-mode is currently enabled, otherwise {@code false}
     */
    public boolean isBuilders() {
        return buildersEnabled;
    }

    /**
     * Set whether on {@link Builder}s can modify the world.
     *
     * @param buildersEnabled {@code true} to disable world modification for all players who are not builders, {@code false} to enable
     */
    public void setBuilders(boolean buildersEnabled) {
        this.buildersEnabled = buildersEnabled;
    }

    /**
     * Get a list of all builders who can modify the world.
     *
     * @return the list of all builders
     */
    public List<Builder> getBuilders() {
        return builders;
    }

    /**
     * Format the {@code %builder%} placeholder.
     *
     * @return The list of builders which have been added to the given world as a string
     */
    public String getBuildersInfo() {
        String template = plugin.getString("world_item_builders_builder_template");
        List<String> builderNames = new ArrayList<>();

        if (configValues.isCreatorIsBuilder()) {
            if (getCreator() != null && !getCreator().equals("-")) {
                builderNames.add(getCreator());
            }
        }

        builderNames.addAll(getBuilderNames());

        String string = "";
        if (builderNames.isEmpty()) {
            string = template.replace("%builder%", "-").trim();
        } else {
            for (String builderName : builderNames) {
                string = string.concat(template.replace("%builder%", builderName));
            }
            string = string.trim();
        }

        return string.substring(0, string.length() - 1);
    }

    /**
     * Get a list of all {@link Builder} names
     *
     * @return A list of all builder names
     */
    public List<String> getBuilderNames() {
        return getBuilders().stream()
                .map(Builder::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get a builder by the given uuid.
     *
     * @param uuid The player's unique-id
     * @return The builder object, if any, or {@code null}
     */
    @Nullable
    public Builder getBuilder(UUID uuid) {
        return this.builders.parallelStream()
                .filter(builder -> builder.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get whether the given uuid matches that of an added builder.
     *
     * @param uuid The unique-id of the player to be checked
     * @return Whether the player is a builder
     */
    public boolean isBuilder(UUID uuid) {
        return this.builders.parallelStream().anyMatch(builder -> builder.getUuid().equals(uuid));
    }

    /**
     * Get whether the given player has been added as a {@link Builder}.
     *
     * @param player The player to be checked
     * @return Whether the {@link Player} is a builder
     * @see BuildWorld#isBuilder(UUID)
     */
    public boolean isBuilder(Player player) {
        return isBuilder(player.getUniqueId());
    }

    /**
     * Add a {@link Builder} to the current list of builders
     *
     * @param builder The builder object
     */
    public void addBuilder(Builder builder) {
        this.builders.add(builder);
    }

    /**
     * Remove a {@link Builder} from the current list of builders
     *
     * @param builder The builder object
     */
    private void removeBuilder(Builder builder) {
        this.builders.remove(builder);
    }

    /**
     * Add a {@link Builder} to the current list of builders
     *
     * @param uuid The builder's unique ID
     * @see BuildWorld#removeBuilder(Builder)
     */
    public void removeBuilder(UUID uuid) {
        removeBuilder(getBuilder(uuid));
    }

    /***
     * Save the list of {@link Builder}s in a string which is suitable to be stored.
     *
     * @return The list of builders as a string
     */
    private String saveBuilders() {
        StringBuilder builderList = new StringBuilder();
        for (Builder builder : getBuilders()) {
            builderList.append(";").append(builder.toString());
        }
        return builderList.length() > 0 ? builderList.substring(1) : builderList.toString();
    }

    /**
     * Get the time in the {@link World} linked to the build world.
     *
     * @return The world time
     */
    public String getWorldTime() {
        World bukkitWorld = Bukkit.getWorld(getName());
        if (bukkitWorld == null) {
            return "?";
        }
        return String.valueOf(bukkitWorld.getTime());
    }

    /**
     * Get whether the world has been loaded, allowing a player to enter it.
     *
     * @return {@code true} if the world is loaded, otherwise {@code false}
     */
    public boolean isLoaded() {
        return loaded;
    }

    public void manageUnload() {
        if (!configValues.isUnloadWorlds()) {
            this.loaded = true;
            return;
        }

        this.seconds = configValues.getTimeUntilUnload();
        this.loaded = (Bukkit.getWorld(name) != null);
        startUnloadTask();
    }

    public void startUnloadTask() {
        if (!configValues.isUnloadWorlds()) {
            return;
        }

        this.unloadTask = Bukkit.getScheduler().runTaskLater(plugin, this::unload, 20L * seconds);
    }

    public void resetUnloadTask() {
        if (this.unloadTask != null) {
            this.unloadTask.cancel();
        }

        startUnloadTask();
    }

    public void forceUnload() {
        if (!isLoaded()) {
            return;
        }

        World bukkitWorld = Bukkit.getWorld(name);
        if (bukkitWorld == null) {
            return;
        }

        if (configValues.getBlackListedWorldsToUnload().contains(name) || isSpawnWorld(bukkitWorld)) {
            return;
        }

        bukkitWorld.save();
        for (Chunk chunk : bukkitWorld.getLoadedChunks()) {
            chunk.unload();
        }

        Bukkit.unloadWorld(bukkitWorld, true);
        Bukkit.getWorlds().remove(bukkitWorld);
        plugin.getLogger().info("*** Unloaded world \"" + name + "\" ***");

        this.loaded = false;
        this.unloadTask = null;
    }

    private void unload() {
        World bukkitWorld = Bukkit.getWorld(name);
        if (bukkitWorld == null) {
            return;
        }

        if (!bukkitWorld.getPlayers().isEmpty()) {
            resetUnloadTask();
            return;
        }

        forceUnload();
    }

    private boolean isSpawnWorld(World bukkitWorld) {
        SpawnManager spawnManager = plugin.getSpawnManager();
        if (!spawnManager.spawnExists()) {
            return false;
        }

        return Objects.equals(spawnManager.getSpawn().getWorld(), bukkitWorld);
    }

    public void load(Player player) {
        if (isLoaded()) {
            return;
        }

        player.closeInventory();
        Titles.sendTitle(player, 5, 70, 20, " ", plugin.getString("loading_world").replace("%world%", name));

        load();
    }

    public void load() {
        if (isLoaded()) {
            return;
        }

        plugin.getLogger().info("*** Loading world \"" + name + "\" ***");
        plugin.getWorldManager().generateBukkitWorld(name, worldType, chunkGenerator);
        this.loaded = true;

        resetUnloadTask();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> world = new HashMap<>();

        world.put("creator", getCreator());
        world.put("creator-id", saveCreatorId());
        world.put("type", getType().name());
        world.put("private", isPrivate());
        world.put("item", getMaterial().name());
        world.put("status", getStatus().toString());
        world.put("project", getProject());
        world.put("permission", getPermission());
        world.put("date", getCreationDate());
        world.put("physics", isPhysics());
        world.put("explosions", isExplosions());
        world.put("mobai", isMobAI());
        world.put("block-breaking", isBlockBreaking());
        world.put("block-placement", isBlockPlacement());
        world.put("block-interactions", isBlockInteractions());
        world.put("builders-enabled", isBuilders());
        world.put("builders", saveBuilders());
        if (customSpawn != null) {
            world.put("spawn", customSpawn);
        }
        if (chunkGeneratorName != null) {
            world.put("chunk-generator", getChunkGeneratorName());
        }

        return world;
    }

    public enum Time {
        SUNRISE, NOON, NIGHT, UNKNOWN
    }
}