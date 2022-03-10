/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.PlayerManager;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.data.WorldStatus;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

/**
 * @author einTosti
 */
public class PlayerInteractAtEntityListener implements Listener {

    private static final double MAX_HEIGHT = 2.074631929397583;
    private static final double MIN_HEIGHT = 1.4409877061843872;

    private final BuildSystem plugin;
    private final PlayerManager playerManager;
    private final WorldManager worldManager;

    public PlayerInteractAtEntityListener(BuildSystem plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        disableArchivedWorlds(player, event);

        if (!playerManager.getOpenNavigator().contains(player) || entity.getType() != EntityType.ARMOR_STAND) {
            return;
        }

        String customName = entity.getCustomName();
        if (customName == null || !customName.contains(" × ")) {
            return;
        }

        event.setCancelled(true);
        Vector clickedPosition = event.getClickedPosition();
        if (clickedPosition.getY() > MIN_HEIGHT && clickedPosition.getY() < MAX_HEIGHT) {
            if (!customName.startsWith(player.getName())) {
                return;
            }

            String invType = customName.replace(player.getName() + " × ", "");

            switch (invType) {
                case "§aWorld Navigator":
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    plugin.getWorldsInventory().openInventory(player);
                    break;
                case "§6World Archive":
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    plugin.getArchiveInventory().openInventory(player);
                    break;
                case "§bPrivate Worlds":
                    XSound.BLOCK_CHEST_OPEN.play(player);
                    plugin.getPrivateInventory().openInventory(player);
                    break;
            }
        } else {
            ItemStack itemStack = player.getItemInHand();
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null) {
                return;
            }

            String displayName = itemMeta.getDisplayName();
            if (!displayName.equals(plugin.getString("barrier_item"))) {
                return;
            }

            event.setCancelled(true);
            playerManager.closeNavigator(player);
        }
    }

    private void disableArchivedWorlds(Player player, PlayerInteractAtEntityEvent event) {
        World bukkitWorld = player.getWorld();
        BuildWorld buildWorld = worldManager.getBuildWorld(bukkitWorld.getName());
        if (buildWorld == null || buildWorld.getStatus() != WorldStatus.ARCHIVE) {
            return;
        }

        if (!playerManager.getBuildPlayers().contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
