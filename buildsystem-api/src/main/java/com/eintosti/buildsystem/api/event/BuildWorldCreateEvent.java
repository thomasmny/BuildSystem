/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.api.event;

import com.eintosti.buildsystem.api.world.BuildWorld;
import org.bukkit.entity.Player;

/**
 * @author einTosti
 */
public class BuildWorldCreateEvent extends BuildWorldEvent {

    private final Player player;

    public BuildWorldCreateEvent(BuildWorld buildWorld, Player player) {
        super(buildWorld);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
