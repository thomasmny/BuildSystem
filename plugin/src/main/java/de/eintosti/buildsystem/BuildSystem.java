/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package de.eintosti.buildsystem;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.command.*;
import de.eintosti.buildsystem.inventory.*;
import de.eintosti.buildsystem.listener.*;
import de.eintosti.buildsystem.manager.*;
import de.eintosti.buildsystem.object.settings.Settings;
import de.eintosti.buildsystem.object.world.BuildWorld;
import de.eintosti.buildsystem.tabcomplete.*;
import de.eintosti.buildsystem.util.Messages;
import de.eintosti.buildsystem.util.SkullCache;
import de.eintosti.buildsystem.util.bstats.Metrics;
import de.eintosti.buildsystem.util.external.UpdateChecker;
import de.eintosti.buildsystem.util.placeholder.BuildSystemExpansion;
import de.eintosti.buildsystem.version.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * @author einTosti
 */
public class BuildSystem extends JavaPlugin {
    public final static int PLUGIN_ID = 60441;

    public Map<UUID, BuildWorld> selectedWorld;
    public Map<UUID, GameMode> buildPlayerGamemode;
    public Map<UUID, Float> playerWalkSpeed;
    public Map<UUID, Float> playerFlySpeed;

    public Set<Player> openNavigator;
    public Set<String> blackListedWorldsToUnload;
    public Set<UUID> buildPlayers;

    private String prefix;
    private String version;
    private String dateFormat;
    private String worldDifficulty;
    private String timeUntilUnload;

    private boolean archiveVanish;
    private boolean scoreboard;
    private boolean spawnTeleportMessage;
    private boolean joinQuitMessages;
    private boolean lockWeather;
    private boolean unloadWorlds;
    private boolean voidBlock;
    private boolean updateChecker;
    private boolean blockWorldEditNonBuilder;
    private boolean creatorIsBuilder;
    private boolean giveNavigatorOnJoin;
    private boolean worldPhysics;
    private boolean worldExplosions;
    private boolean worldMobAi;
    private boolean worldBlockBreaking;
    private boolean worldBlockPlacement;
    private boolean worldBlockInteractions;
    private XMaterial navigatorItem;

    private int sunriseTime;
    private int noonTime;
    private int nightTime;
    private int worldBorderSize;
    private int importDelay;

    private Map<String, String> defaultGameRules;

    private String scoreboardTitle;
    private List<String> scoreboardBody;

    private WorldsCommand worldsCommand;

    private ArmorStandManager armorStandManager;
    private InventoryManager inventoryManager;
    private NoClipManager noClipManager;
    private SettingsManager settingsManager;
    private SpawnManager spawnManager;
    private WorldManager worldManager;

    private ArchiveInventory archiveInventory;
    private BlocksInventory blocksInventory;
    private BuilderInventory builderInventory;
    private CreateInventory createInventory;
    private DeleteInventory deleteInventory;
    private DesignInventory designInventory;
    private EditInventory editInventory;
    private GameRuleInventory gameRuleInventory;
    private NavigatorInventory navigatorInventory;
    private PrivateInventory privateInventory;
    private SettingsInventory settingsInventory;
    private SetupInventory setupInventory;
    private SpeedInventory speedInventory;
    private StatusInventory statusInventory;
    private WorldsInventory worldsInventory;

    private PlayerMoveListener playerMoveListener;
    private PlayerTeleportListener playerTeleportListener;

    private CustomBlocks customBlocks;
    private GameRules gameRules;
    private SkullCache skullCache;

    @Override
    public void onEnable() {
        createLanguageFile();
        createTemplateFolder();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        this.prefix = getPrefixString();
        this.scoreboardTitle = getString("title");
        this.scoreboardBody = getStringList("body");
        setConfigValues();

        this.selectedWorld = new HashMap<>();
        this.buildPlayerGamemode = new HashMap<>();
        this.playerWalkSpeed = new HashMap<>();
        this.playerFlySpeed = new HashMap<>();
        this.openNavigator = new HashSet<>();
        this.buildPlayers = new HashSet<>();

        getVersion();
        if (!setupCustomBlocks()) return;
        if (!setupGameRules()) return;

        initClasses();
        registerCommands();
        registerTabCompleter();
        registerListeners();
        registerStats();
        registerPlaceholders();

        worldManager.load();
        settingsManager.load();
        spawnManager.load();

        Bukkit.getOnlinePlayers().forEach(pl -> {
            getSkullCache().cacheSkull(pl.getName());

            settingsManager.createSettings(pl);
            Settings settings = settingsManager.getSettings(pl);
            if (settings.isScoreboard()) settingsManager.startScoreboard(pl);
            if (settings.isNoClip()) noClipManager.startNoClip(pl);
        });

        if (isUpdateChecker()) {
            performUpdateCheck();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.RESET + "BuildSystem » Plugin " + ChatColor.GREEN + "enabled" + ChatColor.RESET + "!");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(pl -> {
            settingsManager.stopScoreboard(pl);
            noClipManager.stopNoClip(pl.getUniqueId());
            playerMoveListener.closeNavigator(pl);
        });

        reloadConfig();
        reloadConfigData(false);
        saveConfig();

        worldManager.save();
        settingsManager.save();
        spawnManager.save();
        inventoryManager.save();

        Bukkit.getConsoleSender().sendMessage(ChatColor.RESET + "BuildSystem » Plugin " + ChatColor.RED + "disabled" + ChatColor.RESET + "!");
    }

    private void initClasses() {
        this.armorStandManager = new ArmorStandManager();
        this.inventoryManager = new InventoryManager(this);
        this.inventoryManager.loadTypes();
        this.inventoryManager.loadStatus();
        this.noClipManager = new NoClipManager(this);
        this.worldManager = new WorldManager(this);
        this.settingsManager = new SettingsManager(this);
        this.spawnManager = new SpawnManager(this);

        this.archiveInventory = new ArchiveInventory(this);
        this.blocksInventory = new BlocksInventory(this);
        this.builderInventory = new BuilderInventory(this);
        this.createInventory = new CreateInventory(this);
        this.deleteInventory = new DeleteInventory(this);
        this.designInventory = new DesignInventory(this);
        this.editInventory = new EditInventory(this);
        this.gameRuleInventory = new GameRuleInventory(this);
        this.navigatorInventory = new NavigatorInventory(this);
        this.privateInventory = new PrivateInventory(this);
        this.settingsInventory = new SettingsInventory(this);
        this.setupInventory = new SetupInventory(this);
        this.speedInventory = new SpeedInventory(this);
        this.statusInventory = new StatusInventory(this);
        this.worldsInventory = new WorldsInventory(this);

        this.skullCache = new SkullCache();
    }

    private void getVersion() {
        try {
            this.version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            getLogger().log(Level.INFO, "Detected server version: " + version);
        } catch (ArrayIndexOutOfBoundsException e) {
            getLogger().log(Level.SEVERE, "Unknown server version");
        }
    }

    public boolean setupCustomBlocks() {
        switch (version) {
            case "v1_8_R1":
            case "v1_8_R2":
            case "v1_8_R3":
            case "v1_9_R1":
            case "v1_9_R2":
            case "v1_10_R1":
            case "v1_11_R1":
            case "v1_12_R1":
                this.customBlocks = new CustomBlocks_1_12_R1(this);
                return true;
            case "v1_13_R1":
            case "v1_13_R2":
                this.customBlocks = new CustomBlocks_1_13_R1(this);
                return true;
            case "v1_14_R1":
            case "v1_15_R1":
            case "v1_16_R1":
            case "v1_16_R2":
            case "v1_16_R3":
            case "v1_17_R1":
                this.customBlocks = new CustomBlocks_1_14_R1(this);
                return true;
            default:
                getLogger().log(Level.SEVERE, "\"CustomBlocks\" not found for version: " + version);
                getLogger().log(Level.SEVERE, "Please report this bug to einTosti with your server version");
                this.setEnabled(false);
                return false;
        }
    }

    public boolean setupGameRules() {
        switch (version) {
            case "v1_8_R1":
            case "v1_8_R2":
            case "v1_8_R3":
            case "v1_9_R1":
            case "v1_9_R2":
            case "v1_10_R1":
            case "v1_11_R1":
            case "v1_12_R1":
                this.gameRules = new GameRules_1_12_R1(
                        getString("worldeditor_gamerules_title"),
                        getStringList("worldeditor_gamerules_boolean_enabled"),
                        getStringList("worldeditor_gamerules_boolean_disabled"),
                        getStringList("worldeditor_gamerules_unknown"),
                        getStringList("worldeditor_gamerules_integer"));
                return true;
            case "v1_13_R1":
            case "v1_13_R2":
            case "v1_14_R1":
            case "v1_15_R1":
            case "v1_16_R1":
            case "v1_16_R2":
            case "v1_16_R3":
            case "v1_17_R1":
                this.gameRules = new GameRules_1_13_R1(
                        getString("worldeditor_gamerules_title"),
                        getStringList("worldeditor_gamerules_boolean_enabled"),
                        getStringList("worldeditor_gamerules_boolean_disabled"),
                        getStringList("worldeditor_gamerules_integer"));
                return true;
            default:
                getLogger().log(Level.SEVERE, "\"GameRules\" not found for version: " + version);
                getLogger().log(Level.SEVERE, "Please report this bug to einTosti with your server version");
                this.setEnabled(false);
                return false;
        }
    }

    private void registerCommands() {
        new BackCommand(this);
        new BlocksCommand(this);
        new BuildCommand(this);
        new BuildSystemCommand(this);
        new ConfigCommand(this);
        new ExplosionsCommand(this);
        new GamemodeCommand(this);
        new NoAICommand(this);
        new PhysicsCommand(this);
        new SettingsCommand(this);
        new SetupCommand(this);
        new SkullCommand(this);
        new SpawnCommand(this);
        new SpeedCommand(this);
        new TimeCommand(this);
        new TopCommand(this);
        this.worldsCommand = new WorldsCommand(this);
    }

    private void registerTabCompleter() {
        new ConfigTabComplete(this);
        new EmptyTabComplete(this);
        new GamemodeTabComplete(this);
        new PhysicsTabComplete(this);
        new SpawnTabComplete(this);
        new SpeedTabComplete(this);
        new TimeTabComplete(this);
        new WorldsTabComplete(this);
    }

    private void registerListeners() {
        new AsyncPlayerChatListener(this);
        new BlockPhysicsListener(this);
        new BlockPlaceListener(this);
        new EntitySpawnListener(this);
        new FoodLevelChangeListener(this);
        new InventoryClickListener(this);
        new InventoryCloseListener(this);
        new PlayerChangedWorldListener(this);
        new PlayerCommandPreprocessListener(this);
        new PlayerInteractAtEntityListener(this);
        new PlayerInteractListener(this);
        new PlayerInventoryClearListener(this);
        new PlayerJoinListener(this);
        this.playerMoveListener = new PlayerMoveListener(this);
        new PlayerQuitListener(this);
        new PlayerRespawnListener(this);
        this.playerTeleportListener = new PlayerTeleportListener(this);
        new SignChangeListener(this);
        new WeatherChangeListener(this);
        new WorldManipulateListener(this);

        if (isWorldEdit() && isBlockWorldEditNonBuilder()) {
            new EditSessionListener(this);
        }
    }

    private boolean isWorldEdit() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        return pluginManager.getPlugin("FastAsyncWorldEdit") != null || pluginManager.getPlugin("WorldEdit") != null;
    }

    private void registerStats() {
        int pluginId = 7427;
        Metrics metrics = new Metrics(this, pluginId);

        metrics.addCustomChart(new Metrics.SimplePie("scoreboard", () -> String.valueOf(scoreboard)));
        metrics.addCustomChart(new Metrics.SimplePie("archive_vanish", () -> String.valueOf(archiveVanish)));
        metrics.addCustomChart(new Metrics.SimplePie("join_quit_messages", () -> String.valueOf(joinQuitMessages)));
        metrics.addCustomChart(new Metrics.SimplePie("lock_weather", () -> String.valueOf(lockWeather)));
        metrics.addCustomChart(new Metrics.SimplePie("unload_worlds", () -> String.valueOf(unloadWorlds)));
        metrics.addCustomChart(new Metrics.SimplePie("void_block", () -> String.valueOf(voidBlock)));
        metrics.addCustomChart(new Metrics.SimplePie("update_checker", () -> String.valueOf(updateChecker)));
        metrics.addCustomChart(new Metrics.SimplePie("block_world_edit", () -> String.valueOf(blockWorldEditNonBuilder)));
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BuildSystemExpansion(this).register();
        }
    }

    private void performUpdateCheck() {
        UpdateChecker.init(this, PLUGIN_ID).requestUpdateCheck().whenComplete((result, e) -> {
                    if (result.requiresUpdate()) {
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[BuildSystem] Great! a new update is available:" + ChatColor.GREEN + "v" + result.getNewestVersion());
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + " ➥ Your current version: " + ChatColor.RED + this.getDescription().getVersion());
                        return;
                    }

                    UpdateChecker.UpdateReason reason = result.getReason();
                    switch (reason) {
                        case COULD_NOT_CONNECT:
                        case INVALID_JSON:
                        case UNAUTHORIZED_QUERY:
                        case UNKNOWN_ERROR:
                        case UNSUPPORTED_VERSION_SCHEME:
                            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BuildSystem] Could not check for a new version of BuildSystem. Reason: " + reason);
                            break;
                    }
                }
        );
    }

    private void createTemplateFolder() {
        File templateFolder = new File(getDataFolder() + File.separator + "templates");
        if (templateFolder.mkdir()) {
            getLogger().log(Level.INFO, "Created \"templates\" folder");
        }
    }

    private void createLanguageFile() {
        if (getDataFolder().mkdir()) {
            getLogger().log(Level.INFO, "Created \"BuildSystem\" folder");
        }
        Messages.getInstance().createMessageFile();
    }

    private String getPrefixString() {
        String prefix = Messages.getInstance().messageData.get("prefix");
        try {
            return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : "§8× §bBuildSystem §8┃";
        } catch (NullPointerException e) {
            Messages.getInstance().createMessageFile();
            return getPrefixString();
        }
    }

    public String getString(String key) {
        try {
            return ChatColor.translateAlternateColorCodes('&', Messages.getInstance().messageData.get(key).replace("%prefix%", getPrefix()));
        } catch (NullPointerException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BuildSystem] Could not find message with key: " + key);
            Messages.getInstance().createMessageFile();
            return getString(key);
        }
    }

    public List<String> getStringList(String key) {
        try {
            List<String> list = new ArrayList<>();
            String string = Messages.getInstance().messageData.get(key);
            String[] splitString = string.substring(1, string.length() - 1).split(", ");
            for (String s : splitString) {
                list.add(ChatColor.translateAlternateColorCodes('&', s.replace("%prefix%", getPrefix())));
            }
            return list;
        } catch (NullPointerException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BuildSystem] Could not get list with key: " + key);
            Messages.getInstance().createMessageFile();
            return getStringList(key);
        }
    }

    public boolean canBypass(Player player) {
        return player.hasPermission("buildsystem.admin") || player.hasPermission("buildsystem.bypass.archive") || this.buildPlayers.contains(player.getUniqueId());
    }

    public void sendPermissionMessage(CommandSender sender) {
        sender.sendMessage(getString("no_permissions"));
    }

    public void reloadConfigData(boolean init) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            getSettingsManager().stopScoreboard(pl);
        }
        setConfigValues();

        if (init) {
            setupCustomBlocks();
            if (isScoreboard()) {
                getSettingsManager().startScoreboard();
            } else {
                getSettingsManager().stopScoreboard();
            }
        }
    }

    private void setConfigValues() {
        final FileConfiguration config = getConfig();

        // Messages
        this.spawnTeleportMessage = config.getBoolean("messages.spawn-teleport-message", false);
        this.joinQuitMessages = config.getBoolean("messages.join-quit-messages", true);
        this.dateFormat = config.getString("messages.date-format", "dd/MM/yyyy");

        // Settings
        this.updateChecker = config.getBoolean("settings.update-checker", true);
        this.scoreboard = config.getBoolean("settings.scoreboard", true);
        this.archiveVanish = config.getBoolean("settings.archive-vanish", true);

        this.blockWorldEditNonBuilder = config.getBoolean("settings.builder.block-worldedit-non-builder", true);
        this.creatorIsBuilder = config.getBoolean("settings.builder.creator-is-builder", true);

        this.navigatorItem = XMaterial.valueOf(config.getString("settings.navigator.item", "CLOCK"));
        this.giveNavigatorOnJoin = config.getBoolean("settings.navigator.give-item-on-join", true);

        // World
        this.lockWeather = config.getBoolean("world.lock-weather", true);
        this.worldDifficulty = config.getString("world.default.difficulty", "PEACEFUL");
        this.sunriseTime = config.getInt("world.default.time.sunrise", 0);
        this.noonTime = config.getInt("world.default.time.noon", 6000);
        this.nightTime = config.getInt("world.default.time.night", 18000);

        this.worldBorderSize = config.getInt("world.default.worldborder.size", 6000000);

        HashMap<String, String> defaultGameRules = new HashMap<>();
        ConfigurationSection configurationSection = config.getConfigurationSection("world.default.gamerules");
        if (configurationSection != null) {
            for (Map.Entry<String, Object> entry : configurationSection.getValues(true).entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue().toString();
                defaultGameRules.put(name, value);
            }
        }
        this.defaultGameRules = defaultGameRules;

        this.worldPhysics = config.getBoolean("world.default.settings.physics", true);
        this.worldExplosions = config.getBoolean("world.default.settings.explosions", true);
        this.worldMobAi = config.getBoolean("world.default.settings.mob-ai", true);
        this.worldBlockBreaking = config.getBoolean("world.default.settings.block-breaking", true);
        this.worldBlockPlacement = config.getBoolean("world.default.settings.block-placement", true);
        this.worldBlockInteractions = config.getBoolean("world.default.settings.block-interactions", true);

        this.unloadWorlds = config.getBoolean("world.unload.enabled", false);
        this.timeUntilUnload = config.getString("world.unload.time-until-unload", "01:00:00");
        this.blackListedWorldsToUnload = new HashSet<>(config.getStringList("world.unload.blacklisted-worlds"));

        this.voidBlock = config.getBoolean("world.void-block", true);

        this.importDelay = config.getInt("world.import-all.delay", 30);
    }

    public String getPrefix() {
        return prefix;
    }

    public Boolean isArchiveVanish() {
        return archiveVanish;
    }

    public Boolean isScoreboard() {
        return scoreboard;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public String getWorldDifficulty() {
        return worldDifficulty;
    }

    public boolean isSpawnTeleportMessage() {
        return spawnTeleportMessage;
    }

    public boolean isJoinQuitMessages() {
        return joinQuitMessages;
    }

    public boolean isLockWeather() {
        return lockWeather;
    }

    public boolean isUnloadWorlds() {
        return unloadWorlds;
    }

    public boolean isVoidBlock() {
        return voidBlock;
    }

    public boolean isUpdateChecker() {
        return updateChecker;
    }

    public boolean isBlockWorldEditNonBuilder() {
        return blockWorldEditNonBuilder;
    }

    public boolean isCreatorIsBuilder() {
        return creatorIsBuilder;
    }

    public XMaterial getNavigatorItem() {
        return navigatorItem;
    }

    public boolean isGiveNavigatorOnJoin() {
        return giveNavigatorOnJoin;
    }

    public boolean isWorldPhysics() {
        return worldPhysics;
    }

    public boolean isWorldExplosions() {
        return worldExplosions;
    }

    public boolean isWorldMobAi() {
        return worldMobAi;
    }

    public boolean isWorldBlockBreaking() {
        return worldBlockBreaking;
    }

    public boolean isWorldBlockPlacement() {
        return worldBlockPlacement;
    }

    public boolean isWorldBlockInteractions() {
        return worldBlockInteractions;
    }

    public int getSunriseTime() {
        return sunriseTime;
    }

    public int getNoonTime() {
        return noonTime;
    }

    public int getNightTime() {
        return nightTime;
    }

    public int getWorldBorderSize() {
        return worldBorderSize;
    }

    public Map<String, String> getDefaultGameRules() {
        return defaultGameRules;
    }

    public int getImportDelay() {
        return importDelay;
    }

    public long getTimeUntilUnload() {
        String[] timeArray = timeUntilUnload.split(":");
        int hours = Integer.parseInt(timeArray[0]);
        int minutes = Integer.parseInt(timeArray[1]);
        int seconds = Integer.parseInt(timeArray[2]);
        return hours * 3600L + minutes * 60L + seconds;
    }

    public String getScoreboardTitle() {
        return scoreboardTitle;
    }

    public List<String> getScoreboardBody() {
        return scoreboardBody;
    }

    public WorldsCommand getWorldsCommand() {
        return worldsCommand;
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public NoClipManager getNoClipManager() {
        return noClipManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public ArchiveInventory getArchiveInventory() {
        return archiveInventory;
    }

    public BlocksInventory getBlocksInventory() {
        return blocksInventory;
    }

    public BuilderInventory getBuilderInventory() {
        return builderInventory;
    }

    public CreateInventory getCreateInventory() {
        return createInventory;
    }

    public DeleteInventory getDeleteInventory() {
        return deleteInventory;
    }

    public DesignInventory getDesignInventory() {
        return designInventory;
    }

    public EditInventory getEditInventory() {
        return editInventory;
    }

    public GameRuleInventory getGameRuleInventory() {
        return gameRuleInventory;
    }

    public NavigatorInventory getNavigatorInventory() {
        return navigatorInventory;
    }

    public PrivateInventory getPrivateInventory() {
        return privateInventory;
    }

    public SettingsInventory getSettingsInventory() {
        return settingsInventory;
    }

    public SetupInventory getSetupInventory() {
        return setupInventory;
    }

    public SpeedInventory getSpeedInventory() {
        return speedInventory;
    }

    public StatusInventory getStatusInventory() {
        return statusInventory;
    }

    public WorldsInventory getWorldsInventory() {
        return worldsInventory;
    }

    public PlayerMoveListener getPlayerMoveListener() {
        return playerMoveListener;
    }

    public PlayerTeleportListener getPlayerTeleportListener() {
        return playerTeleportListener;
    }

    public CustomBlocks getCustomBlocks() {
        return customBlocks;
    }

    public GameRules getGameRules() {
        return gameRules;
    }

    public SkullCache getSkullCache() {
        return skullCache;
    }

    public String getStatus(BuildWorld buildWorld) {
        if (buildWorld == null) {
            return "§f-";
        }
        return buildWorld.getStatusName();
    }

    public String getPermission(BuildWorld buildWorld) {
        if (buildWorld == null) {
            return "§f-";
        }
        return buildWorld.getPermission();
    }

    public String getProject(BuildWorld buildWorld) {
        if (buildWorld == null) {
            return "§f-";
        }
        return buildWorld.getProject();
    }

    public String getCreator(BuildWorld buildWorld) {
        if (buildWorld == null) {
            return "§f-";
        }
        return buildWorld.getCreator();
    }

    public String getWorldTime(BuildWorld buildWorld) {
        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return "?";
        }
        return String.valueOf(bukkitWorld.getTime());
    }

    public String getCreationDate(BuildWorld buildWorld) {
        if (buildWorld == null) {
            return "§f-";
        }
        return formatDate(buildWorld.getCreationDate());
    }

    public String formatDate(long date) {
        return date > 0 ? new SimpleDateFormat(getDateFormat()).format(date) : "-";
    }

    public String getBuilders(BuildWorld buildWorld) {
        if (buildWorld == null) {
            return "§f-";
        }

        String template = getString("world_item_builders_builder_template");
        ArrayList<String> builderNames = new ArrayList<>();

        if (isCreatorIsBuilder()) {
            if (buildWorld.getCreator() != null && !buildWorld.getCreator().equals("-")) {
                builderNames.add(buildWorld.getCreator());
            }
        }

        builderNames.addAll(buildWorld.getBuilderNames());

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

    public void forceUpdateSidebar(BuildWorld buildWorld) {
        if (!isScoreboard()) {
            return;
        }

        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        bukkitWorld.getPlayers().forEach(this::forceUpdateSidebar);
    }

    public void forceUpdateSidebar(Player player) {
        if (!isScoreboard() || !settingsManager.getSettings(player).isScoreboard()) {
            return;
        }
        settingsManager.updateScoreboard(player);
    }
}
