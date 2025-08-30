/*
 * Copyright (c) 2018-2025, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem;

import de.eintosti.buildsystem.command.BackCommand;
import de.eintosti.buildsystem.command.BlocksCommand;
import de.eintosti.buildsystem.command.BuildCommand;
import de.eintosti.buildsystem.command.BuildSystemCommand;
import de.eintosti.buildsystem.command.ConfigCommand;
import de.eintosti.buildsystem.command.ExplosionsCommand;
import de.eintosti.buildsystem.command.GamemodeCommand;
import de.eintosti.buildsystem.command.NoAICommand;
import de.eintosti.buildsystem.command.PhysicsCommand;
import de.eintosti.buildsystem.command.SettingsCommand;
import de.eintosti.buildsystem.command.SetupCommand;
import de.eintosti.buildsystem.command.SkullCommand;
import de.eintosti.buildsystem.command.SpawnCommand;
import de.eintosti.buildsystem.command.SpeedCommand;
import de.eintosti.buildsystem.command.TimeCommand;
import de.eintosti.buildsystem.command.TopCommand;
import de.eintosti.buildsystem.command.WorldsCommand;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.expansion.luckperms.LuckPermsExpansion;
import de.eintosti.buildsystem.expansion.placeholderapi.PlaceholderApiExpansion;
import de.eintosti.buildsystem.internal.CraftBukkitVersion;
import de.eintosti.buildsystem.listener.AsyncPlayerChatListener;
import de.eintosti.buildsystem.listener.AsyncPlayerPreLoginListener;
import de.eintosti.buildsystem.listener.BlockPhysicsListener;
import de.eintosti.buildsystem.listener.BlockPlaceListener;
import de.eintosti.buildsystem.listener.BuildModePreventationListener;
import de.eintosti.buildsystem.listener.BuildWorldResetUnloadListener;
import de.eintosti.buildsystem.listener.EditSessionListener;
import de.eintosti.buildsystem.listener.EntityDamageListener;
import de.eintosti.buildsystem.listener.EntitySpawnListener;
import de.eintosti.buildsystem.listener.FoodLevelChangeListener;
import de.eintosti.buildsystem.listener.InventoryCloseListener;
import de.eintosti.buildsystem.listener.InventoryCreativeListener;
import de.eintosti.buildsystem.listener.NavigatorListener;
import de.eintosti.buildsystem.listener.PlayerChangedWorldListener;
import de.eintosti.buildsystem.listener.PlayerCommandPreprocessListener;
import de.eintosti.buildsystem.listener.PlayerInventoryClearListener;
import de.eintosti.buildsystem.listener.PlayerJoinListener;
import de.eintosti.buildsystem.listener.PlayerMoveListener;
import de.eintosti.buildsystem.listener.PlayerQuitListener;
import de.eintosti.buildsystem.listener.PlayerRespawnListener;
import de.eintosti.buildsystem.listener.PlayerTeleportListener;
import de.eintosti.buildsystem.listener.SettingsInteractListener;
import de.eintosti.buildsystem.listener.SignChangeListener;
import de.eintosti.buildsystem.listener.WeatherChangeListener;
import de.eintosti.buildsystem.listener.WorldManipulateByAxiomListener;
import de.eintosti.buildsystem.listener.WorldManipulateListener;
import de.eintosti.buildsystem.navigator.ArmorStandManager;
import de.eintosti.buildsystem.navigator.inventory.ArchiveInventory;
import de.eintosti.buildsystem.navigator.inventory.NavigatorInventory;
import de.eintosti.buildsystem.navigator.inventory.PrivateInventory;
import de.eintosti.buildsystem.navigator.inventory.WorldsInventory;
import de.eintosti.buildsystem.navigator.settings.NavigatorType;
import de.eintosti.buildsystem.player.BlocksInventory;
import de.eintosti.buildsystem.player.BuildPlayer;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.PlayerManager;
import de.eintosti.buildsystem.player.settings.DesignInventory;
import de.eintosti.buildsystem.player.settings.NoClipManager;
import de.eintosti.buildsystem.player.settings.Settings;
import de.eintosti.buildsystem.player.settings.SettingsInventory;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.player.settings.SpeedInventory;
import de.eintosti.buildsystem.tabcomplete.BuildTabComplete;
import de.eintosti.buildsystem.tabcomplete.ConfigTabComplete;
import de.eintosti.buildsystem.tabcomplete.EmptyTabComplete;
import de.eintosti.buildsystem.tabcomplete.GamemodeTabComplete;
import de.eintosti.buildsystem.tabcomplete.PhysicsTabComplete;
import de.eintosti.buildsystem.tabcomplete.SpawnTabComplete;
import de.eintosti.buildsystem.tabcomplete.SpeedTabComplete;
import de.eintosti.buildsystem.tabcomplete.TimeTabComplete;
import de.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.version.customblocks.CustomBlocks;
import de.eintosti.buildsystem.version.gamerules.GameRules;
import de.eintosti.buildsystem.version.util.MinecraftVersion;
import de.eintosti.buildsystem.world.BuildWorld;
import de.eintosti.buildsystem.world.SpawnManager;
import de.eintosti.buildsystem.world.WorldService;
import de.eintosti.buildsystem.world.data.StatusInventory;
import de.eintosti.buildsystem.world.display.WorldIcon;
import de.eintosti.buildsystem.world.modification.BuilderInventory;
import de.eintosti.buildsystem.world.modification.CreateInventory;
import de.eintosti.buildsystem.world.modification.DeleteInventory;
import de.eintosti.buildsystem.world.modification.EditInventory;
import de.eintosti.buildsystem.world.modification.GameRuleInventory;
import de.eintosti.buildsystem.world.modification.SetupInventory;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BuildSystem extends JavaPlugin {

    public static final int SPIGOT_ID = 60441;
    public static final int METRICS_ID = 7427;
    public static final String ADMIN_PERMISSION = "buildsystem.admin";

    private CraftBukkitVersion craftBukkitVersion;

    private ArmorStandManager armorStandManager;
    private NoClipManager noClipManager;
    private PlayerManager playerManager;
    private SettingsManager settingsManager;
    private SpawnManager spawnManager;
    private WorldService worldService;

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
    private WorldIcon worldIcon;

    private LuckPermsExpansion luckPermsExpansion;
    private PlaceholderApiExpansion placeholderApiExpansion;

    @Override
    public void onLoad() {
        createTemplateFolder();
        Messages.createMessageFile();
    }

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        initClasses();
        if (!initVersionedClasses()) {
            this.setEnabled(false);
            return;
        }

        registerCommands();
        registerTabCompleter();
        registerListeners();
        registerExpansions();

        performUpdateCheck();

        playerManager.load();
        spawnManager.load();

        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayer buildPlayer = playerManager.createBuildPlayer(pl);
            Settings settings = buildPlayer.getSettings();
            settingsManager.startScoreboard(pl, settings);
            noClipManager.startNoClip(pl, settings);
        });

        registerStats();

        Bukkit.getScheduler().runTaskTimer(this, this::saveBuildConfig, 6000L, 6000L);

        Bukkit.getConsoleSender().sendMessage(String.format(Locale.ROOT,
                "%sBuildSystem » Plugin %senabled%s!",
                ChatColor.RESET, ChatColor.GREEN, ChatColor.RESET
        ));
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayer buildPlayer = playerManager.getBuildPlayer(pl);
            buildPlayer.getCachedValues().resetCachedValues(pl);
            buildPlayer.setLogoutLocation(new LogoutLocation(pl.getWorld().getName(), pl.getLocation()));

            settingsManager.stopScoreboard(pl);
            noClipManager.stopNoClip(pl.getUniqueId());
            playerManager.closeNavigator(pl);
        });

        reloadConfigData(false);
        saveConfig();
        saveBuildConfig();

        unregisterExpansions();

        Bukkit.getConsoleSender().sendMessage(String.format(Locale.ROOT,
                "%sBuildSystem » Plugin %sdisabled%s!",
                ChatColor.RESET, ChatColor.RED, ChatColor.RESET
        ));
    }

    private boolean initVersionedClasses() {
        MinecraftVersion minecraftVersion = MinecraftVersion.getCurrent();
        if (minecraftVersion == null) {
            return false;
        }

        this.craftBukkitVersion = CraftBukkitVersion.matchCraftBukkitVersion(minecraftVersion);
        if (craftBukkitVersion == CraftBukkitVersion.UNKNOWN) {
            getLogger().severe("BuildSystem does not support your server version: " + minecraftVersion);
            getLogger().severe("If you wish to enable the plugin anyway, start your server with the '-DPaper.ignoreWorldDataVersion=true' flag");
            getLogger().severe("Disabling plugin...");
            return false;
        }

        getLogger().info(String.format(Locale.ROOT, "Detected server version: %s (%s)", minecraftVersion, craftBukkitVersion.name()));
        this.customBlocks = craftBukkitVersion.initCustomBlocks();
        this.gameRules = craftBukkitVersion.initGameRules();
        return true;
    }

    private void initClasses() {
        this.configValues = new ConfigValues(this);
        this.worldIcon = new WorldIcon(this);

        this.armorStandManager = new ArmorStandManager();
        this.playerManager = new PlayerManager(this);
        this.noClipManager = new NoClipManager(this);
        this.worldService = new WorldService(this);
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
        new WorldsCommand(this);
    }

    private void registerTabCompleter() {
        new BuildTabComplete(this);
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
        new BuildModePreventationListener(this);
        new BuildWorldResetUnloadListener(this);
        new EntitySpawnListener(this);
        new FoodLevelChangeListener(this);
        new InventoryCloseListener(this);
        new InventoryCreativeListener(this);
        new NavigatorListener(this);
        new PlayerChangedWorldListener(this);
        new EntityDamageListener(this);
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
        metrics.addCustomChart(new AdvancedPie("navigator_type", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                Map<String, Integer> valueMap = new HashMap<>();
                valueMap.put("Old", getPlayersWithNavigator(NavigatorType.OLD));
                valueMap.put("New", getPlayersWithNavigator(NavigatorType.NEW));
                return valueMap;
            }

            private int getPlayersWithNavigator(NavigatorType navigatorType) {
                return (int) playerManager.getBuildPlayers().stream()
                        .filter(buildPlayer -> buildPlayer.getSettings().getNavigatorType() == navigatorType)
                        .count();
            }
        }));
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

        if (pluginManager.getPlugin("AxiomPaper") != null) {
            new WorldManipulateByAxiomListener(this);
            getLogger().info("Axiom build-world manipulation prevention has been enabled.");
        }

        boolean isWorldEdit = pluginManager.getPlugin("WorldEdit") != null
                || pluginManager.getPlugin("FastAsyncWorldEdit") != null;
        if (isWorldEdit && configValues.isBlockWorldEditNonBuilder()) {
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
                        Bukkit.getConsoleSender().sendMessage(
                                ChatColor.YELLOW + "[BuildSystem] Great! a new update is available: "
                                        + ChatColor.GREEN + "v" + result.getNewestVersion()
                        );
                        Bukkit.getConsoleSender().sendMessage(
                                ChatColor.YELLOW + " ➥ Your current version: " +
                                        ChatColor.RED + this.getDescription().getVersion()
                        );
                        return;
                    }

                    UpdateChecker.UpdateReason reason = result.getReason();
                    switch (reason) {
                        case COULD_NOT_CONNECT:
                        case INVALID_JSON:
                        case UNAUTHORIZED_QUERY:
                        case UNKNOWN_ERROR:
                        case UNSUPPORTED_VERSION_SCHEME:
                            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +
                                    "[BuildSystem] Could not check for a new version of BuildSystem. Reason: " + reason
                            );
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

    private void saveBuildConfig() {
        worldService.save();
        playerManager.save();
        spawnManager.save();
    }

    public void sendPermissionMessage(CommandSender sender) {
        Messages.sendMessage(sender, "no_permissions");
    }

    /**
     * Reloads the config and config data.
     *
     * @param init Whether the plugin should reinitialize classes
     */
    public void reloadConfigData(boolean init) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            getSettingsManager().stopScoreboard(pl);
        }

        reloadConfig();
        configValues.setConfigValues();

        if (init) {
            initVersionedClasses();
            worldService.getWorldStorage().getBuildWorlds().forEach(BuildWorld::manageUnload);
            if (configValues.isScoreboard()) {
                getSettingsManager().startScoreboard();
            } else {
                getSettingsManager().stopScoreboard();
            }
        }
    }

    public CraftBukkitVersion getCraftBukkitVersion() {
        return craftBukkitVersion;
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
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

    public WorldService getWorldService() {
        return worldService;
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

    public WorldIcon getWorldIcon() {
        return worldIcon;
    }
}