package de.eintosti.buildsystem.manager;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.object.world.World;
import de.eintosti.buildsystem.object.world.WorldType;
import de.eintosti.buildsystem.object.world.*;
import de.eintosti.buildsystem.util.config.WorldConfig;
import de.eintosti.buildsystem.util.external.PlayerChatInput;
import de.eintosti.buildsystem.util.external.UUIDFetcher;
import de.eintosti.buildsystem.util.external.xseries.Titles;
import de.eintosti.buildsystem.util.external.xseries.XMaterial;
import de.eintosti.buildsystem.util.external.xseries.XSound;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class WorldManager {
    private final BuildSystem plugin;
    private final WorldConfig worldConfig;
    private static final List<World> worlds = new ArrayList<>();

    public HashSet<Player> createPrivateWorldPlayers;

    public WorldManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldConfig = new WorldConfig(plugin);
        this.createPrivateWorldPlayers = new HashSet<>();
    }

    public World getWorld(String worldName) {
        for (World world : worlds) {
            if (world.getName().equalsIgnoreCase(worldName)) return world;
        }
        return null;
    }

    public List<World> getWorlds() {
        return worlds;
    }

    public void openWorldAnvil(Player player, WorldType worldType, String template, boolean privateWorld) {
        if (privateWorld) {
            player.closeInventory();
            if (worldType == WorldType.TEMPLATE) {
                createTemplateWorld(player, player.getName(), ChatColor.stripColor(template));
            } else {
                createWorld(player, player.getName(), worldType, true);
            }
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
            if (worldType == WorldType.TEMPLATE) {
                createTemplateWorld(player, worldName, ChatColor.stripColor(template));
            } else {
                createWorld(player, worldName, worldType, false);
            }
        });
    }

    public void createWorld(Player player, String name, WorldType worldType, boolean privateWorld) {
        boolean worldExists = false;
        for (World world : worlds) {
            if (world.getName().equalsIgnoreCase(name)) {
                worldExists = true;
                break;
            }
        }

        File worldFile = new File(Bukkit.getWorldContainer(), name);
        if (worldExists || worldFile.exists()) {
            player.sendMessage(plugin.getString("worlds_world_exists"));
            XSound.ENTITY_ITEM_BREAK.play(player);
            return;
        }

        World world = new World(plugin, name, player.getName(), player.getUniqueId(), worldType, System.currentTimeMillis(), privateWorld);
        worlds.add(world);

        player.sendMessage(plugin.getString("worlds_world_creation_started")
                .replace("%world%", world.getName())
                .replace("%type%", world.getTypeName()));
        generateWorld(world);
        player.sendMessage(plugin.getString("worlds_creation_finished"));
    }

    @SuppressWarnings("deprecation")
    public org.bukkit.World generateBukkitWorld(String name, WorldType worldType, ChunkGenerator... chunkGenerators) {
        WorldCreator worldCreator = new WorldCreator(name);
        org.bukkit.WorldType bukkitWorldType;

        switch (worldType) {
            case VOID:
                worldCreator.generateStructures(false);
                bukkitWorldType = org.bukkit.WorldType.FLAT;
                if (XMaterial.isNewVersion()) {
                    worldCreator.generator(new ChunkGenerator() {
                        @Override
                        public ChunkData generateChunkData(org.bukkit.World world, Random random, int x, int z, BiomeGrid biome) {
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

        org.bukkit.World bukkitWorld = Bukkit.createWorld(worldCreator);

        if (bukkitWorld != null) {
            bukkitWorld.setDifficulty(Difficulty.PEACEFUL);
            bukkitWorld.setTime(plugin.getNoonTime());
            bukkitWorld.getWorldBorder().setSize(plugin.getWorldBorderSize());
            plugin.getDefaultGameRules().forEach(bukkitWorld::setGameRuleValue);
        }

        return bukkitWorld;
    }

    private void generateWorld(World world) {
        WorldType worldType = world.getType();
        org.bukkit.World bukkitWorld = generateBukkitWorld(world.getName(), worldType);

        switch (worldType) {
            case VOID:
                if (plugin.isVoidBlock()) {
                    bukkitWorld.getBlockAt(0, 64, 0).setType(Material.GOLD_BLOCK);
                }
                bukkitWorld.setSpawnLocation(0, 65, 0);
                break;
            case FLAT:
                bukkitWorld.setSpawnLocation(0, 4, 0);
                break;
        }
    }

    private void createTemplateWorld(Player player, String name, String template) {
        boolean worldExists = false;
        for (World world : worlds) {
            if (world.getName().equalsIgnoreCase(name)) {
                worldExists = true;
                break;
            }
        }
        File worldFile = new File(Bukkit.getWorldContainer(), name);
        if (worldExists || worldFile.exists()) {
            player.sendMessage(plugin.getString("worlds_world_exists"));
            return;
        }

        File templateFile = new File(plugin.getDataFolder() + File.separator + "templates" + File.separator + template);
        if (!templateFile.exists()) {
            player.sendMessage(plugin.getString("worlds_template_does_not_exist"));
            return;
        }
        World world = new World(plugin, name, player.getName(), player.getUniqueId(), WorldType.TEMPLATE, System.currentTimeMillis(), false);
        worlds.add(world);

        player.sendMessage(plugin.getString("worlds_template_creation_started")
                .replace("%world%", world.getName())
                .replace("%template%", template));
        copy(templateFile, worldFile);
        Bukkit.createWorld(WorldCreator.name(name)
                .type(org.bukkit.WorldType.FLAT)
                .generateStructures(false));
        player.sendMessage(plugin.getString("worlds_creation_finished"));
    }

    public void deleteWorld(Player player, World world) {
        if (!worlds.contains(world)) {
            player.sendMessage(plugin.getString("worlds_delete_unknown_world"));
            return;
        }
        worlds.remove(world);
        this.worldConfig.getFile().set("worlds." + world.getName(), null);
        this.worldConfig.saveFile();

        org.bukkit.World bukkitWorld = Bukkit.getWorld(world.getName());
        if (bukkitWorld != null) {
            SpawnManager spawnManager = plugin.getSpawnManager();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (!pl.getWorld().equals(bukkitWorld)) continue;

                Location spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5);
                if (spawnManager.spawnExists()) {
                    if (!spawnManager.getSpawnWorld().equals(pl.getWorld())) {
                        spawnManager.teleport(pl);
                    } else {
                        pl.teleport(spawnLocation);
                        spawnManager.remove();
                    }
                } else {
                    pl.teleport(spawnLocation);
                }
                pl.sendMessage(plugin.getString("worlds_delete_players_world"));
            }
            Bukkit.getServer().unloadWorld(bukkitWorld, false);
            Bukkit.getWorlds().remove(bukkitWorld);
        }

        File deleteFolder = new File(world.getName());
        if (!deleteFolder.exists()) {
            player.sendMessage(plugin.getString("worlds_delete_unknown_directory"));
            return;
        }

        player.sendMessage(plugin.getString("worlds_delete_started").replace("%world%", world.getName()));
        deleteFile(deleteFolder);
        player.sendMessage(plugin.getString("worlds_delete_finished"));
    }

    private void deleteFile(File folder) {
        if (folder.isDirectory()) {
            File[] list = folder.listFiles();
            if (list != null) {
                for (File tempFile : list) {
                    if (tempFile.isDirectory()) {
                        deleteFile(tempFile);
                    }
                    tempFile.delete();
                }
            }
            if (!folder.delete()) {
                plugin.getLogger().log(Level.SEVERE, "Can't delete folder: " + folder);
            }
        }
    }

    public void importWorld(String worldName, Player player, Generator generator, String... generatorName) {
        for (String charString : worldName.split("")) {
            if (charString.matches("[^A-Za-z0-9/_-]")) {
                player.sendMessage(plugin.getString("worlds_import_invalid_character").replace("%world%", worldName).replace("%char%", charString));
                return;
            }
        }

        File file = new File(plugin.getServer().getWorldContainer(), worldName);
        if (file.exists() && file.isDirectory()) {
            ChunkGenerator chunkGenerator = null;
            if (generator == Generator.CUSTOM) {
                List<String> genArray = new ArrayList<>(Arrays.asList(generatorName[0].split(":")));
                if (genArray.size() < 2) {
                    genArray.add("");
                }

                // chunkGenerator = getChunkGenerator(generatorName[0], "", worldName);
                chunkGenerator = getChunkGenerator(genArray.get(0), genArray.get(1), worldName);

                if (chunkGenerator == null) {
                    player.sendMessage(plugin.getString("worlds_import_unknown_generator"));
                    return;
                }
            }

            player.sendMessage(plugin.getString("worlds_import_started").replace("%world%", worldName));

            worlds.add(new World(plugin, worldName, "-", null, WorldType.IMPORTED, getDirectoryCreation(file), false));

            if (chunkGenerator == null) {
                generateBukkitWorld(worldName, generator.getWorldType());
            } else {
                generateBukkitWorld(worldName, generator.getWorldType(), chunkGenerator);
            }

            player.sendMessage(plugin.getString("worlds_import_finished"));
        } else {
            player.sendMessage(plugin.getString("worlds_import_unknown_world"));
        }
    }

    private long getDirectoryCreation(File file) {
        long creation = System.currentTimeMillis();
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            FileTime time = attrs.creationTime();
            creation = time.toMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return creation;
    }

    public ChunkGenerator getChunkGenerator(String generator, String generatorID, String worldName) {
        if (generator == null) return null;

        Plugin plugin = this.plugin.getServer().getPluginManager().getPlugin(generator);
        if (plugin == null) {
            return null;
        } else {
            return plugin.getDefaultWorldGenerator(worldName, generatorID);
        }
    }

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

                WorldManager.worlds.add(new World(plugin, worldName, "-", null, WorldType.IMPORTED, getDirectoryCreation(new File(worldName)), false));
                generateBukkitWorld(worldName, WorldType.VOID);
                player.sendMessage(plugin.getString("worlds_importall_world_imported").replace("%world%", worldName));

                if (!(worldsImported.get() < worlds)) {
                    this.cancel();
                    player.sendMessage(plugin.getString("worlds_importall_finished"));
                }
            }
        }.runTaskTimer(plugin, 0, 20L * delay);
    }

    public void renameWorld(Player player, World world, String name) {
        String oldName = world.getName();
        if (oldName.equalsIgnoreCase(name)) {
            player.sendMessage(plugin.getString("worlds_rename_same_name"));
            return;
        }

        for (String charString : name.split("")) {
            if (charString.matches("[^A-Za-z0-9/_-]")) {
                player.sendMessage(plugin.getString("worlds_world_creation_invalid_characters"));
                break;
            }
        }
        player.closeInventory();
        name = name.replaceAll("[^A-Za-z0-9/_-]", "").replace(" ", "_").trim();

        File oldWorldFile = new File(world.getName());
        File newWorldFile = new File(name);
        org.bukkit.World oldWorld = Bukkit.getWorld(world.getName());

        copy(oldWorldFile, newWorldFile);
        worldConfig.getFile().set("worlds." + name, worldConfig.getFile().getConfigurationSection("worlds." + world.getName()));
        worldConfig.getFile().set("worlds." + world.getName(), null);

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
        deleteFile(deleteFolder);

        world.setName(name);
        org.bukkit.World newWorld = generateBukkitWorld(world.getName(), world.getType());
        Location spawnLocation = oldWorld.getSpawnLocation();
        spawnLocation.setWorld(newWorld);

        players.forEach(pl -> {
            if (pl != null) {
                pl.teleport(spawnLocation.add(0.5, 0, 0.5));
            }
        });
        players.clear();

        if (Objects.equals(spawnManager.getSpawnWorld(), oldWorld)) {
            Location oldSpawn = spawnManager.getSpawn();
            spawnManager.set(new Location(spawnLocation.getWorld(), oldSpawn.getX(), oldSpawn.getY(), oldSpawn.getZ(),
                    oldSpawn.getYaw(), oldSpawn.getPitch()), spawnLocation.getWorld().getName());
        }

        player.sendMessage(plugin.getString("worlds_rename_set")
                .replace("%oldName%", oldName)
                .replace("%newName%", name));
    }

    public void teleport(Player player, World world) {
        boolean hadToLoad = false;
        if (plugin.isUnloadWorlds() && !world.isLoaded()) {
            world.load(player);
            hadToLoad = true;
        }

        org.bukkit.World bukkitWorld = Bukkit.getServer().getWorld(world.getName());
        if (bukkitWorld == null) {
            player.sendMessage(plugin.getString("worlds_tp_unknown_world"));
            return;
        }

        Location location = bukkitWorld.getSpawnLocation().add(0.5, 0, 0.5);
        if (world.getCustomSpawn() == null) {
            switch (world.getType()) {
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
            String[] spawnString = world.getCustomSpawn().split(";");
            location = new Location(bukkitWorld, Double.parseDouble(spawnString[0]), Double.parseDouble(spawnString[1]), Double.parseDouble(spawnString[2]), Float.parseFloat(spawnString[3]), Float.parseFloat(spawnString[4]));
        }

        Location finalLocation = location;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.teleport(finalLocation);
            Titles.clearTitle(player);
        }, hadToLoad ? 20L : 0L);
    }

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
        worlds.forEach(worldConfig::saveWorld);
    }

    public void load() {
        FileConfiguration configuration = worldConfig.getFile();
        if (configuration == null) return;

        ConfigurationSection configurationSection = configuration.getConfigurationSection("worlds");
        if (configurationSection == null) return;

        Set<String> worlds = configurationSection.getKeys(false);
        if (worlds.isEmpty()) return;

        worlds.forEach(this::loadWorld);
        worldConfig.loadWorlds(this);
    }

    public World loadWorld(String worldName) {
        FileConfiguration configuration = worldConfig.getFile();
        if (configuration == null) return null;

        ConfigurationSection configurationSection = configuration.getConfigurationSection("worlds");
        if (configurationSection == null) return null;

        Set<String> worlds = configurationSection.getKeys(false);
        if (worlds.isEmpty()) return null;

        String creator = configuration.isString("worlds." + worldName + ".creator") ? configuration.getString("worlds." + worldName + ".creator") : "-";
        UUID creatorId = loadCreatorId(configuration, worldName, creator);
        WorldType worldType = configuration.isString("worlds." + worldName + ".type") ? WorldType.valueOf(configuration.getString("worlds." + worldName + ".type")) : WorldType.UNKNOWN;
        boolean privateWorld = configuration.isBoolean("worlds." + worldName + ".private") && configuration.getBoolean("worlds." + worldName + ".private");
        XMaterial material;
        try {
            String itemString = configuration.getString("worlds." + worldName + ".item");
            Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(itemString);
            if (xMaterial.isPresent()) {
                material = xMaterial.get();
            } else {
                plugin.getLogger().log(Level.WARNING, "[BuildSystem] Unknown material found for \"" + worldName + "\" (" + configuration.getString("worlds." + worldName + ".item").split(":")[0] + ").");
                plugin.getLogger().log(Level.WARNING, "[BuildSystem] Material changed to BEDROCK.");
                material = XMaterial.BEDROCK;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "[BuildSystem] Unknown material found for \"" + worldName + "\" (" + configuration.getString("worlds." + worldName + ".item").split(":")[0] + ").");
            plugin.getLogger().log(Level.WARNING, "[BuildSystem] Material changed to BEDROCK.");
            material = XMaterial.BEDROCK;
        }
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
        ArrayList<Builder> builders = loadBuilders(configuration, worldName);

        if (worldType == WorldType.PRIVATE) {
            privateWorld = true;
            worldType = WorldType.FLAT;
        }

        World world = new World(plugin, worldName, creator, creatorId, worldType, privateWorld, material, worldStatus, project,
                permission, date, physics, explosions, mobAI, customSpawn, blockBreaking, blockPlacement, blockInteractions, buildersEnabled, builders);
        WorldManager.worlds.add(world);
        return world;
    }

    private UUID loadCreatorId(FileConfiguration configuration, String worldName, String creator) {
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

    private ArrayList<Builder> loadBuilders(FileConfiguration configuration, String worldName) {
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

    private void copy(File source, File target) {
        try {
            ArrayList<String> ignore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));
            if (ignore.contains(source.getName())) return;
            if (source.isDirectory()) {
                if (!target.exists()) {
                    if (!target.mkdirs()) {
                        throw new IOException("Couldn't create world directory!");
                    }
                }
                String[] files = source.list();
                for (String file : files) {
                    if (ignore.contains(file)) continue;
                    File srcFile = new File(source, file);
                    File trgFile = new File(target, file);
                    copy(srcFile, trgFile);
                }
            } else {
                InputStream inputStream = new FileInputStream(source);
                OutputStream outputStream = new FileOutputStream(target);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}