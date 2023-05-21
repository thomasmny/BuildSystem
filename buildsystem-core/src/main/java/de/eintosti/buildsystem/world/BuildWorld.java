/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.world;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.messages.Titles;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.event.world.BuildWorldLoadEvent;
import de.eintosti.buildsystem.event.world.BuildWorldLoadedEvent;
import de.eintosti.buildsystem.event.world.BuildWorldUnloadEvent;
import de.eintosti.buildsystem.event.world.BuildWorldUnloadedEvent;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.util.UUIDFetcher;
import de.eintosti.buildsystem.world.data.WorldData;
import de.eintosti.buildsystem.world.data.WorldType;
import de.eintosti.buildsystem.world.generator.CustomGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class BuildWorld implements ConfigurationSerializable {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private String name;
    private String creator;
    private UUID creatorId;

    private final WorldType worldType;
    private final WorldData worldData;
    private final long creationDate;
    private final CustomGenerator customGenerator;
    private final List<Builder> builders;

    private long seconds;
    private boolean loaded;
    private BukkitTask unloadTask;

    public BuildWorld(
            String name,
            String creator,
            UUID creatorId,
            WorldType worldType,
            long creationDate,
            boolean privateWorld,
            CustomGenerator customGenerator
    ) {
        this.plugin = JavaPlugin.getPlugin(BuildSystem.class);
        this.configValues = plugin.getConfigValues();

        this.name = name;
        this.creator = creator;
        this.creatorId = creatorId;
        this.worldType = worldType;
        this.worldData = new WorldData(
                name,
                configValues,
                privateWorld
        );
        this.customGenerator = customGenerator;
        this.creationDate = creationDate;
        this.builders = new ArrayList<>();

        InventoryUtils inventoryUtils = plugin.getInventoryUtil();
        XMaterial material;
        switch (worldType) {
            case NORMAL:
                material = inventoryUtils.getDefaultItem(WorldType.NORMAL);
                break;
            case FLAT:
                material = inventoryUtils.getDefaultItem(WorldType.FLAT);
                break;
            case NETHER:
                material = inventoryUtils.getDefaultItem(WorldType.NETHER);
                break;
            case END:
                material = inventoryUtils.getDefaultItem(WorldType.END);
                break;
            case VOID:
                material = inventoryUtils.getDefaultItem(WorldType.VOID);
                break;
            case CUSTOM:
            case TEMPLATE:
                material = XMaterial.FILLED_MAP;
                break;
            case IMPORTED:
                material = inventoryUtils.getDefaultItem(WorldType.IMPORTED);
                break;
            default:
                throw new IllegalArgumentException("Unsupported world type '" + worldType.name() + "' for world " + name);
        }
        if (privateWorld) {
            material = XMaterial.PLAYER_HEAD;
        }
        worldData.material().set(material);

        manageUnload();
    }

    public BuildWorld(
            String name,
            String creator,
            UUID creatorId,
            WorldType worldType,
            WorldData worldData,
            long creationDate,
            CustomGenerator customGenerator,
            List<Builder> builders
    ) {
        this.plugin = JavaPlugin.getPlugin(BuildSystem.class);
        this.configValues = plugin.getConfigValues();

        this.name = name;
        this.creator = creator;
        this.creatorId = creatorId;
        this.worldType = worldType;
        this.worldData = worldData;
        this.creationDate = creationDate;
        this.customGenerator = customGenerator;
        this.builders = builders;

        manageUnload();
    }

    /**
     * Get the world linked to this object.
     *
     * @return The bukkit world
     */
    public World getWorld() {
        return Bukkit.getWorld(name);
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
     * Gets whether the world has a creator
     *
     * @return {@code true} if the world has a creator, {@code false} otherwise
     */
    public boolean hasCreator() {
        return getCreator() != null;
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
     * Gets whether the given player is the creator of the world.
     *
     * @param player The player to check
     * @return {@code true} if the player is the creator, {@code false} otherwise
     */
    public boolean isCreator(Player player) {
        return creatorId != null && creatorId.equals(player.getUniqueId());
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
     * Gets the world's data.
     *
     * @return The {@link WorldData} of the world
     */
    public WorldData getData() {
        return worldData;
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
     * Get the custom chunk generator used to generate the world.
     *
     * @return The custom chunk generator used to generate the world.
     */
    @Nullable
    public CustomGenerator getCustomGenerator() {
        return customGenerator;
    }

    /**
     * Get the display name of a {@link Difficulty}.
     *
     * @return the difficulty's display name
     * @see WorldData#DIFFICULTY
     */
    public String getDifficultyName() {
        switch (worldData.difficulty().get()) {
            case PEACEFUL:
                return Messages.getString("difficulty_peaceful");
            case EASY:
                return Messages.getString("difficulty_easy");
            case NORMAL:
                return Messages.getString("difficulty_normal");
            case HARD:
                return Messages.getString("difficulty_hard");
            default:
                return "-";
        }
    }

    /**
     * Cycles to the next {@link Difficulty}.
     */
    public void cycleDifficulty() {
        switch (worldData.difficulty().get()) {
            case PEACEFUL:
                worldData.difficulty().set(Difficulty.EASY);
                break;
            case EASY:
                worldData.difficulty().set(Difficulty.NORMAL);
                break;
            case NORMAL:
                worldData.difficulty().set(Difficulty.HARD);
                break;
            case HARD:
                worldData.difficulty().set(Difficulty.PEACEFUL);
                break;
        }
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
        String template = Messages.getString("world_item_builders_builder_template");
        List<String> builderNames = getBuilderNames();

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
        World bukkitWorld = getWorld();
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
        this.loaded = (getWorld() != null);
        startUnloadTask();
    }

    private void startUnloadTask() {
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

    private void unload() {
        World bukkitWorld = getWorld();
        if (bukkitWorld == null) {
            return;
        }

        if (!bukkitWorld.getPlayers().isEmpty()) {
            resetUnloadTask();
            return;
        }

        forceUnload(true);
    }

    public void forceUnload(boolean save) {
        World bukkitWorld = getWorld();
        if (bukkitWorld == null) {
            return;
        }

        if (configValues.getBlackListedWorldsToUnload().contains(name) || isSpawnWorld(bukkitWorld)) {
            return;
        }

        BuildWorldUnloadEvent unloadEvent = new BuildWorldUnloadEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(unloadEvent);
        if (unloadEvent.isCancelled()) {
            return;
        }

        for (Chunk chunk : bukkitWorld.getLoadedChunks()) {
            chunk.unload(save);
        }
        Bukkit.unloadWorld(bukkitWorld, save);
        Bukkit.getWorlds().remove(bukkitWorld);

        this.worldData.lastUnloaded().set(System.currentTimeMillis());
        this.loaded = false;
        this.unloadTask = null;

        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldUnloadedEvent(this));

        plugin.getLogger().info("*** Unloaded world \"" + name + "\" ***");
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
        Titles.sendTitle(player, 5, 70, 20, " ", Messages.getString("loading_world", new AbstractMap.SimpleEntry<>("%world%", name)));

        load();
    }

    public void load() {
        if (isLoaded()) {
            return;
        }

        BuildWorldLoadEvent loadEvent = new BuildWorldLoadEvent(this);
        Bukkit.getServer().getPluginManager().callEvent(loadEvent);
        if (loadEvent.isCancelled()) {
            return;
        }

        plugin.getLogger().info("*** Loading world \"" + name + "\" ***");
        new BuildWorldCreator(plugin, this).generateBukkitWorld();

        this.worldData.lastLoaded().set(System.currentTimeMillis());
        this.loaded = true;

        Bukkit.getServer().getPluginManager().callEvent(new BuildWorldLoadedEvent(this));

        resetUnloadTask();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> world = new HashMap<>();

        world.put("creator", getCreator());
        world.put("creator-id", saveCreatorId());
        world.put("type", getType().name());
        world.put("data", getData().serialize());
        world.put("date", getCreationDate());
        world.put("builders", saveBuilders());
        if (customGenerator != null) {
            world.put("chunk-generator", customGenerator.getName());
        }

        return world;
    }

    public enum Time {
        SUNRISE, NOON, NIGHT, UNKNOWN
    }
}