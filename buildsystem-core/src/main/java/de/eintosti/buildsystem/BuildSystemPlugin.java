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

import de.eintosti.buildsystem.api.BuildSystem;
import de.eintosti.buildsystem.api.BuildSystemApi;
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.api.world.navigator.settings.NavigatorType;
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
import de.eintosti.buildsystem.command.tabcomplete.BuildTabCompleter;
import de.eintosti.buildsystem.command.tabcomplete.ConfigTabCompleter;
import de.eintosti.buildsystem.command.tabcomplete.EmptyTabCompleter;
import de.eintosti.buildsystem.command.tabcomplete.GamemodeTabCompleter;
import de.eintosti.buildsystem.command.tabcomplete.PhysicsTabCompleter;
import de.eintosti.buildsystem.command.tabcomplete.SpawnTabCompleter;
import de.eintosti.buildsystem.command.tabcomplete.SpeedTabCompleter;
import de.eintosti.buildsystem.command.tabcomplete.TimeTabCompleter;
import de.eintosti.buildsystem.command.tabcomplete.WorldsTabCompleter;
import de.eintosti.buildsystem.config.Config;
import de.eintosti.buildsystem.config.Config.Folder;
import de.eintosti.buildsystem.config.Config.Settings.Archive;
import de.eintosti.buildsystem.config.Config.Settings.Builder;
import de.eintosti.buildsystem.config.Config.World;
import de.eintosti.buildsystem.config.Config.World.Unload;
import de.eintosti.buildsystem.config.migration.ConfigMigrationManager;
import de.eintosti.buildsystem.expansion.luckperms.LuckPermsExpansion;
import de.eintosti.buildsystem.expansion.placeholderapi.PlaceholderApiExpansion;
import de.eintosti.buildsystem.listener.AsyncPlayerChatListener;
import de.eintosti.buildsystem.listener.AsyncPlayerPreLoginListener;
import de.eintosti.buildsystem.listener.BlockPhysicsListener;
import de.eintosti.buildsystem.listener.BuildModePreventationListener;
import de.eintosti.buildsystem.listener.BuildWorldResetUnloadListener;
import de.eintosti.buildsystem.listener.EditSessionListener;
import de.eintosti.buildsystem.listener.EntityDamageListener;
import de.eintosti.buildsystem.listener.EntitySpawnListener;
import de.eintosti.buildsystem.listener.FoodLevelChangeListener;
import de.eintosti.buildsystem.listener.InventoryCreativeListener;
import de.eintosti.buildsystem.listener.InventoryListener;
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
import de.eintosti.buildsystem.player.LogoutLocationImpl;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.settings.NoClipManager;
import de.eintosti.buildsystem.player.settings.SettingsManager;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.util.inventory.InventoryManager;
import de.eintosti.buildsystem.world.SpawnManager;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupService;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.navigator.ArmorStandManager;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class BuildSystemPlugin extends JavaPlugin {

    public static final int SPIGOT_ID = 60441;
    public static final int METRICS_ID = 7427;
    public static final String ADMIN_PERMISSION = "buildsystem.admin";

    private ArmorStandManager armorStandManager;
    private CustomBlockManager customBlockManager;
    private InventoryManager inventoryManager;
    private PlayerServiceImpl playerService;
    private NoClipManager noClipManager;
    private SettingsManager settingsManager;
    private SpawnManager spawnManager;
    private WorldServiceImpl worldService;
    private BackupService backupService;
    private CustomizableIcons customizableIcons;

    private LuckPermsExpansion luckPermsExpansion;
    private PlaceholderApiExpansion placeholderApiExpansion;

    private BuildSystemApi api;

    @Override
    public void onLoad() {
        new ConfigMigrationManager(this).migrate();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        Config.load();

        Messages.createMessageFile();
        createTemplateFolder();
    }

    @Override
    public void onEnable() {
        initClasses();

        registerCommands();
        registerTabCompleters();
        registerListeners();
        registerExpansions();

        performUpdateCheck();

        this.api = new BuildSystemApi(this);
        this.api.register();
        getServer().getServicesManager().register(BuildSystem.class, api, this, ServicePriority.Normal);

        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayer buildPlayer = playerService.getPlayerStorage().createBuildPlayer(pl);
            Settings settings = buildPlayer.getSettings();
            settingsManager.startScoreboard(pl, settings);
            noClipManager.startNoClip(pl, settings);
        });

        registerStats();

        Bukkit.getScheduler().runTaskTimer(this, this::saveBuildConfig, 6000L, 6000L);

        Bukkit.getConsoleSender().sendMessage(
                "%sBuildSystem » Plugin %senabled%s!".formatted(ChatColor.RESET, ChatColor.GREEN, ChatColor.RESET)
        );
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayer buildPlayer = playerService.getPlayerStorage().getBuildPlayer(pl);
            buildPlayer.getCachedValues().resetCachedValues(pl);
            buildPlayer.setLogoutLocation(new LogoutLocationImpl(pl.getWorld().getName(), pl.getLocation()));

            settingsManager.stopScoreboard(pl);
            noClipManager.stopNoClip(pl.getUniqueId());
            playerService.closeNavigator(pl);
        });

        this.backupService.getStorage().close();

        reloadConfigData(false);
        saveConfig();
        saveBuildConfig();

        unregisterExpansions();
        this.api.unregister();

        Bukkit.getConsoleSender().sendMessage(
                "%sBuildSystem » Plugin %sdisabled%s!".formatted(ChatColor.RESET, ChatColor.RED, ChatColor.RESET)
        );
    }

    private void initClasses() {
        this.customizableIcons = new CustomizableIcons(this);

        this.inventoryManager = new InventoryManager();
        this.armorStandManager = new ArmorStandManager();
        this.customBlockManager = new CustomBlockManager(this);
        (this.playerService = new PlayerServiceImpl(this)).init();
        this.noClipManager = new NoClipManager(this);
        (this.worldService = new WorldServiceImpl(this)).init();
        this.backupService = new BackupService(this);
        this.settingsManager = new SettingsManager(this);
        this.spawnManager = new SpawnManager(this);
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

    private void registerTabCompleters() {
        new BuildTabCompleter(this);
        new ConfigTabCompleter(this);
        new EmptyTabCompleter(this);
        new GamemodeTabCompleter(this);
        new PhysicsTabCompleter(this);
        new SpawnTabCompleter(this);
        new SpeedTabCompleter(this);
        new TimeTabCompleter(this);
        new WorldsTabCompleter(this);
    }

    private void registerListeners() {
        new AsyncPlayerChatListener(this);
        new AsyncPlayerPreLoginListener(this);
        new BlockPhysicsListener(this);
        new CustomBlockManager(this);
        new BuildModePreventationListener(this);
        new BuildWorldResetUnloadListener(this);
        new EntitySpawnListener(this);
        new FoodLevelChangeListener(this);
        new InventoryCreativeListener(this);
        new InventoryListener(this);
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
        metrics.addCustomChart(new SimplePie("archive_vanish", () -> String.valueOf(Archive.vanish)));
        metrics.addCustomChart(new SimplePie("block_world_edit", () -> String.valueOf(Builder.blockWorldEditNonBuilder)));
        metrics.addCustomChart(new SimplePie("join_quit_messages", () -> String.valueOf(Config.Messages.joinQuitMessages)));
        metrics.addCustomChart(new SimplePie("lock_weather", () -> String.valueOf(World.lockWeather)));
        metrics.addCustomChart(new SimplePie("scoreboard", () -> String.valueOf(Config.Settings.scoreboard)));
        metrics.addCustomChart(new SimplePie("update_checker", () -> String.valueOf(Config.Settings.updateChecker)));
        metrics.addCustomChart(new SimplePie("unload_worlds", () -> String.valueOf(Unload.enabled)));
        metrics.addCustomChart(new AdvancedPie("navigator_type", new Callable<>() {
            @Override
            public Map<String, Integer> call() {
                Map<String, Integer> valueMap = new HashMap<>();
                valueMap.put("Old", getPlayersWithNavigator(NavigatorType.OLD));
                valueMap.put("New", getPlayersWithNavigator(NavigatorType.NEW));
                return valueMap;
            }

            private int getPlayersWithNavigator(NavigatorType navigatorType) {
                return (int) playerService.getPlayerStorage().getBuildPlayers().stream()
                        .filter(buildPlayer -> buildPlayer.getSettings().getNavigatorType() == navigatorType)
                        .count();
            }
        }));
        metrics.addCustomChart(new SimplePie("folder_override_permissions", () -> String.valueOf(Folder.overridePermissions)));
        metrics.addCustomChart(new SimplePie("folder_override_projects", () -> String.valueOf(Folder.overrideProjects)));
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
        }

        boolean isWorldEdit = pluginManager.getPlugin("WorldEdit") != null
                || pluginManager.getPlugin("FastAsyncWorldEdit") != null;
        if (isWorldEdit && Builder.blockWorldEditNonBuilder) {
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
        if (!Config.Settings.updateChecker) {
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
                        case COULD_NOT_CONNECT, INVALID_JSON, UNAUTHORIZED_QUERY, UNKNOWN_ERROR, UNSUPPORTED_VERSION_SCHEME -> Bukkit.getConsoleSender().sendMessage(
                                ChatColor.RED + "[BuildSystem] Could not check for a new version of BuildSystem. Reason: " + reason
                        );
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
        playerService.save();
        spawnManager.save();
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
        Config.load();

        if (init) {
            worldService.getWorldStorage().getBuildWorlds().forEach(buildWorld -> buildWorld.getUnloader().manageUnload());

            if (Config.Settings.scoreboard) {
                getSettingsManager().startScoreboard();
            } else {
                getSettingsManager().stopScoreboard();
            }
        }
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public CustomBlockManager getCustomBlockManager() {
        return customBlockManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public PlayerServiceImpl getPlayerService() {
        return playerService;
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

    public WorldServiceImpl getWorldService() {
        return worldService;
    }

    public BackupService getBackupService() {
        return backupService;
    }

    public CustomizableIcons getCustomizableIcons() {
        return customizableIcons;
    }
}