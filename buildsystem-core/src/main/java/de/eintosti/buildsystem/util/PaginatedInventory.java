/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.util;

import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class PaginatedInventory {

    protected final Map<UUID, Integer> invIndex;
    protected Inventory[] inventories;

    public PaginatedInventory() {
        this.invIndex = new HashMap<>();
    }

    public int getInvIndex(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!invIndex.containsKey(playerUUID)) {
            setInvIndex(player, 0);
        }
        return invIndex.get(playerUUID);
    }

    public void setInvIndex(Player player, int index) {
        invIndex.put(player.getUniqueId(), index);
    }

    /**
     * Attempts to go to the previous page of an inventory.
     *
     * @param player        The player
     * @param numObjects    The amount of objects to display on the page
     * @param maxNumObjects The maximum amount of objects per page
     * @return {@code true} if the index was decremented (i.e. the page was changed), otherwise {@code false}.
     */
    public boolean decrementInv(Player player, int numObjects, int maxNumObjects) {
        int numOfPages = (numObjects / maxNumObjects) + (numObjects % maxNumObjects == 0 ? 0 : 1);
        UUID playerUUID = player.getUniqueId();

        int index = getInvIndex(player);
        if (numOfPages > 1 && index > 0) {
            invIndex.put(playerUUID, index - 1);
            XSound.ENTITY_CHICKEN_EGG.play(player);
            return true;
        }

        XSound.ENTITY_ITEM_BREAK.play(player);
        return false;
    }

    /**
     * Attempts to go to the next page of an inventory.
     *
     * @param player        The player
     * @param numObjects    The amount of objects to display on the page
     * @param maxNumObjects The maximum amount of objects per page
     * @return {@code true} if the index was incremented (i.e. the page was changed), otherwise {@code false}.
     */
    public boolean incrementInv(Player player, int numObjects, int maxNumObjects) {
        int numOfPages = (numObjects / maxNumObjects) + (numObjects % maxNumObjects == 0 ? 0 : 1);
        UUID playerUUID = player.getUniqueId();

        int index = getInvIndex(player);
        if (numOfPages > 1 && index < (numOfPages - 1)) {
            invIndex.put(playerUUID, index + 1);
            XSound.ENTITY_CHICKEN_EGG.play(player);
            return true;
        }

        XSound.ENTITY_ITEM_BREAK.play(player);
        return false;
    }
}