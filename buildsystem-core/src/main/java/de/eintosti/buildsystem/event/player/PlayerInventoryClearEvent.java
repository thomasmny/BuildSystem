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
package de.eintosti.buildsystem.event.player;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

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