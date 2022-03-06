/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author einTosti
 */
public class PlayerInventoryClearEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final List<Integer> navigatorSlots;

    public PlayerInventoryClearEvent(Player player, List<Integer> navigatorSlots) {
        this.player = player;
        this.navigatorSlots = navigatorSlots;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Integer> getNavigatorSlots() {
        return navigatorSlots;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
