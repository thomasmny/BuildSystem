/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem;

import com.eintosti.buildsystem.command.BackCommand;
import com.eintosti.buildsystem.command.BlocksCommand;
import com.eintosti.buildsystem.command.BuildCommand;
import com.eintosti.buildsystem.command.BuildSystemCommand;
import com.eintosti.buildsystem.command.ConfigCommand;
import com.eintosti.buildsystem.command.ExplosionsCommand;
import com.eintosti.buildsystem.command.GamemodeCommand;
import com.eintosti.buildsystem.command.NoAICommand;
import com.eintosti.buildsystem.command.PhysicsCommand;
import com.eintosti.buildsystem.command.SettingsCommand;
import com.eintosti.buildsystem.command.SetupCommand;
import com.eintosti.buildsystem.command.SkullCommand;
import com.eintosti.buildsystem.command.SpawnCommand;
import com.eintosti.buildsystem.command.SpeedCommand;
import com.eintosti.buildsystem.command.TimeCommand;
import com.eintosti.buildsystem.command.TopCommand;
import com.eintosti.buildsystem.command.WorldsCommand;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.expansion.luckperms.LuckPermsExpansion;
import com.eintosti.buildsystem.expansion.placeholderapi.PlaceholderApiExpansion;
import com.eintosti.buildsystem.inventory.ArchiveInventory;
import com.eintosti.buildsystem.inventory.BlocksInventory;
import com.eintosti.buildsystem.inventory.BuilderInventory;
import com.eintosti.buildsystem.inventory.CreateInventory;
import com.eintosti.buildsystem.inventory.DeleteInventory;
import com.eintosti.buildsystem.inventory.DesignInventory;
import com.eintosti.buildsystem.inventory.EditInventory;
import com.eintosti.buildsystem.inventory.GameRuleInventory;
import com.eintosti.buildsystem.inventory.NavigatorInventory;
import com.eintosti.buildsystem.inventory.PrivateInventory;
import com.eintosti.buildsystem.inventory.SettingsInventory;
import com.eintosti.buildsystem.inventory.SetupInventory;
import com.eintosti.buildsystem.inventory.SpeedInventory;
import com.eintosti.buildsystem.inventory.StatusInventory;
import com.eintosti.buildsystem.inventory.WorldsInventory;
import com.eintosti.buildsystem.listener.*;
import com.eintosti.buildsystem.manager.ArmorStandManager;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.NoClipManager;
import com.eintosti.buildsystem.manager.PlayerManager;
import com.eintosti.buildsystem.manager.SettingsManager;
import com.eintosti.buildsystem.manager.SpawnManager;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.internal.ServerVersion;
import com.eintosti.buildsystem.object.player.BuildPlayer;
import com.eintosti.buildsystem.object.player.LogoutLocation;
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
import com.eintosti.buildsystem.util.RGBUtils;
import com.eintosti.buildsystem.util.SkullCache;
import com.eintosti.buildsystem.util.external.UpdateChecker;
import com.eintosti.buildsystem.version.customblocks.CustomBlocks;
import com.eintosti.buildsystem.version.gamerules.GameRules;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author einTosti
 */
public class BuildSystem extends JavaPlugin {

    public static final int SPIGOT_ID = 60441;
    public static final int METRICS_ID = 7427;
    public static final String ADMIN_PERMISSION = "buildsystem.admin";

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

    private ConfigValues configValues;
    private CustomBlocks customBlocks;
    private GameRules gameRules;
    private SkullCache skullCache;

    private LuckPermsExpansion luckPermsExpansion;
    private PlaceholderApiExpansion placeholderApiExpansion;

    @Override
    public void onLoad() {
        createTemplateFolder();
        parseServerVersion();
        Messages.createMessageFile();
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
        settingsManager.load(); //TODO: Remove in v3.0
        playerManager.load();
        spawnManager.load();

        Bukkit.getOnlinePlayers().forEach(pl -> {
            getSkullCache().cacheSkull(pl.getName());

            BuildPlayer buildPlayer = playerManager.createBuildPlayer(pl);
            Settings settings = buildPlayer.getSettings();
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
            playerManager.getBuildPlayer(pl).setLogoutLocation(new LogoutLocation(pl.getWorld().getName(), pl.getLocation()));
        });

        reloadConfig();
        reloadConfigData(false);
        saveConfig();

        worldManager.save();
        playerManager.save();
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
        new AsyncPlayerPreLoginListener(this);
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
                        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "[BuildSystem] Great! a new update is available: " + ChatColor.GREEN + "v" + result.getNewestVersion());
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
        if (templateFolder.mkdirs()) {
            getLogger().info("Created \"templates\" folder");
        }
    }

    public void sendPermissionMessage(CommandSender sender) {
        Messages.sendMessage(sender, "no_permissions");
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