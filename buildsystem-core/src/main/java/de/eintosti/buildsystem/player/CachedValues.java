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

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Snapshots a player's gameplay state before BuildSystem mutates it, so it can be restored afterwards. Three
 * independent snapshot groups exist: build mode (gamemode and inventory), the navigator (walk and fly speed), and
 * archive worlds (gamemode, inventory and armor). They are deliberately separate — a player can toggle build mode
 * while inside an archive world, and the restores must not overwrite each other.
 *
 * <p>Each group is saved and restored as a unit, because restoring half of one leaves the player in a state neither
 * the caller nor the plugin ever intended (a build inventory in survival, say). Internal to BuildSystem; not part of
 * the public API.
 */
@NullMarked
public class CachedValues {

    private @Nullable GameMode buildGameMode;
    private @Nullable ItemStack @Nullable [] buildInventory;

    private @Nullable Float walkSpeed;
    private @Nullable Float flySpeed;

    private @Nullable GameMode archiveGameMode;
    private @Nullable ItemStack @Nullable [] archiveInventory;
    private @Nullable ItemStack @Nullable [] archiveArmor;

    /**
     * Snapshots gamemode and inventory before build mode replaces them.
     */
    public void saveBuildState(Player player) {
        this.buildGameMode = player.getGameMode();
        this.buildInventory = player.getInventory().getContents();
    }

    /**
     * Restores the state captured by {@link #saveBuildState(Player)}, if any, and clears the snapshot.
     */
    public void resetBuildStateIfPresent(Player player) {
        if (this.buildGameMode != null) {
            player.setGameMode(buildGameMode);
            this.buildGameMode = null;
        }

        if (this.buildInventory != null) {
            player.getInventory().clear();
            player.getInventory().setContents(buildInventory);
            this.buildInventory = null;
        }
    }

    /**
     * Snapshots walk and fly speed before the navigator overrides them.
     */
    public void saveSpeeds(Player player) {
        this.walkSpeed = player.getWalkSpeed();
        this.flySpeed = player.getFlySpeed();
    }

    /**
     * Restores the speeds captured by {@link #saveSpeeds(Player)}, if any, and clears the snapshot.
     */
    public void resetSpeedsIfPresent(Player player) {
        if (this.walkSpeed != null) {
            player.setWalkSpeed(walkSpeed);
            this.walkSpeed = null;
        }

        if (this.flySpeed != null) {
            player.setFlySpeed(flySpeed);
            this.flySpeed = null;
        }
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
        resetBuildStateIfPresent(player);
        resetSpeedsIfPresent(player);
        resetArchiveStateIfPresent(player);
    }
}
