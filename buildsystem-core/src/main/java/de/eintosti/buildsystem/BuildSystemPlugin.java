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
import de.eintosti.buildsystem.i18n.Messages;
import de.eintosti.buildsystem.integration.Integrations;
import de.eintosti.buildsystem.listener.ListenerRegistrar;
import de.eintosti.buildsystem.listener.player.ArchiveMode;
import de.eintosti.buildsystem.player.BuildPlayerImpl;
import de.eintosti.buildsystem.player.CachedValues;
import de.eintosti.buildsystem.player.LogoutLocation;
import de.eintosti.buildsystem.util.Permissions;
import de.eintosti.buildsystem.util.TaskScheduler;
import de.eintosti.buildsystem.util.UpdateChecker;
import de.eintosti.buildsystem.world.WorldNames;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

public class BuildSystemPlugin extends JavaPlugin {

    public static final int METRICS_ID = 7427;
    public static final String ADMIN_PERMISSION = Permissions.ADMIN;

    private static final long CONFIG_SAVE_INTERVAL_TICKS = 5L * 60L * 20L;

    private ConfigService configService;
    private Messages messages;
    private @Nullable TaskScheduler scheduler;
    private @Nullable Services services;
    private UpdateChecker updateChecker;
    private @Nullable Integrations integrations;
    private @Nullable BuildSystemApi api;
    private BukkitTask configSaveTask;

    @Override
    public void onLoad() {
        this.configService = new ConfigService(this);
        new ConfigMigrationManager(this).migrate();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        configService.load();

        (this.messages = new Messages(this, configService)).load();
        createTemplateFolder();
    }

    @Override
    public void onEnable() {
        this.scheduler = new TaskScheduler(this);
        this.services = new Services(this, scheduler, configService, messages);
        this.services.loadWorlds();
        this.updateChecker = new UpdateChecker(this, services.scheduler().background());
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
        Bukkit.getOnlinePlayers().forEach(services.settings()::updateVisibility);
        // After a reload, players may be standing in an archive world, which only exists once the stored worlds load.
        services.world()
                .worldsLoaded()
                .thenRun(() -> Bukkit.getOnlinePlayers().forEach(pl -> {
                    BuildPlayerImpl buildPlayer = BuildPlayerImpl.of(
                            services.player().getPlayerStorage().getBuildPlayer(pl));
                    if (ArchiveMode.enterIfInArchiveWorld(
                            pl,
                            buildPlayer.getCachedValues(),
                            services.world().getWorldStorage(),
                            configService.current().settings().archive())) {
                        services.player().getPlayerStorage().save(buildPlayer);
                    }
                    services.settings().updateVisibility(pl);
                }));

        services.worldDownload().start();

        new BuildSystemMetrics(this, services.config(), services.player()).register();

        // Synchronous on purpose: each storage serializes live domain state (builders, warps, world data) into a map
        // on the calling thread and only writes that map to disk asynchronously. Running the timer off-main read those
        // collections while players were mutating them, which threw ConcurrentModificationException and lost the whole
        // save cycle. Building the maps is cheap; the disk I/O is still async.
        this.configSaveTask = services.scheduler()
                .runTimer(this::saveBuildConfig, CONFIG_SAVE_INTERVAL_TICKS, CONFIG_SAVE_INTERVAL_TICKS);

        Bukkit.getConsoleSender()
                .sendMessage("%sBuildSystem » Plugin %senabled%s!"
                        .formatted(ChatColor.RESET, ChatColor.GREEN, ChatColor.RESET));
    }

    @Override
    public void onDisable() {
        if (services == null) {
            // onEnable failed before the services existed, so nothing was loaded that could be saved.
            if (scheduler != null) {
                scheduler.shutdown();
            }
            return;
        }

        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayerImpl buildPlayer =
                    BuildPlayerImpl.of(services.player().getPlayerStorage().getBuildPlayer(pl));
            buildPlayer.getCachedValues().resetCachedValues(pl);
            // Without this the archive invisibility outlives the plugin; enable enters archive mode again.
            ArchiveMode.exit(pl, buildPlayer.getCachedValues());
            buildPlayer.setLogoutLocation(new LogoutLocation(WorldNames.of(pl.getWorld()), pl.getLocation()));

            services.settings().hideScoreboard(pl);
            services.noClip().stopNoClip(pl.getUniqueId());
            services.navigator().closeNewNavigator(pl);
        });
        services.settings().showAllPlayers();
        services.navigatorEditor().restoreAll();

        services.worldDownload().stop();
        services.backup().close();
        services.world().cancelAllUnloadTasks();

        reloadConfigData(false);
        saveConfig();

        // Cancelling only stops future ticks, not one already running,
        // so this must happen before the join() below
        if (this.configSaveTask != null) {
            this.configSaveTask.cancel();
        }

        try {
            saveBuildConfig().join();
        } catch (CompletionException e) {
            getLogger().severe("Error while waiting for saves: %s".formatted(e.getCause()));
        }

        // Shut the shared background pool down only after the final saves above have completed.
        services.scheduler().shutdown();

        // onEnable may have failed after the services were built, before these existed.
        if (this.integrations != null) {
            this.integrations.deactivate();
        }
        if (api != null) {
            getServer().getServicesManager().unregister(BuildSystem.class, api);
        }

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

        updateChecker
                .requestUpdateCheck()
                .whenCompleteAsync(
                        (result, e) -> {
                            if (result == null) {
                                return;
                            }

                            UpdateChecker.Release update = result.newerRelease();
                            if (update != null) {
                                Bukkit.getConsoleSender()
                                        .sendMessage("%s[BuildSystem] Great! a new update is available: %sv%s"
                                                .formatted(ChatColor.YELLOW, ChatColor.GREEN, update.version()));
                                Bukkit.getConsoleSender()
                                        .sendMessage("%s ➥ Your current version: %s%s"
                                                .formatted(
                                                        ChatColor.YELLOW,
                                                        ChatColor.RED,
                                                        this.getDescription().getVersion()));
                                Bukkit.getConsoleSender()
                                        .sendMessage("%s ➥ Download: %s%s"
                                                .formatted(ChatColor.YELLOW, ChatColor.AQUA, update.url()));
                                return;
                            }

                            UpdateChecker.UpdateReason reason = result.reason();
                            switch (reason) {
                                case COULD_NOT_CONNECT,
                                        INVALID_JSON,
                                        UNAUTHORIZED_QUERY,
                                        RATE_LIMITED,
                                        UNKNOWN_ERROR,
                                        UNSUPPORTED_VERSION_SCHEME ->
                                    Bukkit.getConsoleSender()
                                            .sendMessage(
                                                    "%s[BuildSystem] Could not check for a new version of BuildSystem. Reason: %s"
                                                            .formatted(ChatColor.RED, reason));
                            }
                        },
                        services.scheduler().mainThread());
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

    private CachedValues cachedValues(Player player) {
        return BuildPlayerImpl.of(services.player().getPlayerStorage().getBuildPlayer(player))
                .getCachedValues();
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
            services.worldDownload().reload();
        }

        if (init) {
            services.world().remanageAllUnloadTasks();

            boolean vanish = services.config().current().settings().archive().vanish();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                ArchiveMode.applyVanish(pl, cachedValues(pl), vanish);
            }
            Bukkit.getOnlinePlayers().forEach(services.settings()::updateVisibility);

            if (services.config().current().settings().scoreboard()) {
                services.settings().displayScoreboard();
            } else {
                services.settings().hideScoreboards();
            }
        }
    }
}
