/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem;

import com.eintosti.buildsystem.command.*;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.expansion.luckperms.LuckPermsExpansion;
import com.eintosti.buildsystem.expansion.placeholderapi.PlaceholderApiExpansion;
import com.eintosti.buildsystem.inventory.*;
import com.eintosti.buildsystem.listener.*;
import com.eintosti.buildsystem.manager.ArmorStandManager;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.NoClipManager;
import com.eintosti.buildsystem.manager.PlayerManager;
import com.eintosti.buildsystem.manager.SettingsManager;
import com.eintosti.buildsystem.manager.SpawnManager;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.internal.ServerVersion;
import com.eintosti.buildsystem.object.settings.Settings;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.tabcomplete.ConfigTabComplete;
import com.eintosti.buildsystem.tabcomplete.EmptyTabComplete;
import com.eintosti.buildsystem.tabcomplete.GamemodeTabComplete;
import com.eintosti.buildsystem.tabcomplete.PhysicsTabComplete;
import com.eintosti.buildsystem.tabcomplete.SpawnTabComplete;
import com.eintosti.buildsystem.tabcomplete.SpeedTabComplete;
import com.eintosti.buildsystem.tabcomplete.TimeTabComplete;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.eintosti.buildsystem.util.Messages;
import com.eintosti.buildsystem.util.RBGUtils;
import com.eintosti.buildsystem.util.SkullCache;
import com.eintosti.buildsystem.util.external.UpdateChecker;
import com.eintosti.buildsystem.version.CustomBlocks;
import com.eintosti.buildsystem.version.GameRules;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author einTosti
 */
public class BuildSystem extends JavaPlugin {

    public static final int SPIGOT_ID = 60441;
    public static final int METRICS_ID = 7427;

    private String version;

    private WorldsCommand worldsCommand;

    private ArmorStandManager armorStandManager;
    private InventoryManager inventoryManager;
    private NoClipManager noClipManager;
    private PlayerManager playerManager;
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

    private Messages messages;
    private ConfigValues configValues;
    private CustomBlocks customBlocks;
    private GameRules gameRules;
    private SkullCache skullCache;

    private LuckPermsExpansion luckPermsExpansion;
    private PlaceholderApiExpansion placeholderApiExpansion;

    @Override
    public void onLoad() {
        createLanguageFile();
        createTemplateFolder();
        parseServerVersion();
    }

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.configValues = new ConfigValues(this);

        initClasses();
        if (!initVersionedClasses()) {
            getLogger().severe("BuildSystem does not support your server version: " + version);
            getLogger().severe("Disabling plugin...");
            this.setEnabled(false);
            return;
        }

        registerCommands();
        registerTabCompleter();
        registerListeners();
        registerStats();
        registerExpansions();

        performUpdateCheck();

        worldManager.load();
        settingsManager.load();
        spawnManager.load();

        Bukkit.getOnlinePlayers().forEach(pl -> {
            getSkullCache().cacheSkull(pl.getName());

            Settings settings = settingsManager.createSettings(pl);
            settingsManager.startScoreboard(pl, settings);
            noClipManager.startNoClip(pl, settings);
        });

        Bukkit.getConsoleSender().sendMessage(ChatColor.RESET + "BuildSystem » Plugin " + ChatColor.GREEN + "enabled" + ChatColor.RESET + "!");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(pl -> {
            settingsManager.stopScoreboard(pl);
            noClipManager.stopNoClip(pl.getUniqueId());
            playerManager.closeNavigator(pl);
        });

        reloadConfig();
        reloadConfigData(false);
        saveConfig();

        worldManager.save();
        settingsManager.save();
        spawnManager.save();
        inventoryManager.save();

        unregisterExpansions();

        Bukkit.getConsoleSender().sendMessage(ChatColor.RESET + "BuildSystem » Plugin " + ChatColor.RED + "disabled" + ChatColor.RESET + "!");
    }

    private boolean initVersionedClasses() {
        ServerVersion serverVersion = ServerVersion.matchServerVersion(version);
        if (serverVersion == ServerVersion.UNKNOWN) {
            return false;
        }

        this.customBlocks = serverVersion.initCustomBlocks();
        this.gameRules = serverVersion.initGameRules();

        return true;
    }

    private void initClasses() {
        this.armorStandManager = new ArmorStandManager();
        this.playerManager = new PlayerManager(this);
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

        this.skullCache = new SkullCache(version);
    }

    private void parseServerVersion() {
        try {
            this.version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            getLogger().info("Detected server version: " + version);
        } catch (ArrayIndexOutOfBoundsException e) {
            getLogger().severe("Unknown server version");
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
        new BuildWorldResetUnloadListener(this);
        new EntitySpawnListener(this);
        new FoodLevelChangeListener(this);
        new InventoryCloseListener(this);
        new InventoryCreativeListener(this);
        new NavigatorListener(this);
        new PlayerChangedWorldListener(this);
        new PlayerCommandPreprocessListener(this);
        new PlayerInventoryClearListener(this);
        new PlayerJoinListener(this);
        new PlayerMoveListener(this);
        new PlayerQuitListener(this);
        new PlayerRespawnListener(this);
        new PlayerTeleportListener(this);
        new SettingsInteractListener(this);
        new SignChangeListener(this);
        new WeatherChangeListener(this);
        new WorldManipulateListener(this);
    }

    private void registerStats() {
        Metrics metrics = new Metrics(this, METRICS_ID);
        metrics.addCustomChart(new SimplePie("archive_vanish", () -> String.valueOf(configValues.isArchiveVanish())));
        metrics.addCustomChart(new SimplePie("block_world_edit", () -> String.valueOf(configValues.isBlockWorldEditNonBuilder())));
        metrics.addCustomChart(new SimplePie("join_quit_messages", () -> String.valueOf(configValues.isJoinQuitMessages())));
        metrics.addCustomChart(new SimplePie("lock_weather", () -> String.valueOf(configValues.isLockWeather())));
        metrics.addCustomChart(new SimplePie("scoreboard", () -> String.valueOf(configValues.isScoreboard())));
        metrics.addCustomChart(new SimplePie("teleport_after_creation", () -> String.valueOf(configValues.isTeleportAfterCreation())));
        metrics.addCustomChart(new SimplePie("update_checker", () -> String.valueOf(configValues.isUpdateChecker())));
        metrics.addCustomChart(new SimplePie("unload_worlds", () -> String.valueOf(configValues.isUnloadWorlds())));
        metrics.addCustomChart(new SimplePie("void_block", () -> String.valueOf(configValues.isVoidBlock())));
    }

    private void registerExpansions() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        if (pluginManager.getPlugin("PlaceholderAPI") != null) {
            this.placeholderApiExpansion = new PlaceholderApiExpansion(this);
            this.placeholderApiExpansion.register();
        }

        if (pluginManager.getPlugin("LuckPerms") != null) {
            this.luckPermsExpansion = new LuckPermsExpansion(this);
            this.luckPermsExpansion.registerAll();
        }

        if (configValues.isBlockWorldEditNonBuilder() && (pluginManager.getPlugin("FastAsyncWorldEdit") != null || pluginManager.getPlugin("WorldEdit") != null)) {
            new EditSessionListener(this);
        }
    }

    private void unregisterExpansions() {
        if (this.placeholderApiExpansion != null) {
            this.placeholderApiExpansion.unregister();
        }

        if (this.luckPermsExpansion != null) {
            this.luckPermsExpansion.unregisterAll();
        }
    }

    private void performUpdateCheck() {
        if (!configValues.isUpdateChecker()) {
            return;
        }

        UpdateChecker.init(this, SPIGOT_ID).requestUpdateCheck().whenComplete((result, e) -> {
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
            getLogger().info("Created \"templates\" folder");
        }
    }

    private void createLanguageFile() {
        if (getDataFolder().mkdir()) {
            getLogger().info("Created \"BuildSystem\" folder");
        }

        this.messages = new Messages(this);
        messages.createMessageFile();
    }

    public String getPrefixString() {
        String prefix = messages.getMessageData().get("prefix");
        try {
            final String defaultPrefix = "§8× §bBuildSystem §8┃";
            return prefix != null ? ChatColor.translateAlternateColorCodes('&', RBGUtils.color(prefix)) : defaultPrefix;
        } catch (NullPointerException e) {
            messages.createMessageFile();
            return getPrefixString();
        }
    }

    public String getString(String key) {
        try {
            String message = messages.getMessageData().get(key).replace("%prefix%", getPrefixString());
            return ChatColor.translateAlternateColorCodes('&', RBGUtils.color(message));
        } catch (NullPointerException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BuildSystem] Could not find message with key: " + key);
            messages.createMessageFile();
            return getString(key);
        }
    }

    public List<String> getStringList(String key) {
        try {
            List<String> list = new ArrayList<>();
            String string = messages.getMessageData().get(key);
            String[] splitString = string.substring(1, string.length() - 1).split(", ");
            for (String s : splitString) {
                String message = s.replace("%prefix%", getPrefixString());
                list.add(ChatColor.translateAlternateColorCodes('&', RBGUtils.color(message)));
            }
            return list;
        } catch (NullPointerException e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BuildSystem] Could not get list with key: " + key);
            messages.createMessageFile();
            return getStringList(key);
        }
    }

    public boolean canBypass(Player player) {
        return player.hasPermission("buildsystem.admin")
                || player.hasPermission("buildsystem.bypass.archive")
                || playerManager.getBuildPlayers().contains(player.getUniqueId());
    }

    public boolean isPermitted(Player player, String permission) {
        if (player.hasPermission("buildsystem.admin")) {
            return true;
        }

        try {
            UUID creator = worldManager.getBuildWorld(player.getWorld()).getCreatorId();
            if (player.getUniqueId().equals(creator)) {
                return (player.hasPermission(permission + ".self") || player.hasPermission(permission));
            }
            return player.hasPermission(permission + ".other");
        } catch (NullPointerException e) {
            return false;
        }
    }

    public void sendPermissionMessage(CommandSender sender) {
        sender.sendMessage(getString("no_permissions"));
    }

    public void reloadConfigData(boolean init) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            getSettingsManager().stopScoreboard(pl);
        }

        configValues.setConfigValues();

        if (init) {
            initVersionedClasses();
            worldManager.getBuildWorlds().forEach(BuildWorld::manageUnload);
            if (configValues.isScoreboard()) {
                getSettingsManager().startScoreboard();
            } else {
                getSettingsManager().stopScoreboard();
            }
        }
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

    public PlayerManager getPlayerManager() {
        return playerManager;
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

    public ConfigValues getConfigValues() {
        return configValues;
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
}