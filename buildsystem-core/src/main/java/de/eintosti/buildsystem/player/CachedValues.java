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

import java.util.Arrays;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Snapshots a player's gameplay state before BuildSystem mutates it, so it can be restored afterwards. Two independent
 * snapshot groups exist: one for build mode (gamemode, inventory, walk/fly speed) and one for archive worlds
 * (gamemode, inventory, armor). They are deliberately separate — a player can toggle build mode while inside an
 * archive world, and the two restores must not overwrite each other. Internal to BuildSystem; not part of the public
 * API.
 */
@NullMarked
public class CachedValues {

    private @Nullable GameMode gameMode;
    private @Nullable List<ItemStack> inventory;
    private @Nullable Float walkSpeed;
    private @Nullable Float flySpeed;

    private @Nullable GameMode archiveGameMode;
    private @Nullable ItemStack @Nullable [] archiveInventory;
    private @Nullable ItemStack @Nullable [] archiveArmor;

    public void saveGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void resetGameModeIfPresent(Player player) {
        if (this.gameMode == null) {
            return;
        }
        player.setGameMode(gameMode);
        this.gameMode = null;
    }

    public void saveInventory(ItemStack[] inventory) {
        this.inventory = Arrays.asList(inventory);
    }

    public void resetInventoryIfPresent(Player player) {
        if (this.inventory == null) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setContents(this.inventory.toArray(new ItemStack[0]));
        this.inventory = null;
    }

    public void saveWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public void resetWalkSpeedIfPresent(Player player) {
        if (this.walkSpeed == null) {
            return;
        }
        player.setWalkSpeed(walkSpeed);
        this.walkSpeed = null;
    }

    public void saveFlySpeed(float flySpeed) {
        this.flySpeed = flySpeed;
    }

    public void resetFlySpeedIfPresent(Player player) {
        if (this.flySpeed == null) {
            return;
        }
        player.setFlySpeed(flySpeed);
        this.flySpeed = null;
    }

    /**
     * Snapshots gamemode, inventory and armor before an archive world clears them.
     */
    public void saveArchiveState(Player player) {
        this.archiveGameMode = player.getGameMode();
        this.archiveInventory = player.getInventory().getContents();
        this.archiveArmor = player.getInventory().getArmorContents();
    }

    /**
     * Restores the state captured by {@link #saveArchiveState(Player)}, if any, and clears the snapshot.
     */
    public void resetArchiveStateIfPresent(Player player) {
        if (this.archiveGameMode != null) {
            player.setGameMode(archiveGameMode);
            this.archiveGameMode = null;
        }

        if (this.archiveInventory != null) {
            player.getInventory().clear();
            player.getInventory().setContents(archiveInventory);
            this.archiveInventory = null;
        }

        if (this.archiveArmor != null) {
            player.getInventory().setArmorContents(archiveArmor);
            this.archiveArmor = null;
        }
    }

    public void resetCachedValues(Player player) {
        resetGameModeIfPresent(player);
        resetInventoryIfPresent(player);
        resetWalkSpeedIfPresent(player);
        resetFlySpeedIfPresent(player);
        resetArchiveStateIfPresent(player);
    }
}
