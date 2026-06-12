/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
import de.eintosti.buildsystem.command.CommandRegistrar;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.PluginConfig;
import de.eintosti.buildsystem.config.migration.ConfigMigrationManager;
import de.eintosti.buildsystem.expansion.luckperms.LuckPermsExpansion;
import de.eintosti.buildsystem.expansion.placeholderapi.PlaceholderApiExpansion;
import de.eintosti.buildsystem.listener.ListenerRegistrar;
import de.eintosti.buildsystem.player.LogoutLocationImpl;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.settings.NoClipManager;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.world.SpawnManager;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupService;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.navigator.ArmorStandManager;
import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class BuildSystemPlugin extends JavaPlugin {

    public static final int SPIGOT_ID = 60441;
    public static final int METRICS_ID = 7427;
    public static final String ADMIN_PERMISSION = "buildsystem.admin";

    private static BuildSystemPlugin instance;

    private ConfigService configService;
    private de.eintosti.buildsystem.i18n.Messages messages;

    private ArmorStandManager armorStandManager;
    private CustomBlockManager customBlockManager;
    private PlayerServiceImpl playerService;
    private NoClipManager noClipManager;
    private SettingsService settingsService;
    private SpawnManager spawnManager;
    private WorldServiceImpl worldService;
    private BackupService backupService;
    private CustomizableIcons customizableIcons;

    private LuckPermsExpansion luckPermsExpansion;
    private PlaceholderApiExpansion placeholderApiExpansion;

    private BuildSystemApi api;

    private BukkitTask configSaveTask;

    @Override
    public void onLoad() {
        instance = this;

        this.configService = new ConfigService(this);
        new ConfigMigrationManager(this).migrate();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.configService.load();

        this.messages = new de.eintosti.buildsystem.i18n.Messages(this, configService);
        this.messages.load();
        createTemplateFolder();
    }

    @Override
    public void onEnable() {
        initClasses();

        new CommandRegistrar(this).registerAll();
        new ListenerRegistrar(this).registerAll();
        registerExpansions();

        performUpdateCheck();

        this.api = new BuildSystemApi(this);
        this.api.register();
        getServer().getServicesManager().register(BuildSystem.class, api, this, ServicePriority.Normal);

        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayer buildPlayer = playerService.getPlayerStorage().createBuildPlayer(pl);
            Settings settings = buildPlayer.getSettings();
            noClipManager.startNoClip(pl, settings);
            settingsService.displayScoreboard(pl);
        });

        registerStats();

        this.configSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveBuildConfig, 6000L, 6000L); // Every 5 minutes

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

            settingsService.hideScoreboard(pl);
            noClipManager.stopNoClip(pl.getUniqueId());
            playerService.closeNewNavigator(pl);
        });

        this.backupService.close();
        worldService.cancelAllUnloadTasks();

        reloadConfigData(false);
        saveConfig();
        try {
            saveBuildConfig().join();
        } catch (CompletionException e) {
            getLogger().severe("Error while waiting for saves: " + e.getCause());
        }

        if (this.configSaveTask != null) {
            this.configSaveTask.cancel();
        }

        unregisterExpansions();
        this.api.unregister();

        Bukkit.getConsoleSender().sendMessage(
                "%sBuildSystem » Plugin %sdisabled%s!".formatted(ChatColor.RESET, ChatColor.RED, ChatColor.RESET)
        );

        instance = null;
    }

    public static BuildSystemPlugin get() {
        if (instance == null) {
            throw new IllegalStateException("BuildSystemPlugin instance is not initialized. Make sure the plugin is enabled.");
        }
        return instance;
    }

    private void initClasses() {
        this.customizableIcons = new CustomizableIcons(this);

        this.armorStandManager = new ArmorStandManager();
        this.customBlockManager = new CustomBlockManager(this);
        (this.playerService = new PlayerServiceImpl(this)).init();
        this.noClipManager = new NoClipManager(this);
        (this.worldService = new WorldServiceImpl(this)).init();
        this.backupService = new BackupService(this);
        this.settingsService = new SettingsService(this);
        this.spawnManager = new SpawnManager(this);
    }

    private void registerStats() {
        Metrics metrics = new Metrics(this, METRICS_ID);
        metrics.addCustomChart(new SimplePie("archive_vanish", () -> String.valueOf(configService.current().settings().archive().vanish())));
        metrics.addCustomChart(new SimplePie("block_world_edit", () -> String.valueOf(configService.current().settings().builder().blockWorldEditNonBuilder())));
        metrics.addCustomChart(new SimplePie("join_quit_messages", () -> String.valueOf(configService.current().messages().joinQuitMessages())));
        metrics.addCustomChart(new SimplePie("lock_weather", () -> String.valueOf(configService.current().world().lockWeather())));
        metrics.addCustomChart(new SimplePie("scoreboard", () -> String.valueOf(configService.current().settings().scoreboard())));
        metrics.addCustomChart(new SimplePie("update_checker", () -> String.valueOf(configService.current().settings().updateChecker())));
        metrics.addCustomChart(new SimplePie("unload_worlds", () -> String.valueOf(configService.current().world().unload().enabled())));
        metrics.addCustomChart(new AdvancedPie("navigator_type", () -> {
            Map<NavigatorType, Long> countsByType = playerService.getPlayerStorage().getBuildPlayers().stream()
                    .collect(Collectors.groupingBy(
                            buildPlayer -> buildPlayer.getSettings().getNavigatorType(),
                            Collectors.counting()
                    ));
            int oldCount = countsByType.getOrDefault(NavigatorType.OLD, 0L).intValue();
            int newCount = countsByType.getOrDefault(NavigatorType.NEW, 0L).intValue();
            return Map.of("Old", oldCount, "New", newCount);
        }));
        metrics.addCustomChart(new SimplePie("folder_override_permissions", () -> String.valueOf(configService.current().folder().overridePermissions())));
        metrics.addCustomChart(new SimplePie("folder_override_projects", () -> String.valueOf(configService.current().folder().overrideProjects())));
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
        if (!configService.current().settings().updateChecker()) {
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

    private CompletableFuture<Void> saveBuildConfig() {
        CompletableFuture<Void> worldSave = worldService.save();
        CompletableFuture<Void> playerSave = playerService.save();
        CompletableFuture<Void> spawnSave = spawnManager.save();
        return CompletableFuture.allOf(worldSave, playerSave, spawnSave);
    }

    /**
     * Reloads the config and config data.
     *
     * @param init Whether the plugin should reinitialize classes
     */
    public void reloadConfigData(boolean init) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            getSettingsService().hideScoreboard(pl);
        }

        reloadConfig();
        configService.load();
        if (isEnabled()) {
            backupService.reload();
        }

        if (init) {
            worldService.remanageAllUnloadTasks();

            if (configService.current().settings().scoreboard()) {
                getSettingsService().displayScoreboard();
            } else {
                getSettingsService().hideScoreboards();
            }
        }
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public CustomBlockManager getCustomBlockManager() {
        return customBlockManager;
    }

    public PlayerServiceImpl getPlayerService() {
        return playerService;
    }

    public NoClipManager getNoClipManager() {
        return noClipManager;
    }

    public SettingsService getSettingsService() {
        return settingsService;
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

    public ConfigService getConfigService() {
        return configService;
    }

    public de.eintosti.buildsystem.i18n.Messages getMessages() {
        return messages;
    }

    public CustomizableIcons getCustomizableIcons() {
        return customizableIcons;
    }
}