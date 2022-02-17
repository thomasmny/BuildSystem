/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.settings;

/**
 * @author einTosti
 */
public interface Settings {

    NavigatorType getNavigatorType();

    void setNavigatorType(NavigatorType navigatorType);

    GlassColor getGlassColor();

    void setGlassColor(GlassColor glassColor);

    WorldSort getWorldSort();

    void setWorldSort(WorldSort worldSort);

    boolean isClearInventory();

    void setClearInventory(boolean value);

    boolean isDisableInteract();

    void setDisableInteract(boolean value);

    boolean isHidePlayers();

    void setHidePlayers(boolean value);

    boolean isInstantPlaceSigns();

    void setInstantPlaceSigns(boolean value);

    boolean isKeepNavigator();

    void setKeepNavigator(boolean value);

    boolean isNightVision();

    void setNightVision(boolean value);

    boolean isNoClip();

    void setNoClip(boolean value);

    boolean isPlacePlants();

    void setPlacePlants(boolean value);

    boolean isScoreboard();

    void setScoreboard(boolean value);

    boolean isSlabBreaking();

    void setSlabBreaking(boolean value);

    boolean isSpawnTeleport();

    void setSpawnTeleport(boolean value);

    boolean isOpenTrapDoor();

    void setOpenTrapDoor(boolean value);
}
