/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.settings;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.config.SettingsConfig;
import com.eintosti.buildsystem.navigator.NavigatorType;
import com.eintosti.buildsystem.navigator.WorldSort;
import com.eintosti.buildsystem.player.PlayerManager;
import com.eintosti.buildsystem.version.util.MinecraftVersion;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author einTosti
 */
public class SettingsManager {

    private final BuildSystem plugin;
    private final ConfigValues configValues;
    private final SettingsConfig settingsConfig;
    private final WorldManager worldManager;

    private final Map<UUID, FastBoard> boards;

    private final String scoreboardTitle;
    private final List<String> scoreboardBody;

    public SettingsManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();
        this.settingsConfig = new SettingsConfig(plugin);
        this.worldManager = plugin.getWorldManager();

        this.boards = new HashMap<>();

        String title = Messages.getString("title");
        List<String> body = Messages.getStringList("body");
        if (MinecraftVersion.getCurrent().isLowerThan(MinecraftVersion.AQUATIC_13)) {
            title = title.substring(0, 30);
            body = body.stream().map(line -> line.substring(0, 30)).collect(Collectors.toList());
        }
        this.scoreboardTitle = title;
        this.scoreboardBody = body;
    }

    public Settings getSettings(UUID uuid) {
        return plugin.getPlayerManager().getBuildPlayer(uuid).getSettings();
    }

    public Settings getSettings(Player player) {
        return getSettings(player.getUniqueId());
    }

    /**
     * Only set a player's scoreboard if {@link Settings#isScoreboard} is equal to {@code true}.
     *
     * @param player   The player object
     * @param settings The player's settings
     */
    public void startScoreboard(Player player, Settings settings) {
        if (!settings.isScoreboard()) {
            stopScoreboard(player, settings);
            return;
        }

        startScoreboard(player);
    }

    /**
     * Only set a player's scoreboard if {@link Settings#isScoreboard} is equal to {@code true}.
     *
     * @param player The player object
     */
    public void startScoreboard(Player player) {
        if (!configValues.isScoreboard()) {
            return;
        }

        Settings settings = getSettings(player);
        FastBoard board = new FastBoard(player);
        this.boards.put(player.getUniqueId(), board);

        if (!settings.isScoreboard()) {
            stopScoreboard(player, settings);
            return;
        }

        board.updateTitle(this.scoreboardTitle);
        BukkitTask scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> updateScoreboard(player, board), 0L, 20L);
        settings.setScoreboardTask(scoreboardTask);
    }

    /**
     * Set each player's scoreboard if they have {@link Settings#isScoreboard} enabled.
     */
    public void startScoreboard() {
        if (!configValues.isScoreboard()) {
            return;
        }

        Bukkit.getOnlinePlayers().forEach(this::startScoreboard);
    }

    public void updateScoreboard(Player player) {
        FastBoard board = this.boards.get(player.getUniqueId());
        if (board != null) {
            updateScoreboard(player, board);
        }
    }

    private void updateScoreboard(Player player, FastBoard board) {
        ArrayList<String> body = new ArrayList<>();

        for (String line : this.scoreboardBody) {
            body.add(injectPlaceholders(line, player));
        }

        board.updateLines(body);
    }

    private String injectPlaceholders(String originalString, Player player) {
        if (!originalString.matches(".*%*%.*")) {
            return originalString;
        }

        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);

        return originalString
                .replace("%world%", worldName)
                .replace("%status%", parseWorldInformation(buildWorld, "%status%"))
                .replace("%permission%", parseWorldInformation(buildWorld, "%permission%"))
                .replace("%project%", parseWorldInformation(buildWorld, "%project%"))
                .replace("%creator%", parseWorldInformation(buildWorld, "%creator%"))
                .replace("%creation%", parseWorldInformation(buildWorld, "%creation%"));
    }

    // Is there an easier way of doing this?
    private String parseWorldInformation(BuildWorld buildWorld, String input) {
        if (buildWorld == null) {
            return "§f-";
        }

        switch (input) {
            case "%status%":
                return buildWorld.getStatus().getName();
            case "%permission%":
                return buildWorld.getPermission();
            case "%project%":
                return buildWorld.getProject();
            case "%creator%":
                return buildWorld.getCreator();
            case "%creation%":
                return buildWorld.getFormattedCreationDate();
            default:
                return "§f-";
        }
    }

    private void stopScoreboard(Player player, Settings settings) {
        BukkitTask scoreboardTask = settings.getScoreboardTask();
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            settings.setScoreboardTask(null);
        }

        FastBoard board = this.boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public void stopScoreboard(Player player) {
        stopScoreboard(player, getSettings(player));
    }

    public void stopScoreboard() {
        Bukkit.getOnlinePlayers().forEach(this::stopScoreboard);
    }

    /**
     * Loads all per-player settings.
     *
     * @deprecated Player settings are now loading in {@link PlayerManager#load()}
     */
    @Deprecated
    public void load() {
        FileConfiguration configuration = settingsConfig.getFile();
        ConfigurationSection configurationSection = configuration.getConfigurationSection("settings");
        if (configurationSection == null) {
            return;
        }

        Set<String> uuids = configurationSection.getKeys(false);
        uuids.forEach(uuid -> {
            NavigatorType navigatorType = NavigatorType.valueOf(configuration.getString("settings." + uuid + ".type"));
            DesignColor glassColor = configuration.getString("settings." + uuid + ".glass") != null ? DesignColor.matchColor(configuration.getString("settings." + uuid + ".glass")) : DesignColor.BLACK;
            WorldSort worldSort = WorldSort.matchWorldSort(configuration.getString("settings." + uuid + ".world-sort"));
            boolean clearInventory = configuration.isBoolean("settings." + uuid + ".clear-inventory") && configuration.getBoolean("settings." + uuid + ".clear-inventory");
            boolean disableInteract = configuration.isBoolean("settings." + uuid + ".disable-interact") && configuration.getBoolean("settings." + uuid + ".disable-interact");
            boolean hidePlayers = configuration.isBoolean("settings." + uuid + ".hide-players") && configuration.getBoolean("settings." + uuid + ".hide-players");
            boolean instantPlaceSigns = configuration.isBoolean("settings." + uuid + ".instant-place-signs") && configuration.getBoolean("settings." + uuid + ".instant-place-signs");
            boolean keepNavigator = configuration.isBoolean("settings." + uuid + ".keep-navigator") && configuration.getBoolean("settings." + uuid + ".keep-navigator");
            boolean nightVision = configuration.getBoolean("settings." + uuid + ".nightvision");
            boolean noClip = configuration.isBoolean("settings." + uuid + ".no-clip") && configuration.getBoolean("settings." + uuid + ".no-clip");
            boolean placePlants = configuration.isBoolean("settings." + uuid + ".place-plants") && configuration.getBoolean("settings." + uuid + ".place-plants");
            boolean scoreboard = !configuration.isBoolean("settings." + uuid + ".scoreboard") || configuration.getBoolean("settings." + uuid + ".scoreboard");
            boolean slabBreaking = configuration.isBoolean("settings." + uuid + ".slab-breaking") && configuration.getBoolean("settings." + uuid + ".slab-breaking");
            boolean spawnTeleport = !configuration.isBoolean("settings." + uuid + ".spawn-teleport") || configuration.getBoolean("settings." + uuid + ".spawn-teleport");
            boolean trapDoor = configuration.getBoolean("settings." + uuid + ".trapdoor");

            plugin.getPlayerManager().createBuildPlayer(UUID.fromString(uuid), new Settings(
                    navigatorType,
                    glassColor,
                    worldSort,
                    clearInventory,
                    disableInteract,
                    hidePlayers,
                    instantPlaceSigns,
                    keepNavigator,
                    nightVision,
                    noClip,
                    placePlants,
                    scoreboard,
                    slabBreaking,
                    spawnTeleport,
                    trapDoor
            ));
        });

        File settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        if (settingsFile.exists()) {
            settingsFile.delete();
        }
    }
}