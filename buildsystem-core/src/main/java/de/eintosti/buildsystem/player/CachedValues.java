/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.player;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author einTosti
 * @since 2.21.0
 */
public class CachedValues {

    private GameMode gameMode;
    private ItemStack[] inventory;

    private Float walkSpeed;
    private Float flySpeed;

    public void saveGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void resetGameModeIfPresent(Player player) {
        if (this.gameMode == null) {
            return;
        }
        player.setGameMode(gameMode);
        this.gameMode = null;
    }

    public void saveInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    public void resetInventoryIfPresent(Player player) {
        if (this.inventory == null) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setContents(this.inventory);
        this.inventory = null;
    }

    public void saveWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public void resetWalkSpeedIfPresent(Player player) {
        if (this.walkSpeed == null) {
            return;
        }
        player.setWalkSpeed(walkSpeed);
        this.walkSpeed = null;
    }

    public void saveFlySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
    }

    public void resetFlySpeedIfPresent(Player player) {
        if (this.flySpeed == null) {
            return;
        }
        player.setFlySpeed(flySpeed);
        this.flySpeed = null;
    }

    public void resetCachedValues(Player player) {
        resetGameModeIfPresent(player);
        resetInventoryIfPresent(player);
        resetWalkSpeedIfPresent(player);
        resetFlySpeedIfPresent(player);
    }
}