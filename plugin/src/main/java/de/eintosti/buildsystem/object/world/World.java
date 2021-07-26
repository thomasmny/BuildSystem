package de.eintosti.buildsystem.object.world;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.manager.InventoryManager;
import de.eintosti.buildsystem.manager.SpawnManager;
import de.eintosti.buildsystem.util.external.UUIDFetcher;
import de.eintosti.buildsystem.util.external.xseries.Titles;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.craftbukkit.v1_17_R1.generator.CustomChunkGenerator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class World implements ConfigurationSerializable {
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

    private String chunkGeneratorString;
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

    public World(BuildSystem plugin, String name, String creator, UUID creatorId, WorldType worldType, long date, boolean privateWorld, String... chunkGeneratorString) {
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
        if (privateWorld) this.material = XMaterial.PLAYER_HEAD;

        if (plugin.isUnloadWorlds()) {
            this.seconds = plugin.getTimeUntilUnload();
            this.loaded = (Bukkit.getWorld(name) != null);
            startUnloadTask();
        } else {
            this.loaded = true;
        }
    }

    public World(BuildSystem plugin, String name, String creator, UUID creatorId, WorldType worldType, boolean privateWorld,
                 XMaterial material, WorldStatus worldStatus, String project, String permission, long date, boolean physics,
                 boolean explosions, boolean mobAI, String customSpawn, boolean blockBreaking, boolean blockPlacement,
                 boolean blockInteractions, boolean buildersEnabled, ArrayList<Builder> builders, ChunkGenerator chunkGenerator, String chunkGeneratorString) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

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

    public boolean isPrivate() {
        return privateWorld;
    }

    public void setPrivate(boolean privateWorld) {
        this.privateWorld = privateWorld;
    }

    public XMaterial getMaterial() {
        return material;
    }

    public void setMaterial(XMaterial material) {
        this.material = material;
    }

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

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public long getCreationDate() {
        return date;
    }

    public boolean isPhysics() {
        return physics;
    }

    public void setPhysics(boolean physics) {
        this.physics = physics;
    }

    public boolean isExplosions() {
        return explosions;
    }

    public void setExplosions(boolean explosions) {
        this.explosions = explosions;
    }

    public boolean isMobAI() {
        return mobAI;
    }

    public void setMobAI(boolean mobAI) {
        this.mobAI = mobAI;
    }

    public String getCustomSpawn() {
        return customSpawn;
    }

    public void setCustomSpawn(Location customSpawn) {
        this.customSpawn = customSpawn.getX() + ";" + customSpawn.getY() + ";" + customSpawn.getZ() + ";" +
                customSpawn.getYaw() + ";" + customSpawn.getPitch();
    }

    public boolean isBlockBreaking() {
        return blockBreaking;
    }

    public void setBlockBreaking(boolean blockBreaking) {
        this.blockBreaking = blockBreaking;
    }

    public boolean isBlockPlacement() {
        return blockPlacement;
    }

    public void setBlockPlacement(boolean blockPlacement) {
        this.blockPlacement = blockPlacement;
    }

    public boolean isBlockInteractions() {
        return blockInteractions;
    }

    public void setBlockInteractions(boolean blockInteractions) {
        this.blockInteractions = blockInteractions;
    }

    public boolean isBuilders() {
        return buildersEnabled;
    }

    public ArrayList<Builder> getBuilders() {
        return builders;
    }

    public void setBuilders(boolean buildersEnabled) {
        this.buildersEnabled = buildersEnabled;
    }

    public ArrayList<String> getBuilderNames() {
        ArrayList<String> builderName = new ArrayList<>();
        getBuilders().forEach(builder -> builderName.add(builder.getName()));
        return builderName;
    }

    public Builder getBuilder(UUID uuid) {
        for (Builder builder : this.builders) {
            if (builder.getUuid().equals(uuid)) {
                return builder;
            }
        }
        return null;
    }

    public boolean isBuilder(UUID uuid) {
        for (Builder b : this.builders) {
            if (b.getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBuilder(Player player) {
        return isBuilder(player.getUniqueId());
    }

    public void addBuilder(Builder builder) {
        this.builders.add(builder);
    }

    private void removeBuilder(Builder builder) {
        this.builders.remove(builder);
    }

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

    public boolean isLoaded() {
        return loaded;
    }

    public void startUnloadTask() {
        if (!plugin.isUnloadWorlds()) return;
        this.unloadTask = Bukkit.getScheduler().runTaskLater(plugin, this::unload, 20L * seconds);
    }

    public void resetUnloadTask() {
        if (this.unloadTask != null) {
            this.unloadTask.cancel();
        }
        startUnloadTask();
    }

    private void unload() {
        if (!isLoaded()) return;

        org.bukkit.World bukkitWorld = Bukkit.getWorld(name);
        if (bukkitWorld == null) return;

        if (isSpawnWorld(bukkitWorld)) return;
        if (!bukkitWorld.getPlayers().isEmpty()) {
            resetUnloadTask();
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

    private boolean isSpawnWorld(org.bukkit.World bukkitWorld) {
        SpawnManager spawnManager = plugin.getSpawnManager();
        if (!spawnManager.spawnExists()) return false;
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
    public Map<String, Object> serialize() {
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
        if (chunkGeneratorString != null) {
            world.put("chunk-generator", getChunkGeneratorString());
        }
        return world;
    }

    public enum Time {
        SUNRISE, NOON, NIGHT, UNKNOWN
    }

    public String getChunkGeneratorString() {
        return chunkGeneratorString;
    }

    public ChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }
}
