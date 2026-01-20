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
package de.eintosti.buildsystem.player;

import de.eintosti.buildsystem.api.player.CachedValues;
import java.util.Arrays;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CachedValuesImpl implements CachedValues {

    @Nullable
    private GameMode gameMode;
    @Nullable
    private List<ItemStack> inventory;
    @Nullable
    private Float walkSpeed;
    @Nullable
    private Float flySpeed;

    @Override
    public void saveGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    @Override
    public void resetGameModeIfPresent(Player player) {
        if (this.gameMode == null) {
            return;
        }
        player.setGameMode(gameMode);
        this.gameMode = null;
    }

    @Override
    public void saveInventory(ItemStack[] inventory) {
        this.inventory = Arrays.asList(inventory);
    }

    @Override
    public void resetInventoryIfPresent(Player player) {
        if (this.inventory == null) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setContents(this.inventory.toArray(new ItemStack[0]));
        this.inventory = null;
    }

    @Override
    public void saveWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    @Override
    public void resetWalkSpeedIfPresent(Player player) {
        if (this.walkSpeed == null) {
            return;
        }
        player.setWalkSpeed(walkSpeed);
        this.walkSpeed = null;
    }

    @Override
    public void saveFlySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
    }

    @Override
    public void resetFlySpeedIfPresent(Player player) {
        if (this.flySpeed == null) {
            return;
        }
        player.setFlySpeed(flySpeed);
        this.flySpeed = null;
    }

    @Override
    public void resetCachedValues(Player player) {
        resetGameModeIfPresent(player);
        resetInventoryIfPresent(player);
        resetWalkSpeedIfPresent(player);
        resetFlySpeedIfPresent(player);
    }
}