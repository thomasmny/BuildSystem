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
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.command.CommandRegistrar;
import de.eintosti.buildsystem.config.ConfigService;
import de.eintosti.buildsystem.config.migration.ConfigMigrationManager;
import de.eintosti.buildsystem.integration.Integrations;
import de.eintosti.buildsystem.listener.ListenerRegistrar;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.util.UpdateChecker;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

    private static final long CONFIG_SAVE_INTERVAL_TICKS = 5L * 60L * 20L;

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
        this.updateChecker = new UpdateChecker(this, SPIGOT_ID);
        performUpdateCheck();

        new CommandRegistrar(this, services).registerAll();
        new ListenerRegistrar(this, services).registerAll();
        (this.integrations = new Integrations(
                        this, services.messages(), services.settings(), services.player(), services.world()))
                .activate();

        this.api = new BuildSystemApi(services);
        getServer().getServicesManager().register(BuildSystem.class, api, this, ServicePriority.Normal);

        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayer buildPlayer = services.player().getPlayerStorage().createBuildPlayer(pl);
            Settings settings = buildPlayer.getSettings();
            services.noClip().startNoClip(pl, settings);
            services.settings().displayScoreboard(pl);
        });

        new BuildSystemMetrics(this, services.config(), services.player()).register();

        this.configSaveTask = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(
                        this, this::saveBuildConfig, CONFIG_SAVE_INTERVAL_TICKS, CONFIG_SAVE_INTERVAL_TICKS);

        Bukkit.getConsoleSender()
                .sendMessage("%sBuildSystem » Plugin %senabled%s!"
                        .formatted(ChatColor.RESET, ChatColor.GREEN, ChatColor.RESET));
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayerImpl buildPlayer =
                    BuildPlayerImpl.of(services.player().getPlayerStorage().getBuildPlayer(pl));
            buildPlayer.getCachedValues().resetCachedValues(pl);
            buildPlayer.setLogoutLocation(new LogoutLocation(pl.getWorld().getName(), pl.getLocation()));

            services.settings().hideScoreboard(pl);
            services.noClip().stopNoClip(pl.getUniqueId());
            services.navigator().closeNewNavigator(pl);
        });
        services.navigatorEditor().restoreAll();

        services.backup().close();
        services.world().cancelAllUnloadTasks();

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

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    private void performUpdateCheck() {
        if (!services.config().current().settings().updateChecker()) {
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
        CompletableFuture<Void> worldSave = services.world().save();
        CompletableFuture<Void> playerSave = services.player().save();
        CompletableFuture<Void> spawnSave = services.spawn().save();
        return CompletableFuture.allOf(worldSave, playerSave, spawnSave);
    }

    /**
     * Reloads the config and config data.
     *
     * @param init Whether the plugin should reinitialize classes
     */
    public void reloadConfigData(boolean init) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            services.settings().hideScoreboard(pl);
        }

        reloadConfig();
        services.config().load();
        if (isEnabled()) {
            services.backup().reload();
        }

        if (init) {
            services.world().remanageAllUnloadTasks();

            if (services.config().current().settings().scoreboard()) {
                services.settings().displayScoreboard();
            } else {
                services.settings().hideScoreboards();
            }
        }
    }
}
