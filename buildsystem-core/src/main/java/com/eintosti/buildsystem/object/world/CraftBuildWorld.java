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
import com.eintosti.buildsystem.api.event.data.BuildWorldPermissionChangeEvent;
import com.eintosti.buildsystem.api.event.data.BuildWorldProjectChangeEvent;
import com.eintosti.buildsystem.api.event.data.BuildWorldStatusChangeEvent;
import com.eintosti.buildsystem.api.event.data.BuildWorldStatusChangeEvent.Reason;
import com.eintosti.buildsystem.api.world.BuildWorld;
import com.eintosti.buildsystem.api.world.Builder;
import com.eintosti.buildsystem.api.world.WorldStatus;
import com.eintosti.buildsystem.api.world.WorldType;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.SpawnManager;
import com.eintosti.buildsystem.util.config.ConfigValues;
import com.eintosti.buildsystem.util.exception.UnexpectedEnumValueException;
import com.eintosti.buildsystem.util.external.UUIDFetcher;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author einTosti
 */
public class CraftBuildWorld implements BuildWorld, ConfigurationSerializable {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private String name;
    private String creatorName;
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

    public CraftBuildWorld(
            BuildSystem plugin,
            String name,
            String creatorName,
            UUID creatorId,
            WorldType worldType,
            long creationDate,
            boolean privateWorld,
            String... chunkGeneratorName
    ) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.name = name;
        this.creatorName = creatorName;
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

        if (configValues.isUnloadWorlds()) {
            this.seconds = configValues.getTimeUntilUnload();
            this.loaded = (Bukkit.getWorld(name) != null);
            startUnloadTask();
        } else {
            this.loaded = true;
        }
    }

    public CraftBuildWorld(
            BuildSystem plugin,
            String name,
            String creatorName,
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
        this.creatorName = creatorName;
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

        if (configValues.isUnloadWorlds()) {
            this.seconds = configValues.getTimeUntilUnload();
            this.loaded = (Bukkit.getWorld(name) != null);
            startUnloadTask();
        } else {
            this.loaded = true;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCreatorName() {
        return creatorName;
    }

    @Override
    public void setCreatorName(String name) {
        this.creatorName = name;
    }

    @Override
    public UUID getCreatorId() {
        return creatorId;
    }

    @Override
    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    /**
     * Gets the world creator as a builder.
     *
     * @return The creator as a builder
     */
    public Builder getCreator() {
        return new CraftBuilder(creatorId, creatorName);
    }

    /**
     * Saves the creator's unique-id in a string which is suitable to be stored.
     *
     * @return The creator's unique-id as a string
     */
    @Nullable
    private String saveCreatorId() {
        if (creatorId != null) {
            return String.valueOf(getCreatorId());
        }

        String creator = getCreatorName();
        if (creator != null && !creator.equalsIgnoreCase("-")) {
            return String.valueOf(UUIDFetcher.getUUID(creator));
        } else {
            return null;
        }
    }

    @Override
    public WorldType getType() {
        return worldType;
    }

    /**
     * Gets the display name of a {@link WorldType}.
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

    @Override
    public boolean isPrivate() {
        return privateWorld;
    }

    @Override
    public void setPrivate(boolean privateWorld) {
        this.privateWorld = privateWorld;
    }

    public XMaterial getXMaterial() {
        return material;
    }

    public void setXMaterial(XMaterial material) {
        this.material = material;
    }

    @Override
    public void setMaterial(Material material) {
        setXMaterial(XMaterial.matchXMaterial(material));
    }

    @Override
    public Material getMaterial() {
        return material.parseMaterial();
    }

    @Override
    public WorldStatus getStatus() {
        return worldStatus;
    }

    @Override
    public void setStatus(WorldStatus worldStatus) {
        setStatus(worldStatus, Reason.PLUGIN);
    }

    public void setStatus(WorldStatus worldStatus, Reason reason) {
        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldStatusChangeEvent(this, this.worldStatus, worldStatus, reason));
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

    @Override
    public String getProject() {
        return project;
    }

    @Override
    public void setProject(String project) {
        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldProjectChangeEvent(this, this.project, project));
        this.project = project;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public void setPermission(String permission) {
        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldPermissionChangeEvent(this, this.permission, permission));
        this.permission = permission;
    }

    @Override
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

    @Override
    public ChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }

    /**
     * Get whether block physics are activated in the world.
     *
     * @return {@code true} if world physics are currently enabled, otherwise {@code false}
     */
    public String getChunkGeneratorName() {
        return chunkGeneratorName;
    }

    @Override
    public boolean isPhysics() {
        return physics;
    }

    @Override
    public void setPhysics(boolean physics) {
        this.physics = physics;
    }

    @Override
    public boolean isExplosions() {
        return explosions;
    }

    @Override
    public void setExplosions(boolean explosions) {
        this.explosions = explosions;
    }

    @Override
    public boolean isMobAI() {
        return mobAI;
    }

    @Override
    public void setMobAI(boolean mobAI) {
        this.mobAI = mobAI;
    }

    @Override
    public boolean hasCustomSpawn() {
        return customSpawn != null;
    }

    @Override
    public Location getCustomSpawn() {
        if (!hasCustomSpawn()) {
            return null;
        }

        String[] spawnString = customSpawn.split(";");
        return new Location(
                Bukkit.getWorld(getName()),
                Double.parseDouble(spawnString[0]),
                Double.parseDouble(spawnString[1]),
                Double.parseDouble(spawnString[2]),
                Float.parseFloat(spawnString[3]),
                Float.parseFloat(spawnString[4])
        );
    }

    public String getCustomSpawnString() {
        return customSpawn;
    }

    @Override
    public void setCustomSpawn(Location customSpawn) {
        this.customSpawn = customSpawn.getX() + ";" + customSpawn.getY() + ";" + customSpawn.getZ() + ";" + customSpawn.getYaw() + ";" + customSpawn.getPitch();
    }

    @Override
    public void removeCustomSpawn() {
        this.customSpawn = null;
    }

    @Override
    public boolean isBlockBreaking() {
        return blockBreaking;
    }

    @Override
    public void setBlockBreaking(boolean blockBreaking) {
        this.blockBreaking = blockBreaking;
    }

    @Override
    public boolean isBlockPlacement() {
        return blockPlacement;
    }

    @Override
    public void setBlockPlacement(boolean blockPlacement) {
        this.blockPlacement = blockPlacement;
    }

    @Override
    public boolean isBlockInteractions() {
        return blockInteractions;
    }

    @Override
    public void setBlockInteractions(boolean blockInteractions) {
        this.blockInteractions = blockInteractions;
    }

    @Override
    public boolean isBuildersEnabled() {
        return buildersEnabled;
    }

    @Override
    public void setBuildersEnabled(boolean buildersEnabled) {
        this.buildersEnabled = buildersEnabled;
    }

    @Override
    public List<Builder> getBuilders() {
        return builders;
    }

    /**
     * Formats the {@code %builder%} placeholder.
     *
     * @return The list of builders which have been added to the given world as a string
     */
    public String getBuildersInfo() {
        String template = plugin.getString("world_item_builders_builder_template");
        List<String> builderNames = new ArrayList<>();

        if (configValues.isCreatorIsBuilder()) {
            if (creatorName != null && !creatorName.equals("-")) {
                builderNames.add(creatorName);
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
     * Gets a list of all {@link Builder} names
     *
     * @return A list of all builder names
     */
    public List<String> getBuilderNames() {
        return getBuilders().stream()
                .map(Builder::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Builder getBuilder(UUID uuid) {
        return this.builders.parallelStream()
                .filter(builder -> builder.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Builder getBuilder(Player player) {
        return getBuilder(player.getUniqueId());
    }

    @Override
    public boolean isBuilder(UUID uuid) {
        return this.builders.parallelStream().anyMatch(builder -> builder.getUuid().equals(uuid));
    }

    @Override
    public boolean isBuilder(Player player) {
        return isBuilder(player.getUniqueId());
    }

    @Override
    public void addBuilder(UUID uuid, String name) {
        this.builders.add(new CraftBuilder(uuid, name));
    }

    @Override
    public void addBuilder(Player player) {
        addBuilder(player.getUniqueId(), player.getName());
    }

    @Override
    public void removeBuilder(Builder builder) {
        this.builders.remove(builder);
    }

    @Override
    public void removeBuilder(UUID uuid) {
        removeBuilder(getBuilder(uuid));
    }

    @Override
    public void removeBuilder(Player player) {
        removeBuilder(getBuilder(player));
    }

    /**
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

    @Override
    public boolean isLoaded() {
        return loaded;
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

    @Override
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

        this.loaded = false;
        this.unloadTask = null;
    }

    @Override
    public void unload() {
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
        String subtitle = plugin.getString("loading_world").replace("%world%", name);
        Titles.sendTitle(player, "", subtitle);

        load();
    }

    @Override
    public void load() {
        if (isLoaded()) {
            return;
        }

        plugin.getLogger().log(Level.INFO, "*** Loading world \"" + name + "\" ***");
        Bukkit.createWorld(new WorldCreator(name));
        this.loaded = true;

        resetUnloadTask();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> world = new HashMap<>();

        world.put("creator", getCreatorName());
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
        world.put("builders-enabled", isBuildersEnabled());
        world.put("builders", saveBuilders());
        if (customSpawn != null) {
            world.put("spawn", getCustomSpawnString());
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