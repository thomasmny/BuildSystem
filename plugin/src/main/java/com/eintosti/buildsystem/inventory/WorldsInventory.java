/*
 * Copyright (c) 2021, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author einTosti
 */
public class WorldsInventory {
    private final static int MAX_NUM_WORLDS = 36;

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final WorldManager worldManager;

    private final Map<UUID, Integer> invIndex;
    private Inventory[] inventories;

    public WorldsInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.worldManager = plugin.getWorldManager();
        this.invIndex = new HashMap<>();
    }

    private Inventory createInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, plugin.getString("world_navigator_title"));

        int numOfWorlds = (numOfWorlds(player) / MAX_NUM_WORLDS) + (numOfWorlds(player) % MAX_NUM_WORLDS == 0 ? 0 : 1);
        inventoryManager.fillMultiInvWithGlass(plugin, inventory, player, invIndex, numOfWorlds);
        addWorldCreateItem(inventory, player);

        return inventory;
    }

    private int numOfWorlds(Player player) {
        int numOfWorlds = 0;
        for (BuildWorld buildWorld : worldManager.getBuildWorlds()) {
            if (isValid(player, buildWorld)) {
                numOfWorlds++;
            }
        }
        return numOfWorlds;
    }

    private void addWorldCreateItem(Inventory inventory, Player player) {
        if (!player.hasPermission("buildsystem.createworld")) {
            inventoryManager.addGlassPane(plugin, player, inventory, 49);
            return;
        }
        inventoryManager.addUrlSkull(inventory, 49, plugin.getString("world_navigator_create_world"), "http://textures.minecraft.net/texture/3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
    }

    private Inventory getInventory(Player player) {
        if (getInvIndex(player) == null) {
            setInvIndex(player, 0);
        }
        addWorlds(player);
        return inventories[getInvIndex(player)];
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void addWorlds(Player player) {
        int columnWorld = 9, maxColumnWorld = 44;
        int numWorlds = numOfWorlds(player);
        int numInventories = (numWorlds % MAX_NUM_WORLDS == 0 ? numWorlds : numWorlds + 1) != 0 ? (numWorlds % MAX_NUM_WORLDS == 0 ? numWorlds : numWorlds + 1) : 1;

        inventories = new Inventory[numInventories];
        Inventory inventory = createInventory(player);

        int index = 0;
        inventories[index] = inventory;
        if (numWorlds == 0) {
            inventoryManager.addUrlSkull(inventory, 22, plugin.getString("world_navigator_no_worlds"), "http://textures.minecraft.net/texture/2e3f50ba62cbda3ecf5479b62fedebd61d76589771cc19286bf2745cd71e47c6");
            return;
        }

        List<BuildWorld> buildWorlds = inventoryManager.sortWorlds(player, worldManager, plugin);
        for (BuildWorld buildWorld : buildWorlds) {
            if (isValid(player, buildWorld)) {
                inventoryManager.addWorldItem(player, inventory, columnWorld++, buildWorld);
            }
            if (columnWorld > maxColumnWorld) {
                columnWorld = 9;
                inventory = createInventory(player);
                inventories[++index] = inventory;
            }
        }
    }

    private boolean isValid(Player player, BuildWorld buildWorld) {
        if (buildWorld.isPrivate()) {
            return false;
        }

        if (player.hasPermission(buildWorld.getPermission()) || buildWorld.getPermission().equalsIgnoreCase("-")) {
            switch (buildWorld.getStatus()) {
                case NOT_STARTED:
                case IN_PROGRESS:
                case ALMOST_FINISHED:
                case FINISHED:
                    if (Bukkit.getWorld(buildWorld.getName()) != null || (Bukkit.getWorld(buildWorld.getName()) == null && !buildWorld.isLoaded())) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    public Integer getInvIndex(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!invIndex.containsKey(playerUUID)) {
            setInvIndex(player, 0);
        }
        return invIndex.get(playerUUID);
    }

    public void setInvIndex(Player player, int index) {
        invIndex.put(player.getUniqueId(), index);
    }

    public void incrementInv(Player player) {
        UUID playerUUID = player.getUniqueId();
        invIndex.put(playerUUID, invIndex.get(playerUUID) + 1);
    }

    public void decrementInv(Player player) {
        UUID playerUUID = player.getUniqueId();
        invIndex.put(playerUUID, invIndex.get(playerUUID) - 1);
    }
}
