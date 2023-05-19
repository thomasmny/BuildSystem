/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.version.gamerules;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public interface GameRules {

    Inventory getInventory(Player player, World world);

    void addGameRules(World worldName);

    void toggleGameRule(InventoryClickEvent event, World world);

    void incrementInv(Player player);

    void decrementInv(Player player);

    int getInvIndex(UUID uuid);

    void resetInvIndex(UUID uuid);

    int getNumGameRules();

    int[] getSlots();
}