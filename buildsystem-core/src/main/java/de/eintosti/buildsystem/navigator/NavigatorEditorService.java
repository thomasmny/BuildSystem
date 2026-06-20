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
package de.eintosti.buildsystem.navigator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * Manages the inventory takeover shared by the drag-and-drop layout editors (the navigator and the status picker). An
 * editor repurposes the player's own inventory to show its controls, so this service snapshots the real contents before
 * the editor opens and restores them when the editor closes, the player quits, or the server stops. A player only ever
 * has one such editor open at a time, so a single snapshot per player is enough. The snapshot lives in memory; a
 * graceful shutdown restores it via {@link #restoreAll()}.
 */
@NullMarked
public class NavigatorEditorService {

    private final Map<UUID, ItemStack[]> snapshots = new HashMap<>();

    /**
     * Snapshots and clears the player's inventory in preparation for the editor's control items, unless a session is
     * already active (so re-rendering the editor never overwrites the saved snapshot).
     *
     * @param player The editing player
     */
    public void beginSession(Player player) {
        if (snapshots.containsKey(player.getUniqueId())) {
            return;
        }
        snapshots.put(player.getUniqueId(), player.getInventory().getContents().clone());
        player.getInventory().clear();
    }

    /**
     * {@return whether the player currently has the editor open (a snapshot is held)}
     *
     * @param player The player to check
     */
    public boolean hasSession(Player player) {
        return snapshots.containsKey(player.getUniqueId());
    }

    /**
     * Restores the player's snapshotted inventory and clears their cursor, ending the editing session. No-op when the
     * player has no active session.
     *
     * @param player The editing player
     */
    public void restore(Player player) {
        ItemStack[] snapshot = snapshots.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        player.getInventory().setContents(snapshot);
        player.setItemOnCursor(null);
    }

    /**
     * Restores every active session, used on plugin disable so a server stop never strands a player's items.
     */
    public void restoreAll() {
        for (UUID uuid : Map.copyOf(snapshots).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                restore(player);
            } else {
                snapshots.remove(uuid);
            }
        }
    }
}
