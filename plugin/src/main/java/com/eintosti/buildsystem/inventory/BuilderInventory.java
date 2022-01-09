/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.Builder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author einTosti
 */
public class BuilderInventory {

    private static final int MAX_BUILDERS = 9;

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    private final Map<UUID, Integer> invIndex;
    private Inventory[] inventories;

    private int numBuilders;

    public BuilderInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();

        this.invIndex = new HashMap<>();
    }

    private Inventory createInventory(BuildWorld buildWorld, Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, plugin.getString("worldeditor_builders_title"));
        fillGuiWithGlass(inventory, player);

        addCreatorInfoItem(inventory, buildWorld);
        addBuilderAddItem(inventory, buildWorld, player);

        return inventory;
    }

    private void addCreatorInfoItem(Inventory inventory, BuildWorld buildWorld) {
        String creatorName = buildWorld.getCreator();
        if (creatorName == null || creatorName.equalsIgnoreCase("-")) {
            inventoryManager.addItemStack(inventory, 4, XMaterial.BARRIER, plugin.getString("worldeditor_builders_no_creator_item"));
        } else {
            inventoryManager.addSkull(inventory, 4, plugin.getString("worldeditor_builders_creator_item"),
                    buildWorld.getCreator(), plugin.getString("worldeditor_builders_creator_lore").replace("%creator%", buildWorld.getCreator()));
        }
    }

    private void addBuilderAddItem(Inventory inventory, BuildWorld buildWorld, Player player) {
        UUID creatorId = buildWorld.getCreatorId();
        if ((creatorId != null && creatorId.equals(player.getUniqueId())) || player.hasPermission("buildsystem.admin")) {
            inventoryManager.addUrlSkull(inventory, 22, plugin.getString("worldeditor_builders_add_builder_item"),
                    "https://textures.minecraft.net/texture/3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 22);
        }
    }

    private void addItems(BuildWorld buildWorld, Player player) {
        ArrayList<Builder> builders = buildWorld.getBuilders();
        this.numBuilders = builders.size();
        int numInventories = (numBuilders % MAX_BUILDERS == 0 ? numBuilders : numBuilders + 1) != 0 ? (numBuilders % MAX_BUILDERS == 0 ? numBuilders : numBuilders + 1) : 1;

        int index = 0;

        Inventory inventory = createInventory(buildWorld, player);

        inventories = new Inventory[numInventories];
        inventories[index] = inventory;

        int columnSkull = 9, maxColumnSkull = 17;
        for (Builder builder : builders) {
            String builderName = builder.getName();
            inventoryManager.addSkull(inventory, columnSkull++, plugin.getString("worldeditor_builders_builder_item").replace("%builder%", builderName),
                    builderName, plugin.getStringList("worldeditor_builders_builder_lore"));

            if (columnSkull > maxColumnSkull) {
                columnSkull = 9;
                inventory = createInventory(buildWorld, player);
                inventories[++index] = inventory;
            }
        }
    }

    public Inventory getInventory(BuildWorld buildWorld, Player player) {
        addItems(buildWorld, player);
        if (getInvIndex(player) == null) {
            setInvIndex(player, 0);
        }
        return inventories[getInvIndex(player)];
    }

    private void fillGuiWithGlass(Inventory inventory, Player player) {
        for (int i = 0; i <= 8; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 19; i <= 25; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }

        int numOfPages = (numBuilders / MAX_BUILDERS) + (numBuilders % MAX_BUILDERS == 0 ? 0 : 1);
        int invIndex = getInvIndex(player);

        if (numOfPages > 1 && invIndex > 0) {
            inventoryManager.addUrlSkull(inventory, 18, plugin.getString("gui_previous_page"), "http://textures.minecraft.net/texture/f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 18);
        }

        if (numOfPages > 1 && invIndex < (numOfPages - 1)) {
            inventoryManager.addUrlSkull(inventory, 26, plugin.getString("gui_next_page"), "http://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 26);
        }
    }

    public Integer getInvIndex(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!invIndex.containsKey(playerUUID)) {
            invIndex.put(playerUUID, 0);
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
