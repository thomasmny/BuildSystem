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
import de.eintosti.buildsystem.menu.MenuItems;
import de.eintosti.buildsystem.menu.Menus;
import de.eintosti.buildsystem.menu.Prompts;
import de.eintosti.buildsystem.navigator.NavigatorEditorService;
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
import de.eintosti.buildsystem.world.backup.BackupServiceImpl;
import de.eintosti.buildsystem.world.data.WorldStatusRegistryImpl;
import de.eintosti.buildsystem.world.display.CustomizableIcons;
import de.eintosti.buildsystem.world.display.NavigatorCategoryRegistryImpl;
import de.eintosti.buildsystem.world.spawn.SpawnService;
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

        new CommandRegistrar(this).registerAll();
        new ListenerRegistrar(this).registerAll();
        (this.integrations = new Integrations(
                        this, getMessages(), getSettingsService(), getPlayerService(), getWorldService()))
                .activate();

        this.api = new BuildSystemApi(this);
        getServer().getServicesManager().register(BuildSystem.class, api, this, ServicePriority.Normal);

        Bukkit.getOnlinePlayers().forEach(pl -> {
            BuildPlayer buildPlayer = getPlayerService().getPlayerStorage().createBuildPlayer(pl);
            Settings settings = buildPlayer.getSettings();
            getNoClipService().startNoClip(pl, settings);
            getSettingsService().displayScoreboard(pl);
        });

        new BuildSystemMetrics(this).register();

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
                    BuildPlayerImpl.of(getPlayerService().getPlayerStorage().getBuildPlayer(pl));
            buildPlayer.getCachedValues().resetCachedValues(pl);
            buildPlayer.setLogoutLocation(new LogoutLocation(pl.getWorld().getName(), pl.getLocation()));

            getSettingsService().hideScoreboard(pl);
            getNoClipService().stopNoClip(pl.getUniqueId());
            getNavigatorService().closeNewNavigator(pl);
        });
        getNavigatorEditorService().restoreAll();

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

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    private void performUpdateCheck() {
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

    public NavigatorEditorService getNavigatorEditorService() {
        return services.navigatorEditor();
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

    public BackupServiceImpl getBackupService() {
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

    public WorldStatusRegistryImpl getWorldStatusRegistry() {
        return services.worldStatusRegistry();
    }

    public NavigatorCategoryRegistryImpl getNavigatorCategoryRegistry() {
        return services.navigatorCategoryRegistry();
    }

    public MenuItems getMenuItems() {
        return services.menuItems();
    }

    public Menus getMenus() {
        return services.menus();
    }

    public Prompts getPrompts() {
        return services.prompts();
    }
}
