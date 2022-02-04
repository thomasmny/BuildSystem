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
import com.eintosti.buildsystem.api.world.BuildWorld;
import com.eintosti.buildsystem.api.world.Builder;
import com.eintosti.buildsystem.api.world.WorldStatus;
import com.eintosti.buildsystem.api.world.WorldType;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.SpawnManager;
import com.eintosti.buildsystem.util.ConfigValues;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class CraftBuildWorld implements BuildWorld, ConfigurationSerializable {

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

    private final String chunkGeneratorString;
    private ChunkGenerator chunkGenerator;

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
            String creator,
            UUID creatorId,
            WorldType worldType,
            long creationDate,
            boolean privateWorld,
            String... chunkGeneratorString
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
        this.chunkGeneratorString = (chunkGeneratorString != null && chunkGeneratorString.length > 0) ? chunkGeneratorString[0] : null;

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
                //TODO: Make an own item for custom generated worlds?
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
            String chunkGeneratorString
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
        this.chunkGeneratorString = chunkGeneratorString;

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
        return creator;
    }

    @Override
    public void setCreatorName(String creator) {
        this.creator = creator;
    }

    @Override
    public UUID getCreatorId() {
        return creatorId;
    }

    @Override
    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    private String saveCreatorId() {
        String idString;
        if (getCreatorId() == null) {
            String creator = getCreatorName();
            if (creator != null && !creator.equalsIgnoreCase("-")) {
                UUID uuid = UUIDFetcher.getUUID(creator);
                idString = String.valueOf(uuid);
            } else {
                idString = null;
            }
        } else {
            idString = String.valueOf(getCreatorId());
        }
        return idString;
    }

    @Override
    public WorldType getType() {
        return worldType;
    }

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

    @Override
    public Material getMaterial() {
        return material.parseMaterial();
    }

    @Override
    public void setMaterial(Material material) {
        this.material = XMaterial.matchXMaterial(material);
    }

    public XMaterial getXMaterial() {
        return material;
    }

    public void setXMaterial(XMaterial material) {
        this.material = material;
    }

    @Override
    public WorldStatus getStatus() {
        return worldStatus;
    }

    @Override
    public void setStatus(WorldStatus worldStatus) {
        this.worldStatus = worldStatus;
    }

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
        this.project = project;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Override
    public long getCreationDate() {
        return creationDate;
    }

    public String getFormattedCreationDate() {
        return creationDate > 0 ? new SimpleDateFormat(configValues.getDateFormat()).format(creationDate) : "-";
    }

    public String getChunkGeneratorString() {
        return chunkGeneratorString;
    }

    @Override
    public ChunkGenerator getChunkGenerator() {
        return chunkGenerator;
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
    public String getCustomSpawn() {
        return customSpawn;
    }

    @Override
    public void setCustomSpawn(Location location) {
        this.customSpawn = location.getX() + ";" + location.getY() + ";" + location.getZ() + ";" + location.getYaw() + ";" + location.getPitch();
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
    public List<Builder> getBuilders() {
        return builders;
    }

    @Override
    public void setBuildersEnabled(boolean buildersEnabled) {
        this.buildersEnabled = buildersEnabled;
    }

    public String getBuildersInfo() {
        String template = plugin.getString("world_item_builders_builder_template");
        ArrayList<String> builderNames = new ArrayList<>();

        if (configValues.isCreatorIsBuilder()) {
            if (getCreatorName() != null && !getCreatorName().equals("-")) {
                builderNames.add(getCreatorName());
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

    public ArrayList<String> getBuilderNames() {
        ArrayList<String> builderName = new ArrayList<>();
        getBuilders().forEach(builder -> builderName.add(builder.getName()));
        return builderName;
    }

    @Override
    public Builder getBuilder(UUID uuid) {
        return this.builders.parallelStream()
                .filter(builder -> builder.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Builder getBuilder(Player player){
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
        removeBuilder(player.getUniqueId());
    }

    private String saveBuilders() {
        StringBuilder builderList = new StringBuilder();
        for (Builder builder : getBuilders()) {
            builderList.append(";").append(builder.toString());
        }
        return builderList.length() > 0 ? builderList.substring(1) : builderList.toString();
    }

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
        world.put("item", getXMaterial().name());
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
            world.put("spawn", customSpawn);
        }
        if (chunkGeneratorString != null) {
            world.put("chunk-generator", getChunkGeneratorString());
        }

        return world;
    }

    public enum Time {
        SUNRISE, NOON, NIGHT, UNKNOWN
    }
}