/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.settings;

import de.eintosti.buildsystem.navigator.settings.NavigatorType;
import de.eintosti.buildsystem.navigator.settings.WorldDisplay;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Settings implements ConfigurationSerializable {

    private DesignColor designColor;
    private NavigatorType navigatorType;
    private WorldDisplay worldDisplay;
    private boolean clearInventory;
    private boolean disableInteract;
    private boolean hidePlayers;
    private boolean instantPlaceSigns;
    private boolean keepNavigator;
    private boolean nightVision;
    private boolean noClip;
    private boolean placePlants;
    private boolean scoreboard;
    private boolean slabBreaking;
    private boolean spawnTeleport;
    private boolean trapDoor;

    private BukkitTask scoreboardTask;

    public Settings() {
        this.navigatorType = NavigatorType.OLD;
        this.designColor = DesignColor.BLACK;
        this.worldDisplay = new WorldDisplay();
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
        this.trapDoor = false;
    }

    public Settings(
            NavigatorType navigatorType,
            DesignColor designColor,
            WorldDisplay worldDisplay,
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
            boolean trapDoor
    ) {
        this.navigatorType = navigatorType == null ? NavigatorType.OLD : navigatorType;
        this.designColor = designColor == null ? DesignColor.BLACK : designColor;
        this.worldDisplay = worldDisplay;
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
        this.trapDoor = trapDoor;
    }

    public NavigatorType getNavigatorType() {
        return navigatorType;
    }

    public void setNavigatorType(NavigatorType navigatorType) {
        this.navigatorType = navigatorType;
    }

    public DesignColor getDesignColor() {
        return designColor;
    }

    public void setDesignColor(DesignColor designColor) {
        this.designColor = designColor;
    }

    public WorldDisplay getWorldDisplay() {
        return worldDisplay;
    }

    public void setWorldDisplay(WorldDisplay worldDisplay) {
        this.worldDisplay = worldDisplay;
    }

    public boolean isClearInventory() {
        return clearInventory;
    }

    public void setClearInventory(boolean clearInventory) {
        this.clearInventory = clearInventory;
    }

    public boolean isDisableInteract() {
        return disableInteract;
    }

    public void setDisableInteract(boolean disableInteract) {
        this.disableInteract = disableInteract;
    }

    public boolean isHidePlayers() {
        return hidePlayers;
    }

    public void setHidePlayers(boolean hidePlayers) {
        this.hidePlayers = hidePlayers;
    }

    public boolean isInstantPlaceSigns() {
        return instantPlaceSigns;
    }

    public void setInstantPlaceSigns(boolean instantPlaceSigns) {
        this.instantPlaceSigns = instantPlaceSigns;
    }

    public boolean isKeepNavigator() {
        return keepNavigator;
    }

    public void setKeepNavigator(boolean keepNavigator) {
        this.keepNavigator = keepNavigator;
    }

    public boolean isNightVision() {
        return nightVision;
    }

    public void setNightVision(boolean nightVision) {
        this.nightVision = nightVision;
    }

    public boolean isNoClip() {
        return noClip;
    }

    public void setNoClip(boolean noClip) {
        this.noClip = noClip;
    }

    public boolean isPlacePlants() {
        return placePlants;
    }

    public void setPlacePlants(boolean placePlants) {
        this.placePlants = placePlants;
    }

    public boolean isScoreboard() {
        return scoreboard;
    }

    public void setScoreboard(boolean scoreboard) {
        this.scoreboard = scoreboard;
    }

    public boolean isSlabBreaking() {
        return slabBreaking;
    }

    public void setSlabBreaking(boolean slabBreaking) {
        this.slabBreaking = slabBreaking;
    }

    public boolean isSpawnTeleport() {
        return spawnTeleport;
    }

    public void setSpawnTeleport(boolean spawnTeleport) {
        this.spawnTeleport = spawnTeleport;
    }

    public boolean isTrapDoor() {
        return trapDoor;
    }

    public void setTrapDoor(boolean trapDoor) {
        this.trapDoor = trapDoor;
    }

    public BukkitTask getScoreboardTask() {
        return scoreboardTask;
    }

    public void setScoreboardTask(BukkitTask scoreboardTask) {
        this.scoreboardTask = scoreboardTask;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> settings = new HashMap<>();

        settings.put("type", getNavigatorType().toString());
        settings.put("glass", getDesignColor().toString());
        settings.put("world-display", getWorldDisplay().serialize());
        settings.put("slab-breaking", isSlabBreaking());
        settings.put("no-clip", isNoClip());
        settings.put("trapdoor", isTrapDoor());
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