/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.version;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

/**
 * @author einTosti
 */
public interface GameRules {

    Inventory getInventory(Player player, World worldName);

    void addGameRules(World worldName);

    void toggleGameRule(InventoryClickEvent event, World world);

    void incrementInv(Player player);

    void decrementInv(Player player);

    int getInvIndex(UUID uuid);

    int getNumGameRules();

    int[] getSlots();
}
