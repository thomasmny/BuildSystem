/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.settings;

public interface Settings {

    NavigatorType getNavigatorType();

    void setNavigatorType(NavigatorType navigatorType);

    DesignColor getDesignColor();

    void setDesignColor(DesignColor designColor);

    WorldDisplay getWorldDisplay();

    void setWorldDisplay(WorldDisplay worldDisplay);

    boolean isClearInventory();

    void setClearInventory(boolean clearInventory);

    boolean isDisableInteract();

    void setDisableInteract(boolean disableInteract);

    boolean isHidePlayers();

    void setHidePlayers(boolean hidePlayers);

    boolean isInstantPlaceSigns();

    void setInstantPlaceSigns(boolean instantPlaceSigns);

    boolean isKeepNavigator();

    void setKeepNavigator(boolean keepNavigator);

    boolean isNightVision();

    void setNightVision(boolean nightVision);

    boolean isNoClip();

    void setNoClip(boolean noClip);

    boolean isPlacePlants();

    void setPlacePlants(boolean placePlants);

    boolean isScoreboard();

    void setScoreboard(boolean scoreboard);

    boolean isSlabBreaking();

    void setSlabBreaking(boolean slabBreaking);

    boolean isSpawnTeleport();

    void setSpawnTeleport(boolean spawnTeleport);

    boolean isTrapDoor();

    void setTrapDoor(boolean trapDoor);
}