/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.listener;

import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.config.ConfigValues;
import de.eintosti.buildsystem.player.PlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;

public class BuildModePreventationListener implements Listener {

    private final ConfigValues configValues;
    private final PlayerManager playerManager;

    public BuildModePreventationListener(BuildSystem plugin) {
        this.configValues = plugin.getConfigValues();
        this.playerManager = plugin.getPlayerManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Stops a player from dropping items when in build-mode.
     *
     * @param event The drop item event
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (configValues.isBuildModeDropItems()) {
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
        if (configValues.isBuildModeMoveItems()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!playerManager.isInBuildMode(player)) {
            return;
        }

        if (player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
            event.setCancelled(true);
        }
    }
}