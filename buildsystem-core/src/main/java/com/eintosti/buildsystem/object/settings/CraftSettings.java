/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.settings;

import com.eintosti.buildsystem.api.settings.GlassColor;
import com.eintosti.buildsystem.api.settings.NavigatorType;
import com.eintosti.buildsystem.api.settings.Settings;
import com.eintosti.buildsystem.api.settings.WorldSort;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author einTosti
 */
public class CraftSettings implements Settings, ConfigurationSerializable {

    private GlassColor glassColor;
    private NavigatorType navigatorType;
    private WorldSort worldSort;
    private boolean clearInventory;
    private boolean disableInteract;
    private boolean hidePlayers;
    private boolean instantPlaceSigns;
    private boolean keepNavigator;
    private boolean nightVision;
    private boolean noClip;
    private boolean openTrapDoor;
    private boolean placePlants;
    private boolean scoreboard;
    private boolean slabBreaking;
    private boolean spawnTeleport;

    private BukkitTask scoreboardTask;

    public CraftSettings() {
        this.navigatorType = NavigatorType.OLD;
        this.glassColor = GlassColor.BLACK;
        this.worldSort = WorldSort.NAME_A_TO_Z;
        this.clearInventory = false;
        this.disableInteract = false;
        this.hidePlayers = false;
        this.instantPlaceSigns = false;
        this.keepNavigator = false;
        this.nightVision = false;
        this.noClip = false;
        this.placePlants = false;
        this.scoreboard = true;
        this.slabBreaking = false;
        this.spawnTeleport = true;
        this.openTrapDoor = false;
    }

    public CraftSettings(
            NavigatorType navigatorType,
            GlassColor glassColor,
            WorldSort worldSort,
            boolean clearInventory,
            boolean disableInteract,
            boolean hidePlayers,
            boolean instantPlaceSigns,
            boolean keepNavigator,
            boolean nightVision,
            boolean noClip,
            boolean placePlants,
            boolean scoreboard,
            boolean slabBreaking,
            boolean spawnTeleport,
            boolean openTrapDoor
    ) {
        this.navigatorType = navigatorType == null ? NavigatorType.OLD : navigatorType;
        this.glassColor = glassColor == null ? GlassColor.BLACK : glassColor;
        this.worldSort = worldSort;
        this.clearInventory = clearInventory;
        this.disableInteract = disableInteract;
        this.hidePlayers = hidePlayers;
        this.instantPlaceSigns = instantPlaceSigns;
        this.keepNavigator = keepNavigator;
        this.nightVision = nightVision;
        this.noClip = noClip;
        this.placePlants = placePlants;
        this.scoreboard = scoreboard;
        this.slabBreaking = slabBreaking;
        this.spawnTeleport = spawnTeleport;
        this.openTrapDoor = openTrapDoor;
    }

    @Override
    public NavigatorType getNavigatorType() {
        return navigatorType;
    }

    @Override
    public void setNavigatorType(NavigatorType navigatorType) {
        this.navigatorType = navigatorType;
    }

    @Override
    public GlassColor getGlassColor() {
        return glassColor;
    }

    @Override
    public void setGlassColor(GlassColor glassColor) {
        this.glassColor = glassColor;
    }

    @Override
    public WorldSort getWorldSort() {
        return worldSort;
    }

    @Override
    public void setWorldSort(WorldSort worldSort) {
        this.worldSort = worldSort;
    }

    @Override
    public boolean isClearInventory() {
        return clearInventory;
    }

    @Override
    public void setClearInventory(boolean clearInventory) {
        this.clearInventory = clearInventory;
    }

    @Override
    public boolean isDisableInteract() {
        return disableInteract;
    }

    @Override
    public void setDisableInteract(boolean disableInteract) {
        this.disableInteract = disableInteract;
    }

    @Override
    public boolean isHidePlayers() {
        return hidePlayers;
    }

    @Override
    public void setHidePlayers(boolean hidePlayers) {
        this.hidePlayers = hidePlayers;
    }

    @Override
    public boolean isInstantPlaceSigns() {
        return instantPlaceSigns;
    }

    @Override
    public void setInstantPlaceSigns(boolean instantPlaceSigns) {
        this.instantPlaceSigns = instantPlaceSigns;
    }

    @Override
    public boolean isKeepNavigator() {
        return keepNavigator;
    }

    @Override
    public void setKeepNavigator(boolean keepNavigator) {
        this.keepNavigator = keepNavigator;
    }

    @Override
    public boolean isNightVision() {
        return nightVision;
    }

    @Override
    public void setNightVision(boolean nightVision) {
        this.nightVision = nightVision;
    }

    @Override
    public boolean isNoClip() {
        return noClip;
    }

    @Override
    public void setNoClip(boolean noClip) {
        this.noClip = noClip;
    }

    @Override
    public boolean isPlacePlants() {
        return placePlants;
    }

    @Override
    public void setPlacePlants(boolean placePlants) {
        this.placePlants = placePlants;
    }

    @Override
    public boolean isScoreboard() {
        return scoreboard;
    }

    @Override
    public void setScoreboard(boolean scoreboard) {
        this.scoreboard = scoreboard;
    }

    @Override
    public boolean isSlabBreaking() {
        return slabBreaking;
    }

    @Override
    public void setSlabBreaking(boolean slabBreaking) {
        this.slabBreaking = slabBreaking;
    }

    @Override
    public boolean isSpawnTeleport() {
        return spawnTeleport;
    }

    @Override
    public void setSpawnTeleport(boolean spawnTeleport) {
        this.spawnTeleport = spawnTeleport;
    }

    @Override
    public boolean isOpenTrapDoor() {
        return openTrapDoor;
    }

    @Override
    public void setOpenTrapDoor(boolean openTrapDoor) {
        this.openTrapDoor = openTrapDoor;
    }

    public BukkitTask getScoreboardTask() {
        return scoreboardTask;
    }

    public void setScoreboardTask(BukkitTask scoreboardTask) {
        this.scoreboardTask = scoreboardTask;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> settings = new HashMap<>();

        settings.put("type", getNavigatorType().toString());
        settings.put("glass", getGlassColor().toString());
        settings.put("world-sort", getWorldSort().toString());
        settings.put("slab-breaking", isSlabBreaking());
        settings.put("no-clip", isNoClip());
        settings.put("trapdoor", isOpenTrapDoor());
        settings.put("nightvision", isNightVision());
        settings.put("scoreboard", isScoreboard());
        settings.put("keep-navigator", isKeepNavigator());
        settings.put("disable-interact", isDisableInteract());
        settings.put("spawn-teleport", isSpawnTeleport());
        settings.put("clear-inventory", isClearInventory());
        settings.put("instant-place-signs", isInstantPlaceSigns());
        settings.put("hide-players", isHidePlayers());
        settings.put("place-plants", isPlacePlants());

        return settings;
    }
}
