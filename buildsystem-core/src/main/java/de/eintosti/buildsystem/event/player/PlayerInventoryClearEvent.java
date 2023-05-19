/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Thrown when a player's inventory is cleared.
 */
public class PlayerInventoryClearEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final List<Integer> navigatorSlots;

    public PlayerInventoryClearEvent(Player player, List<Integer> navigatorSlots) {
        super(player);
        this.navigatorSlots = navigatorSlots;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public List<Integer> getNavigatorSlots() {
        return navigatorSlots;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}