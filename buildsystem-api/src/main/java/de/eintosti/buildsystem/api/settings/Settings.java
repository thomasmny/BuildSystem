/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.api.settings;

import org.bukkit.GameMode;
import org.bukkit.potion.PotionEffectType;

public interface Settings {

    /**
     * Gets the mode the navigator is set to.
     *
     * @return The navigator type
     */
    NavigatorType getNavigatorType();

    /**
     * Sets the navigator type.
     * <p>
     * The {@link NavigatorType#OLD} is the classic chest menu, whereas {@link NavigatorType#NEW} is a new 3D selector.
     *
     * @param navigatorType The navigator type
     */
    void setNavigatorType(NavigatorType navigatorType);

    /**
     * Gets the design color used in menus.
     *
     * @return The design color
     */
    DesignColor getDesignColor();

    /**
     * Sets the design color used in menus.
     *
     * @param designColor The design color
     */
    void setDesignColor(DesignColor designColor);

    /**
     * Gets the set of rules by which worlds are displayed in the navigator.
     *
     * @return The world display rules
     */
    WorldDisplay getWorldDisplay();

    /**
     * Sets the world display rule set
     *
     * @param worldDisplay The world display
     */
    void setWorldDisplay(WorldDisplay worldDisplay);

    /**
     * Gets whether the player's inventory is to be cleared when joining the server.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isClearInventory();

    /**
     * Sets whether the player's inventory is to be cleared when joining the server.
     *
     * @param clearInventory If the inventory is to be cleared
     */
    void setClearInventory(boolean clearInventory);

    /**
     * Gets whether the interaction with blocks is disabled.
     *
     * @return {@code true} if disabled, otherwise {@code false}
     */
    boolean isDisableInteract();

    /**
     * Sets whether the interaction with blocks should be disabled.
     *
     * @param disableInteract If the interaction with blocks is to be disabled
     */
    void setDisableInteract(boolean disableInteract);

    /**
     * Gets whether all online players are to be hidden.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isHidePlayers();

    /**
     * Sets whether all online players are to be hidden.
     *
     * @param hidePlayers If the players are to be hidden
     */
    void setHidePlayers(boolean hidePlayers);

    /**
     * Gets whether signs should be placed without opening the text input.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isInstantPlaceSigns();

    /**
     * Sets whether signs should be placed without opening the text input.
     *
     * @param instantPlaceSigns If signs are to be placed instantly
     */
    void setInstantPlaceSigns(boolean instantPlaceSigns);

    /**
     * Gets whether the navigator is kept in the player's inventory after a clear.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isKeepNavigator();

    /**
     * Sets whether the navigator is kept in the player's inventory after a clear.
     *
     * @param keepNavigator If the navigator is to kept
     */
    void setKeepNavigator(boolean keepNavigator);

    /**
     * Gets whether the player has permanent {@link PotionEffectType#NIGHT_VISION}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isNightVision();

    /**
     * Sets whether the player has permanent {@link PotionEffectType#NIGHT_VISION}.
     *
     * @param nightVision If the night vision is to be enabled
     */
    void setNightVision(boolean nightVision);

    /**
     * Gets whether fling against a wall puts the player in {@link GameMode#SPECTATOR}.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isNoClip();

    /**
     * Sets whether fling against a wall puts the player in {@link GameMode#SPECTATOR}.
     *
     * @param noClip If no-clip is to be enabled
     */
    void setNoClip(boolean noClip);

    /**
     * Gets whether plants can be placed anywhere.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isPlacePlants();

    /**
     * Sets whether plants can be placed anywhere.
     *
     * @param placePlants If plants are to be placed anywhere
     */
    void setPlacePlants(boolean placePlants);

    /**
     * Gets whether the scoreboard is enabled.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isScoreboard();

    /**
     * Sets whether the scoreboard is enabled.
     *
     * @param scoreboard If the scoreboard is to be enabled
     */
    void setScoreboard(boolean scoreboard);

    /**
     * Gets whether only one half of a slab will be broken when breaking double slabs.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isSlabBreaking();

    /**
     * Sets whether only one half of a slab will be broken when breaking double slabs.
     *
     * @param slabBreaking If precise slab breaking is to be enabled
     */
    void setSlabBreaking(boolean slabBreaking);

    /**
     * Gets whether the player will be teleported to the spawn, if set, when joining the server.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isSpawnTeleport();

    /**
     * Sets whether the player will be teleported to the spawn, if set, when joining the server.
     *
     * @param spawnTeleport If the player is to be teleported to the spawn
     */
    void setSpawnTeleport(boolean spawnTeleport);

    /**
     * Gets whether right-clicking iron (trap-)doors will be open/close them.
     *
     * @return {@code true} if enabled, otherwise {@code false}
     */
    boolean isOpenTrapDoors();

    /**
     * Sets whether right-clicking iron (trap-)doors will be open/close them.
     *
     * @param openTrapDoors If the iron (trap-)doors are to be opened/closed via right-click
     */
    void setOpenTrapDoors(boolean openTrapDoors);
}