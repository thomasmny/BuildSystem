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
package de.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.inventory.XInventoryView;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.config.Config.Settings.BuildMode;
import de.eintosti.buildsystem.player.PlayerServiceImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BuildModePreventationListener implements Listener {

    private final PlayerServiceImpl playerManager;

    public BuildModePreventationListener(BuildSystemPlugin plugin) {
        this.playerManager = plugin.getPlayerService();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Stops a player from dropping items when in build-mode.
     *
     * @param event The drop item event
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (BuildMode.dropItems) {
            return;
        }

        if (playerManager.isInBuildMode(event.getPlayer())) {
            event.getItemDrop().remove();
        }
    }

    /**
     * Stops a player from moving items into another inventory when in build-mode.
     *
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (BuildMode.moveItems) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!playerManager.isInBuildMode(player)) {
            return;
        }

        if (XInventoryView.of(event.getView()).getTopInventory().getType() != InventoryType.CRAFTING) {
            event.setCancelled(true);
        }
    }
}