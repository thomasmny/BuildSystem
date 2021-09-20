package de.eintosti.buildsystem.object.world;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SpawnManager;
import de.eintosti.buildsystem.util.external.UUIDFetcher;
import de.eintosti.buildsystem.util.external.xseries.Titles;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class BuildWorld implements ConfigurationSerializable {
    private final BuildSystem plugin;

    private String name;
    private String creator;
    private UUID creatorId;
    private final WorldType worldType;
    private XMaterial material;
    private boolean privateWorld;
    private WorldStatus worldStatus;
    private String project;
    private String permission;
    private String customSpawn;
    private final ArrayList<Builder> builders;
    private final long date;

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

    public BuildWorld(BuildSystem plugin, String name, String creator, UUID creatorId, WorldType worldType, long date, boolean privateWorld, String... chunkGeneratorString) {
        this.plugin = plugin;

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
        this.date = date;

        this.physics = plugin.isWorldPhysics();
        this.explosions = plugin.isWorldExplosions();
        this.mobAI = plugin.isWorldMobAi();
        this.blockBreaking = plugin.isWorldBlockBreaking();
        this.blockPlacement = plugin.isWorldBlockPlacement();
        this.blockInteractions = plugin.isWorldBlockInteractions();
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
        }

        if (privateWorld) {
            this.material = XMaterial.PLAYER_HEAD;
        }

        if (plugin.isUnloadWorlds()) {
            this.seconds = plugin.getTimeUntilUnload();
            this.loaded = (Bukkit.getWorld(name) != null);
            startUnloadTask();
        } else {
            this.loaded = true;
        }
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
            long date,
            boolean physics,
            boolean explosions,
            boolean mobAI,
            String customSpawn,
            boolean blockBreaking,
            boolean blockPlacement,
            boolean blockInteractions,
            boolean buildersEnabled,
            ArrayList<Builder> builders,
            ChunkGenerator chunkGenerator,
            String chunkGeneratorString
    ) {
        this.plugin = plugin;
        this.name = name;
        this.creator = creator;
        this.creatorId = creatorId;
        this.worldType = worldType;
        this.privateWorld = privateWorld;
        this.material = material;
        this.worldStatus = worldStatus;
        this.project = project;
        this.permission = permission;
        this.date = date;
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

        if (plugin.isUnloadWorlds()) {
            this.seconds = plugin.getTimeUntilUnload();
            this.loaded = (Bukkit.getWorld(name) != null);
            startUnloadTask();
        } else {
            this.loaded = true;
        }
    }

    /**
     * @return the {@link BuildWorld}'s name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the name of the {@link Player} who created the {@link BuildWorld}
     */
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * @return the {@link UUID} of the {@link Player} who created the {@link BuildWorld}
     */
    public UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(UUID creatorId) {
        this.creatorId = creatorId;
    }

    private String saveCreatorId() {
        String idString;
        if (getCreatorId() == null) {
            String creator = getCreator();
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

    /**
     * @return the {@link WorldType} of the {@link BuildWorld}
     */
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
        }
        return "-";
    }

    /**
     * @return the {@link BuildWorld}'s visibility (private or public)
     */
    public boolean isPrivate() {
        return privateWorld;
    }

    public void setPrivate(boolean privateWorld) {
        this.privateWorld = privateWorld;
    }

    /**
     * @return the {@link XMaterial} which represents a {@link BuildWorld} in the World Navigator
     */
    public XMaterial getMaterial() {
        return material;
    }

    public void setMaterial(XMaterial material) {
        this.material = material;
    }

    /**
     * @return the {@link WorldStatus} of the {@link BuildWorld}
     */
    public WorldStatus getStatus() {
        return worldStatus;
    }

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
        }
        return "-";
    }

    /**
     * @return the project of the {@link BuildWorld}
     */
    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    /**
     * @return the permission a {@link Player} must have in order to view and join the {@link BuildWorld}
     */
    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * @return the amount of milliseconds that have passed since January 1, 1970 UTC and the creation of the {@link BuildWorld}
     */
    public long getCreationDate() {
        return date;
    }

    /**
     * @return the name of the {@link ChunkGenerator} which is used to generate the {@link World}
     */
    public String getChunkGeneratorString() {
        return chunkGeneratorString;
    }

    /**
     * @return the chunk generator used to generate the {@link World}
     */
    public ChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }

    /**
     * @return whether physics are enabled
     */
    public boolean isPhysics() {
        return physics;
    }

    public void setPhysics(boolean physics) {
        this.physics = physics;
    }

    /**
     * @return whether explosions are enabled
     */
    public boolean isExplosions() {
        return explosions;
    }

    public void setExplosions(boolean explosions) {
        this.explosions = explosions;
    }

    /**
     * @return whether mobs have an AI
     */
    public boolean isMobAI() {
        return mobAI;
    }

    public void setMobAI(boolean mobAI) {
        this.mobAI = mobAI;
    }

    /**
     * @return the location as a {@link String} where a {@link Player} spawns in the {@link BuildWorld}
     */
    public String getCustomSpawn() {
        return customSpawn;
    }

    public void setCustomSpawn(Location customSpawn) {
        this.customSpawn = customSpawn.getX() + ";" + customSpawn.getY() + ";" + customSpawn.getZ() + ";" +
                customSpawn.getYaw() + ";" + customSpawn.getPitch();
    }

    public void removeCustomSpawn() {
        this.customSpawn = null;
    }

    /**
     * @return whether blocks can be broken in the {@link BuildWorld}
     */
    public boolean isBlockBreaking() {
        return blockBreaking;
    }

    public void setBlockBreaking(boolean blockBreaking) {
        this.blockBreaking = blockBreaking;
    }

    /**
     * @return whether blocks can be placed in the {@link BuildWorld}
     */
    public boolean isBlockPlacement() {
        return blockPlacement;
    }

    public void setBlockPlacement(boolean blockPlacement) {
        this.blockPlacement = blockPlacement;
    }

    /**
     * @return whether blocks can be interacted with in the {@link BuildWorld}
     */
    public boolean isBlockInteractions() {
        return blockInteractions;
    }

    public void setBlockInteractions(boolean blockInteractions) {
        this.blockInteractions = blockInteractions;
    }

    /**
     * @return whether only {@link Builder}s can break and place blocks in a {@link BuildWorld}
     */
    public boolean isBuilders() {
        return buildersEnabled;
    }

    /**
     * @return list of all {@link Builder}s
     */
    public ArrayList<Builder> getBuilders() {
        return builders;
    }

    public void setBuilders(boolean buildersEnabled) {
        this.buildersEnabled = buildersEnabled;
    }

    /**
     * @return list of all {@link Builder}'s names
     */
    public ArrayList<String> getBuilderNames() {
        ArrayList<String> builderName = new ArrayList<>();
        getBuilders().forEach(builder -> builderName.add(builder.getName()));
        return builderName;
    }

    /**
     * @param uuid the unique id of the {@link Player}
     * @return the builder object
     */
    public Builder getBuilder(UUID uuid) {
        return this.builders.parallelStream()
                .filter(builder -> builder.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * @param uuid the unique id of the {@link Player} to be checked
     * @return whether the {@link Player} is a builder
     */
    public boolean isBuilder(UUID uuid) {
        return this.builders.parallelStream().anyMatch(builder -> builder.getUuid().equals(uuid));
    }

    /**
     * @param player the player to be checked
     * @return whether the {@link Player} is a builder
     */
    public boolean isBuilder(Player player) {
        return isBuilder(player.getUniqueId());
    }

    /**
     * @param builder adds a {@link Builder} to the current list of builders
     */
    public void addBuilder(Builder builder) {
        this.builders.add(builder);
    }

    /**
     * @param builder removed a {@link Builder} from the current list of builders
     */
    private void removeBuilder(Builder builder) {
        this.builders.remove(builder);
    }

    /**
     * @param uuid adds a {@link Builder} to the current list of builders
     */
    public void removeBuilder(UUID uuid) {
        removeBuilder(getBuilder(uuid));
    }

    private String saveBuilders() {
        StringBuilder builderList = new StringBuilder();
        for (Builder builder : getBuilders()) {
            builderList.append(";").append(builder.toString());
        }
        return builderList.length() > 0 ? builderList.substring(1) : builderList.toString();
    }

    /**
     * @return whether the {@link World} has been loaded, allowing a {@link Player} to join the {@link BuildWorld}
     */
    public boolean isLoaded() {
        return loaded;
    }

    public void startUnloadTask() {
        if (!plugin.isUnloadWorlds()) {
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
        if (!isLoaded()) return;

        World bukkitWorld = Bukkit.getWorld(name);
        if (bukkitWorld == null) {
            return;
        }

        if (plugin.blackListedWorldsToUnload.contains(name) || isSpawnWorld(bukkitWorld)) {
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
        if (isLoaded()) return;

        player.closeInventory();
        String subtitle = plugin.getString("loading_world").replace("%world%", name);
        Titles.sendTitle(player, "", subtitle);

        plugin.getLogger().log(Level.INFO, "*** Loading world \"" + name + "\" ***");
        Bukkit.createWorld(new WorldCreator(name));
        this.loaded = true;

        resetUnloadTask();
    }

    public void load() {
        if (isLoaded()) return;

        plugin.getLogger().log(Level.INFO, "*** Loading world \"" + name + "\" ***");
        Bukkit.createWorld(new WorldCreator(name));
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
        if (customSpawn != null) world.put("spawn", customSpawn);
        if (chunkGeneratorString != null) world.put("chunk-generator", getChunkGeneratorString());

        return world;
    }

    public enum Time {
        SUNRISE, NOON, NIGHT, UNKNOWN
    }
}
