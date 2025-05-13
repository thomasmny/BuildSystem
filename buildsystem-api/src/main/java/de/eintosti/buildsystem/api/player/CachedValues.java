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
package de.eintosti.buildsystem.api.player;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface CachedValues {

    void saveGameMode(GameMode gameMode);

    void resetGameModeIfPresent(Player player);

    void saveInventory(ItemStack[] inventory);

    void resetInventoryIfPresent(Player player);

    void saveWalkSpeed(float walkSpeed);

    void resetWalkSpeedIfPresent(Player player);

    void saveFlySpeed(float flySpeed);

    void resetFlySpeedIfPresent(Player player);

    void resetCachedValues(Player player);
}