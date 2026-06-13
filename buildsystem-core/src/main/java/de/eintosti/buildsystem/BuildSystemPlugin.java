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
import de.eintosti.buildsystem.api.player.BuildPlayer;
import de.eintosti.buildsystem.api.player.settings.NavigatorType;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.command.CommandRegistrar;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.migration.ConfigMigrationManager;
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.integration.Integrations;
import de.eintosti.buildsystem.listener.ListenerRegistrar;
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.navigator.NavigatorService;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.player.PlayerLookupService;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import de.eintosti.buildsystem.player.customblock.CustomBlockManager;
import de.eintosti.buildsystem.player.noclip.NoClipService;
import de.eintosti.buildsystem.player.settings.SettingsService;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.world.WorldServiceImpl;
import de.eintosti.buildsystem.world.backup.BackupService;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.spawn.SpawnService;
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
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class BuildSystemPlugin extends JavaPlugin {

    public static final int SPIGOT_ID = 60441;
    public static final int METRICS_ID = 7427;
    public static final String ADMIN_PERMISSION = "buildsystem.admin";

    private Services services;

    private UpdateChecker updateChecker;

    private Integrations integrations;

    private BuildSystemApi api;

    private BukkitTask configSaveTask;

    @Override
    public void onLoad() {
        this.services = new Services(this);

        ConfigService configService = this.services.createConfigService();
        new ConfigMigrationManager(this).migrate();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        configService.load();

        this.services.createMessages().load();
        createTemplateFolder();
    }

    @Override
    public void onEnable() {
        this.services.initClasses();

        new CommandRegistrar(this).registerAll();
        new ListenerRegistrar(this).registerAll();
        (this.integrations = new Integrations(this)).activate();

        performUpdateCheck();

        this.api = new BuildSystemApi(this);
        getServer().getServicesManager().register(BuildSystem.class, api, this, ServicePriority.Normal);

        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayer buildPlayer = getPlayerService().getPlayerStorage().createBuildPlayer(pl);
            Settings settings = buildPlayer.getSettings();
            getNoClipService().startNoClip(pl, settings);
            getSettingsService().displayScoreboard(pl);
        });

        registerStats();

        this.configSaveTask = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(this, this::saveBuildConfig, 6000L, 6000L); // Every 5 minutes

        Bukkit.getConsoleSender()
                .sendMessage("%sBuildSystem » Plugin %senabled%s!"
                        .formatted(ChatColor.RESET, ChatColor.GREEN, ChatColor.RESET));
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayerImpl buildPlayer =
                    BuildPlayerImpl.of(getPlayerService().getPlayerStorage().getBuildPlayer(pl));
            buildPlayer.getCachedValues().resetCachedValues(pl);
            buildPlayer.setLogoutLocation(new LogoutLocation(pl.getWorld().getName(), pl.getLocation()));

            getSettingsService().hideScoreboard(pl);
            getNoClipService().stopNoClip(pl.getUniqueId());
            getNavigatorService().closeNewNavigator(pl);
        });

        getBackupService().close();
        getWorldService().cancelAllUnloadTasks();

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

        this.integrations.deactivate();
        getServer().getServicesManager().unregister(BuildSystem.class, api);

        Bukkit.getConsoleSender()
                .sendMessage("%sBuildSystem » Plugin %sdisabled%s!"
                        .formatted(ChatColor.RESET, ChatColor.RED, ChatColor.RESET));
    }

    private void registerStats() {
        Metrics metrics = new Metrics(this, METRICS_ID);
        metrics.addCustomChart(new SimplePie(
                "archive_vanish",
                () -> String.valueOf(
                        getConfigService().current().settings().archive().vanish())));
        metrics.addCustomChart(new SimplePie(
                "block_world_edit",
                () -> String.valueOf(
                        getConfigService().current().settings().builder().blockWorldEditNonBuilder())));
        metrics.addCustomChart(new SimplePie(
                "join_quit_messages",
                () -> String.valueOf(getConfigService().current().settings().joinQuitMessages())));
        metrics.addCustomChart(new SimplePie(
                "lock_weather",
                () -> String.valueOf(getConfigService().current().world().lockWeather())));
        metrics.addCustomChart(new SimplePie(
                "scoreboard",
                () -> String.valueOf(getConfigService().current().settings().scoreboard())));
        metrics.addCustomChart(new SimplePie(
                "update_checker",
                () -> String.valueOf(getConfigService().current().settings().updateChecker())));
        metrics.addCustomChart(new SimplePie(
                "unload_worlds",
                () -> String.valueOf(
                        getConfigService().current().world().unload().enabled())));
        metrics.addCustomChart(new AdvancedPie("navigator_type", () -> {
            Map<NavigatorType, Long> countsByType = getPlayerService().getPlayerStorage().getBuildPlayers().stream()
                    .collect(Collectors.groupingBy(
                            buildPlayer -> buildPlayer.getSettings().getNavigatorType(), Collectors.counting()));
            int oldCount = countsByType.getOrDefault(NavigatorType.OLD, 0L).intValue();
            int newCount = countsByType.getOrDefault(NavigatorType.NEW, 0L).intValue();
            return Map.of("Old", oldCount, "New", newCount);
        }));
        metrics.addCustomChart(new SimplePie(
                "folder_override_permissions",
                () -> String.valueOf(getConfigService().current().folder().overridePermissions())));
        metrics.addCustomChart(new SimplePie(
                "folder_override_projects",
                () -> String.valueOf(getConfigService().current().folder().overrideProjects())));
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    private void performUpdateCheck() {
        this.updateChecker = new UpdateChecker(this, SPIGOT_ID);
        if (!getConfigService().current().settings().updateChecker()) {
            return;
        }

        updateChecker.requestUpdateCheck().whenComplete((result, e) -> {
            if (result.requiresUpdate()) {
                Bukkit.getConsoleSender()
                        .sendMessage(ChatColor.YELLOW + "[BuildSystem] Great! a new update is available: "
                                + ChatColor.GREEN + "v" + result.getNewestVersion());
                Bukkit.getConsoleSender()
                        .sendMessage(ChatColor.YELLOW + " ➥ Your current version: " + ChatColor.RED
                                + this.getDescription().getVersion());
                return;
            }

            UpdateChecker.UpdateReason reason = result.getReason();
            switch (reason) {
                case COULD_NOT_CONNECT, INVALID_JSON, UNAUTHORIZED_QUERY, UNKNOWN_ERROR, UNSUPPORTED_VERSION_SCHEME ->
                    Bukkit.getConsoleSender()
                            .sendMessage(ChatColor.RED
                                    + "[BuildSystem] Could not check for a new version of BuildSystem. Reason: "
                                    + reason);
            }
        });
    }

    private void createTemplateFolder() {
        File templateFolder = new File(getDataFolder() + File.separator + "templates");
        if (templateFolder.mkdirs()) {
            getLogger().info("Created \"templates\" folder");
        }
    }

    private CompletableFuture<Void> saveBuildConfig() {
        CompletableFuture<Void> worldSave = getWorldService().save();
        CompletableFuture<Void> playerSave = getPlayerService().save();
        CompletableFuture<Void> spawnSave = getSpawnService().save();
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
        getConfigService().load();
        if (isEnabled()) {
            getBackupService().reload();
        }

        if (init) {
            getWorldService().remanageAllUnloadTasks();

            if (getConfigService().current().settings().scoreboard()) {
                getSettingsService().displayScoreboard();
            } else {
                getSettingsService().hideScoreboards();
            }
        }
    }

    public NavigatorService getNavigatorService() {
        return services.navigator();
    }

    public CustomBlockManager getCustomBlockManager() {
        return services.customBlockManager();
    }

    public PlayerServiceImpl getPlayerService() {
        return services.player();
    }

    public PlayerLookupService getPlayerLookupService() {
        return services.playerLookup();
    }

    public NoClipService getNoClipService() {
        return services.noClip();
    }

    public SettingsService getSettingsService() {
        return services.settings();
    }

    public SpawnService getSpawnService() {
        return services.spawn();
    }

    public WorldServiceImpl getWorldService() {
        return services.world();
    }

    public BackupService getBackupService() {
        return services.backup();
    }

    public ConfigService getConfigService() {
        return services.config();
    }

    public Messages getMessages() {
        return services.messages();
    }

    public CustomizableIcons getCustomizableIcons() {
        return services.customizableIcons();
    }

    public MenuItems getMenuItems() {
        return services.menuItems();
    }
}
