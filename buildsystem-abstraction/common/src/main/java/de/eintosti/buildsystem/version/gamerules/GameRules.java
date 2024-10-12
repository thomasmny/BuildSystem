/*
 * Copyright (c) 2018-2024, Thomas Meaney
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

package de.eintosti.buildsystem.version.gamerules;

import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface GameRules {

    /**
     * Gets the {@link Inventory} for modifying a world's {@link GameRules}.
     *
     * @param player The player to display the inventory to
     * @param world  The world to modify
     * @return The inventory
     */
    Inventory getInventory(Player player, World world);

    /**
     * Adds all valid {@link GameRules} to the inventory.
     *
     * @param world The world to modify
     */
    void addGameRules(World world);

    /**
     * Called when the player has interacted with the inventory, attempting to modify a game rule.
     *
     * @param event The inventory click event
     * @param world The world to modify
     */
    void modifyGameRule(InventoryClickEvent event, World world);

    /**
     * Increments the current page index. Does not open the new inventory.
     *
     * @param player The player to increment the index for
     */
    void incrementInv(Player player);

    /**
     * Decrements the current page index. Does not open the new inventory.
     *
     * @param player The player to decrement the index for
     */
    void decrementInv(Player player);

    /**
     * Gets the index of current open inventory.
     *
     * @param uuid The uuid of the player to check
     * @return The index of current ofen inventory
     */
    int getInvIndex(UUID uuid);

    /**
     * Resets the index of the current open inventory to {@code 0}.
     *
     * @param uuid The uuid of the player to reset
     */
    void resetInvIndex(UUID uuid);

    /**
     * Gets the total number of {@link GameRules}.
     *
     * @return The total number of game rules
     */
    int getNumGameRules();

    /**
     * Gets an array of all valid slot numbers which can be used to display a game rule.
     *
     * @return All valid slot numbers which can be used to display a game rule.
     */
    int[] getSlots();
}