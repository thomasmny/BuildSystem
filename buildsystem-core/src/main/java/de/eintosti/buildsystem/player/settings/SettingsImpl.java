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
package de.eintosti.buildsystem.player.settings;

import de.eintosti.buildsystem.api.navigator.settings.NavigatorType;
import de.eintosti.buildsystem.api.navigator.settings.WorldDisplay;
import de.eintosti.buildsystem.api.player.settings.DesignColor;
import de.eintosti.buildsystem.api.player.settings.Settings;
import de.eintosti.buildsystem.navigator.settings.WorldDisplayImpl;
import org.bukkit.scheduler.BukkitTask;

public class SettingsImpl implements Settings {

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
    private boolean openTrapDoors;

    private BukkitTask scoreboardTask;

    public SettingsImpl() {
        this.navigatorType = NavigatorType.OLD;
        this.designColor = DesignColor.BLACK;
        this.worldDisplay = new WorldDisplayImpl();
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
        this.openTrapDoors = false;
    }

    public SettingsImpl(
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
            boolean openTrapDoors
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
        this.openTrapDoors = openTrapDoors;
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
    public DesignColor getDesignColor() {
        return designColor;
    }

    @Override
    public void setDesignColor(DesignColor designColor) {
        this.designColor = designColor;
    }

    @Override
    public WorldDisplay getWorldDisplay() {
        return worldDisplay;
    }

    @Override
    public void setWorldDisplay(WorldDisplay worldDisplay) {
        this.worldDisplay = worldDisplay;
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
    public boolean isOpenTrapDoors() {
        return openTrapDoors;
    }

    @Override
    public void setOpenTrapDoors(boolean trapDoor) {
        this.openTrapDoors = trapDoor;
    }

    @Override
    public BukkitTask getScoreboardTask() {
        return scoreboardTask;
    }

    @Override
    public void setScoreboardTask(BukkitTask scoreboardTask) {
        this.scoreboardTask = scoreboardTask;
    }
}