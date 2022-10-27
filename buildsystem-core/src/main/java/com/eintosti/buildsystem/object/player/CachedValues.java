/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.object.player;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 * @since TODO
 */
public class CachedValues {

    private GameMode gameMode;
    private Float walkSpeed;
    private Float flySpeed;

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void resetGameModeIfPresent(Player player) {
        if (this.gameMode == null) {
            return;
        }
        player.setGameMode(gameMode);
        this.gameMode = null;
    }

    public void setWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public void resetWalkSpeedIfPresent(Player player) {
        if (this.walkSpeed == null) {
            return;
        }
        player.setWalkSpeed(walkSpeed);
        this.walkSpeed = null;
    }

    public void setFlySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
    }

    public void resetFlySpeedIfPresent(Player player) {
        if (this.flySpeed == null) {
            return;
        }
        player.setFlySpeed(flySpeed);
        this.flySpeed = null;
    }
}