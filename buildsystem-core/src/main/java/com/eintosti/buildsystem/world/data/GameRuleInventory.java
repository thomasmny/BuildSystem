/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.world.data;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.version.gamerules.GameRules;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.UUID;

/**
 * @author einTosti
 */
public class GameRuleInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryUtil inventoryManager;

    public GameRuleInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openInventory(Player player, BuildWorld buildWorld) {
        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());

        Inventory inventory = plugin.getGameRules().getInventory(player, bukkitWorld);
        fillGuiWithGlass(player, inventory);

        player.openInventory(inventory);
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!isValidSlot(i)) {
                inventoryManager.addGlassPane(plugin, player, inventory, i);
            }
        }

        UUID playerUUID = player.getUniqueId();
        GameRules gameRules = plugin.getGameRules();
        int numGameRules = gameRules.getNumGameRules();
        int invIndex = gameRules.getInvIndex(playerUUID);

        if (numGameRules > 1 && invIndex > 0) {
            inventoryManager.addUrlSkull(inventory, 36, Messages.getString("gui_previous_page"), "f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 36);
        }

        if (numGameRules > 1 && invIndex < (numGameRules - 1)) {
            inventoryManager.addUrlSkull(inventory, 44, Messages.getString("gui_next_page"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 44);
        }
    }

    private boolean isValidSlot(int slot) {
        return Arrays.stream(plugin.getGameRules().getSlots()).anyMatch(i -> i == slot);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "worldeditor_gamerules_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = plugin.getPlayerManager().getBuildPlayer(player).getCachedWorld();
        if (buildWorld == null) {
            player.closeInventory();
            Messages.sendMessage(player, "worlds_edit_error");
            return;
        }

        GameRules gameRules = plugin.getGameRules();

        switch (XMaterial.matchXMaterial(event.getCurrentItem())) {
            case PLAYER_HEAD:
                int slot = event.getSlot();
                if (slot == 36) {
                    gameRules.decrementInv(player);
                } else if (slot == 44) {
                    gameRules.incrementInv(player);
                }
                break;

            case FILLED_MAP:
            case MAP:
                World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
                gameRules.toggleGameRule(event, bukkitWorld);
                break;

            default:
                XSound.BLOCK_CHEST_OPEN.play(player);
                plugin.getEditInventory().openInventory(player, buildWorld);
                return;
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        openInventory(player, buildWorld);
    }
}