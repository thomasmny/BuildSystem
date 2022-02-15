/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.util;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import org.bukkit.Difficulty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author einTosti
 */
public class ConfigValues {

    private final BuildSystem plugin;

    private String dateFormat;
    private String timeUntilUnload;

    private XMaterial navigatorItem;
    private XMaterial worldEditWand;
    private Difficulty worldDifficulty;

    private boolean archiveVanish;
    private boolean scoreboard;
    private boolean spawnTeleportMessage;
    private boolean joinQuitMessages;
    private boolean lockWeather;
    private boolean unloadWorlds;
    private boolean voidBlock;
    private boolean updateChecker;
    private boolean blockWorldEditNonBuilder;
    private boolean creatorIsBuilder;
    private boolean giveNavigatorOnJoin;
    private boolean worldPhysics;
    private boolean worldExplosions;
    private boolean worldMobAi;
    private boolean worldBlockBreaking;
    private boolean worldBlockPlacement;
    private boolean worldBlockInteractions;

    private int sunriseTime;
    private int noonTime;
    private int nightTime;
    private int worldBorderSize;
    private int importDelay;
    private int maxPublicWorldAmount;
    private int maxPrivateWorldAmount;

    private Map<String, String> defaultGameRules;
    private Set<String> blackListedWorldsToUnload;

    public ConfigValues(BuildSystem plugin) {
        this.plugin = plugin;
        setConfigValues();
    }

    public void setConfigValues() {
        final FileConfiguration config = plugin.getConfig();

        // Messages
        this.spawnTeleportMessage = config.getBoolean("messages.spawn-teleport-message", false);
        this.joinQuitMessages = config.getBoolean("messages.join-quit-messages", true);
        this.dateFormat = config.getString("messages.date-format", "dd/MM/yyyy");

        // Settings
        this.updateChecker = config.getBoolean("settings.update-checker", true);
        this.scoreboard = config.getBoolean("settings.scoreboard", true);
        this.archiveVanish = config.getBoolean("settings.archive-vanish", true);

        this.blockWorldEditNonBuilder = config.getBoolean("settings.builder.block-worldedit-non-builder", true);
        this.worldEditWand = XMaterial.valueOf(config.getString("settings.builder.world-edit-wand", "WOODEN_AXE"));
        this.creatorIsBuilder = config.getBoolean("settings.builder.creator-is-builder", true);

        this.navigatorItem = XMaterial.valueOf(config.getString("settings.navigator.item", "CLOCK"));
        this.giveNavigatorOnJoin = config.getBoolean("settings.navigator.give-item-on-join", true);

        // World
        this.lockWeather = config.getBoolean("world.lock-weather", true);
        this.worldDifficulty = Difficulty.valueOf(config.getString("world.default.difficulty".toUpperCase(), "PEACEFUL"));
        this.sunriseTime = config.getInt("world.default.time.sunrise", 0);
        this.noonTime = config.getInt("world.default.time.noon", 6000);
        this.nightTime = config.getInt("world.default.time.night", 18000);

        this.worldBorderSize = config.getInt("world.default.worldborder.size", 6000000);

        Map<String, String> defaultGameRules = new HashMap<>();
        ConfigurationSection configurationSection = config.getConfigurationSection("world.default.gamerules");
        if (configurationSection != null) {
            for (Map.Entry<String, Object> entry : configurationSection.getValues(true).entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue().toString();
                defaultGameRules.put(name, value);
            }
        }
        this.defaultGameRules = defaultGameRules;

        this.worldPhysics = config.getBoolean("world.default.settings.physics", true);
        this.worldExplosions = config.getBoolean("world.default.settings.explosions", true);
        this.worldMobAi = config.getBoolean("world.default.settings.mob-ai", true);
        this.worldBlockBreaking = config.getBoolean("world.default.settings.block-breaking", true);
        this.worldBlockPlacement = config.getBoolean("world.default.settings.block-placement", true);
        this.worldBlockInteractions = config.getBoolean("world.default.settings.block-interactions", true);

        this.unloadWorlds = config.getBoolean("world.unload.enabled", false);
        this.timeUntilUnload = config.getString("world.unload.time-until-unload", "01:00:00");
        this.blackListedWorldsToUnload = new HashSet<>(config.getStringList("world.unload.blacklisted-worlds"));

        this.importDelay = config.getInt("world.import-all.delay", 30);

        this.maxPublicWorldAmount = config.getInt("world.max-amount.public", -1);
        this.maxPrivateWorldAmount = config.getInt("world.max-amount.private", -1);

        this.voidBlock = config.getBoolean("world.void-block", true);
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public Difficulty getWorldDifficulty() {
        return worldDifficulty;
    }

    public long getTimeUntilUnload() {
        String[] timeArray = timeUntilUnload.split(":");
        int hours = Integer.parseInt(timeArray[0]);
        int minutes = Integer.parseInt(timeArray[1]);
        int seconds = Integer.parseInt(timeArray[2]);
        return hours * 3600L + minutes * 60L + seconds;
    }

    public boolean isArchiveVanish() {
        return archiveVanish;
    }

    public boolean isScoreboard() {
        return scoreboard;
    }

    public boolean isSpawnTeleportMessage() {
        return spawnTeleportMessage;
    }

    public boolean isJoinQuitMessages() {
        return joinQuitMessages;
    }

    public boolean isLockWeather() {
        return lockWeather;
    }

    public boolean isUnloadWorlds() {
        return unloadWorlds;
    }

    public boolean isVoidBlock() {
        return voidBlock;
    }

    public boolean isUpdateChecker() {
        return updateChecker;
    }

    public boolean isBlockWorldEditNonBuilder() {
        return blockWorldEditNonBuilder;
    }

    public boolean isCreatorIsBuilder() {
        return creatorIsBuilder;
    }

    public boolean isGiveNavigatorOnJoin() {
        return giveNavigatorOnJoin;
    }

    public boolean isWorldPhysics() {
        return worldPhysics;
    }

    public boolean isWorldExplosions() {
        return worldExplosions;
    }

    public boolean isWorldMobAi() {
        return worldMobAi;
    }

    public boolean isWorldBlockBreaking() {
        return worldBlockBreaking;
    }

    public boolean isWorldBlockPlacement() {
        return worldBlockPlacement;
    }

    public boolean isWorldBlockInteractions() {
        return worldBlockInteractions;
    }

    public XMaterial getNavigatorItem() {
        return navigatorItem;
    }

    public XMaterial getWorldEditWand() {
        return worldEditWand;
    }

    public int getSunriseTime() {
        return sunriseTime;
    }

    public int getNoonTime() {
        return noonTime;
    }

    public int getNightTime() {
        return nightTime;
    }

    public int getWorldBorderSize() {
        return worldBorderSize;
    }

    public int getImportDelay() {
        return importDelay;
    }

    public int getMaxWorldAmount(boolean privateWorld) {
        return privateWorld ? maxPrivateWorldAmount : maxPublicWorldAmount;
    }

    public Map<String, String> getDefaultGameRules() {
        return defaultGameRules;
    }

    public Set<String> getBlackListedWorldsToUnload() {
        return blackListedWorldsToUnload;
    }
}
