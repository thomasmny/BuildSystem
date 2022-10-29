/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.command.subcommand.worlds.AddBuilderSubCommand;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.object.world.BuildWorld;
import com.eintosti.buildsystem.object.world.Builder;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.util.UUIDFetcher;
import com.eintosti.buildsystem.util.external.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.AbstractMap;
import java.util.List;
import java.util.UUID;

/**
 * @author einTosti
 */
public class BuilderInventory extends PaginatedInventory implements Listener {

    private static final int MAX_BUILDERS = 9;

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    private int numBuilders = 0;

    public BuilderInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory createInventory(BuildWorld buildWorld, Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Messages.getString("worldeditor_builders_title"));
        fillGuiWithGlass(inventory, player);

        addCreatorInfoItem(inventory, buildWorld);
        addBuilderAddItem(inventory, buildWorld, player);

        return inventory;
    }

    private void addCreatorInfoItem(Inventory inventory, BuildWorld buildWorld) {
        String creatorName = buildWorld.getCreator();
        if (creatorName == null || creatorName.equalsIgnoreCase("-")) {
            inventoryManager.addItemStack(inventory, 4, XMaterial.BARRIER, Messages.getString("worldeditor_builders_no_creator_item"));
        } else {
            inventoryManager.addSkull(inventory, 4, Messages.getString("worldeditor_builders_creator_item"),
                    buildWorld.getCreator(), Messages.getString("worldeditor_builders_creator_lore", new AbstractMap.SimpleEntry<>("%creator%", buildWorld.getCreator())));
        }
    }

    private void addBuilderAddItem(Inventory inventory, BuildWorld buildWorld, Player player) {
        UUID creatorId = buildWorld.getCreatorId();
        if ((creatorId != null && creatorId.equals(player.getUniqueId())) || player.hasPermission(BuildSystem.ADMIN_PERMISSION)) {
            inventoryManager.addUrlSkull(inventory, 22, Messages.getString("worldeditor_builders_add_builder_item"),
                    "3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 22);
        }
    }

    private void addItems(BuildWorld buildWorld, Player player) {
        List<Builder> builders = buildWorld.getBuilders();
        this.numBuilders = builders.size();
        int numInventories = (numBuilders % MAX_BUILDERS == 0 ? numBuilders : numBuilders + 1) != 0 ? (numBuilders % MAX_BUILDERS == 0 ? numBuilders : numBuilders + 1) : 1;

        int index = 0;

        Inventory inventory = createInventory(buildWorld, player);

        inventories = new Inventory[numInventories];
        inventories[index] = inventory;

        int columnSkull = 9, maxColumnSkull = 17;
        for (Builder builder : builders) {
            String builderName = builder.getName();
            inventoryManager.addSkull(inventory, columnSkull++, Messages.getString("worldeditor_builders_builder_item", new AbstractMap.SimpleEntry<>("%builder%", builderName)),
                    builderName, Messages.getStringList("worldeditor_builders_builder_lore"));

            if (columnSkull > maxColumnSkull) {
                columnSkull = 9;
                inventory = createInventory(buildWorld, player);
                inventories[++index] = inventory;
            }
        }
    }

    public Inventory getInventory(BuildWorld buildWorld, Player player) {
        addItems(buildWorld, player);
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
            inventoryManager.addUrlSkull(inventory, 18, Messages.getString("gui_previous_page"), "f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 18);
        }

        if (numOfPages > 1 && invIndex < (numOfPages - 1)) {
            inventoryManager.addUrlSkull(inventory, 26, Messages.getString("gui_next_page"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
        } else {
            inventoryManager.addGlassPane(plugin, player, inventory, 26);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "worldeditor_builders_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        BuildWorld buildWorld = plugin.getPlayerManager().getBuildPlayer(player).getCachedWorld();
        if (buildWorld == null) {
            player.closeInventory();
            Messages.sendMessage(player, "worlds_addbuilder_error");
            return;
        }

        ItemStack itemStack = event.getCurrentItem();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return;
        }

        Material material = itemStack.getType();
        if (material != XMaterial.PLAYER_HEAD.parseMaterial()) {
            XSound.BLOCK_CHEST_OPEN.play(player);
            plugin.getEditInventory().openInventory(player, buildWorld);
            return;
        }

        int slot = event.getSlot();
        switch (slot) {
            case 18:
                decrementInv(player);
                break;
            case 22:
                XSound.ENTITY_CHICKEN_EGG.play(player);
                new AddBuilderSubCommand(plugin, buildWorld.getName()).getAddBuilderInput(player, buildWorld, false);
                return;
            case 26:
                incrementInv(player);
                break;
            default:
                if (slot == 4) {
                    return;
                }
                if (!itemMeta.hasDisplayName()) {
                    return;
                }
                if (!event.isShiftClick()) {
                    return;
                }

                String template = Messages.getString("worldeditor_builders_builder_item", new AbstractMap.SimpleEntry<>("%builder%", ""));
                String builderName = StringUtils.difference(template, itemMeta.getDisplayName());
                UUID builderId = UUIDFetcher.getUUID(builderName);
                buildWorld.removeBuilder(builderId);

                XSound.ENTITY_ENDERMAN_TELEPORT.play(player);
                Messages.sendMessage(player, "worlds_removebuilder_removed", new AbstractMap.SimpleEntry<>("%builder%", builderName));
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.openInventory(getInventory(buildWorld, player));
    }
}