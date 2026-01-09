/*
 * Copyright (c) 2018-2026, Thomas Meaney
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
package de.eintosti.buildsystem.api.player;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.NullMarked;

/**
 * Interface for managing cached values of a player.
 *
 * @since 3.0.0
 */
@Internal
@NullMarked
public interface CachedValues {

    /**
     * Saves the given {@link GameMode} to be restored later.
     *
     * @param gameMode The game mode to save
     */
    void saveGameMode(GameMode gameMode);

    /**
     * Resets the player's game mode to the previously saved one, if present.
     *
     * @param player The player whose game mode is to be reset
     */
    void resetGameModeIfPresent(Player player);

    /**
     * Saves the given inventory contents to be restored later.
     *
     * @param inventory The inventory contents to save
     */
    void saveInventory(ItemStack[] inventory);

    /**
     * Resets the player's inventory to the previously saved one, if present.
     *
     * @param player The player whose inventory is to be reset
     */
    void resetInventoryIfPresent(Player player);

    /**
     * Saves the given walk speed to be restored later.
     *
     * @param walkSpeed The walk speed to save
     */
    void saveWalkSpeed(float walkSpeed);

    /**
     * Resets the player's walk speed to the previously saved one, if present.
     *
     * @param player The player whose walk speed is to be reset
     */
    void resetWalkSpeedIfPresent(Player player);

    /**
     * Saves the given fly speed to be restored later.
     *
     * @param flySpeed The fly speed to save
     */
    void saveFlySpeed(float flySpeed);

    /**
     * Resets the player's fly speed to the previously saved one, if present.
     *
     * @param player The player whose fly speed is to be reset
     */
    void resetFlySpeedIfPresent(Player player);

    /**
     * Resets all cached values for the given player.
     *
     * @param player The player whose cached values are to be reset
     */
    void resetCachedValues(Player player);
}