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
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.manager.WorldManager;
import com.eintosti.buildsystem.object.world.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileFilter;
import java.util.UUID;

/**
 * @author einTosti
 */
public class CreateInventory extends PaginatedInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;
    private final WorldManager worldManager;

    private int numTemplates = 0;

    public CreateInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player, Page page) {
        Inventory inventory = Bukkit.createInventory(null, 45, plugin.getString("create_title"));
        fillGuiWithGlass(player, inventory, page);

        addPageItem(inventory, page, Page.PREDEFINED, 12, inventoryManager.getUrlSkull(plugin.getString("create_predefined_worlds"), "https://textures.minecraft.net/texture/2cdc0feb7001e2c10fd5066e501b87e3d64793092b85a50c856d962f8be92c78"));
        addPageItem(inventory, page, Page.GENERATOR, 13, inventoryManager.getUrlSkull(plugin.getString("create_generators"), "https://textures.minecraft.net/texture/b2f79016cad84d1ae21609c4813782598e387961be13c15682752f126dce7a"));
        addPageItem(inventory, page, Page.TEMPLATES, 14, inventoryManager.getUrlSkull(plugin.getString("create_templates"), "https://textures.minecraft.net/texture/d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c"));

        switch (page) {
            case PREDEFINED:
                inventoryManager.addItemStack(inventory, 29, inventoryManager.getCreateItem(WorldType.NORMAL), plugin.getString("create_normal_world"));
                inventoryManager.addItemStack(inventory, 30, inventoryManager.getCreateItem(WorldType.FLAT), plugin.getString("create_flat_world"));
                inventoryManager.addItemStack(inventory, 31, inventoryManager.getCreateItem(WorldType.NETHER), plugin.getString("create_nether_world"));
                inventoryManager.addItemStack(inventory, 32, inventoryManager.getCreateItem(WorldType.END), plugin.getString("create_end_world"));
                inventoryManager.addItemStack(inventory, 33, inventoryManager.getCreateItem(WorldType.VOID), plugin.getString("create_void_world"));
                break;
            case GENERATOR:
                inventoryManager.addUrlSkull(inventory, 31, plugin.getString("create_generators_create_world"), "https://textures.minecraft.net/texture/3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
                break;
            case TEMPLATES:
                // Template stuff is done during inventory open
                break;
        }

        return inventory;
    }

    public void openInventory(Player player, Page page) {
        if (page == Page.TEMPLATES) {
            addTemplates(player, page);
            player.openInventory(inventories[getInvIndex(player)]);
        } else {
            player.openInventory(getInventory(player, page));
        }
    }

    private void addPageItem(Inventory inventory, Page currentPage, Page page, int position, ItemStack itemStack) {
        if (currentPage == page) {
            itemStack.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        }
        inventory.setItem(position, itemStack);
    }

    private void addTemplates(Player player, Page page) {
        final int maxNumTemplates = 5;
        File[] templateFiles = new File(plugin.getDataFolder() + File.separator + "templates").listFiles(new TemplateFilter());

        int columnTemplate = 29, maxColumnTemplate = 33;
        int fileLength = templateFiles != null ? templateFiles.length : 0;
        this.numTemplates = (fileLength / maxNumTemplates) + (fileLength % maxNumTemplates == 0 ? 0 : 1);
        int numInventories = (numTemplates % maxNumTemplates == 0 ? numTemplates : numTemplates + 1) != 0 ? (numTemplates % maxNumTemplates == 0 ? numTemplates : numTemplates + 1) : 1;

        inventories = new Inventory[numInventories];
        Inventory inventory = getInventory(player, page);

        int index = 0;
        inventories[index] = inventory;
        if (numTemplates == 0) {
            for (int i = 29; i <= 33; i++) {
                inventoryManager.addItemStack(inventory, i, XMaterial.BARRIER, plugin.getString("create_no_templates"));
            }
            return;
        }

        if (templateFiles == null) {
            return;
        }

        for (File templateFile : templateFiles) {
            inventoryManager.addItemStack(inventory, columnTemplate++, XMaterial.FILLED_MAP, plugin.getString("create_template").replace("%template%", templateFile.getName()));
            if (columnTemplate > maxColumnTemplate) {
                columnTemplate = 29;
                inventory = getInventory(player, page);
                inventories[++index] = inventory;
            }
        }
    }

    private void fillGuiWithGlass(Player player, Inventory inventory, Page page) {
        for (int i = 0; i <= 28; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 34; i <= 44; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }

        switch (page) {
            case GENERATOR:
                inventoryManager.addGlassPane(plugin, player, inventory, 29);
                inventoryManager.addGlassPane(plugin, player, inventory, 30);
                inventoryManager.addGlassPane(plugin, player, inventory, 32);
                inventoryManager.addGlassPane(plugin, player, inventory, 33);
                break;
            case TEMPLATES:
                UUID playerUUID = player.getUniqueId();
                if (numTemplates > 1 && invIndex.get(playerUUID) > 0) {
                    inventoryManager.addUrlSkull(inventory, 38, plugin.getString("gui_previous_page"), "https://textures.minecraft.net/texture/f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
                } else {
                    inventoryManager.addGlassPane(plugin, player, inventory, 38);
                }

                if (numTemplates > 1 && invIndex.get(playerUUID) < (numTemplates - 1)) {
                    inventoryManager.addUrlSkull(inventory, 42, plugin.getString("gui_next_page"), "https://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
                } else {
                    inventoryManager.addGlassPane(plugin, player, inventory, 42);
                }
                break;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "create_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        boolean privateWorld = worldManager.createPrivateWorldPlayers.contains(player);
        CreateInventory.Page newPage = null;

        switch (event.getSlot()) {
            case 12:
                newPage = CreateInventory.Page.PREDEFINED;
                break;
            case 13:
                newPage = CreateInventory.Page.GENERATOR;
                break;
            case 14:
                newPage = CreateInventory.Page.TEMPLATES;
                break;
        }

        if (newPage != null) {
            openInventory(player, newPage);
            if (privateWorld) {
                worldManager.createPrivateWorldPlayers.add(player);
            }
            XSound.ENTITY_CHICKEN_EGG.play(player);
            return;
        }

        Inventory inventory = event.getClickedInventory();
        if (inventory == null) {
            return;
        }

        boolean createPrivateWorld = worldManager.createPrivateWorldPlayers.contains(player);

        switch (getCurrentPage(inventory)) {
            case PREDEFINED: {
                WorldType worldType = null;

                switch (event.getSlot()) {
                    case 29:
                        worldType = WorldType.NORMAL;
                        break;
                    case 30:
                        worldType = WorldType.FLAT;
                        break;
                    case 31:
                        worldType = WorldType.NETHER;
                        break;
                    case 32:
                        worldType = WorldType.END;
                        break;
                    case 33:
                        worldType = WorldType.VOID;
                        break;
                }

                if (worldType != null) {
                    worldManager.startWorldNameInput(player, worldType, null, createPrivateWorld);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                }
                break;
            }

            case TEMPLATES: {
                ItemStack itemStack = event.getCurrentItem();
                if (itemStack.getType() == XMaterial.FILLED_MAP.parseMaterial()) {
                    worldManager.startWorldNameInput(player, WorldType.TEMPLATE, itemStack.getItemMeta().getDisplayName(), createPrivateWorld);
                } else if (itemStack.getType() == XMaterial.PLAYER_HEAD.parseMaterial()) {
                    switch (event.getSlot()) {
                        case 38:
                            decrementInv(player);
                            break;
                        case 42:
                            incrementInv(player);
                            break;
                    }
                    openInventory(player, CreateInventory.Page.TEMPLATES);
                }
                XSound.ENTITY_CHICKEN_EGG.play(player);
                break;
            }

            default: {
                if (event.getSlot() == 31) {
                    worldManager.startWorldNameInput(player, WorldType.CUSTOM, null, createPrivateWorld);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                }
                break;
            }
        }
    }

    private CreateInventory.Page getCurrentPage(Inventory inventory) {
        if (inventory.getItem(12).containsEnchantment(Enchantment.KNOCKBACK)) {
            return CreateInventory.Page.PREDEFINED;
        } else if (inventory.getItem(13).containsEnchantment(Enchantment.KNOCKBACK)) {
            return CreateInventory.Page.GENERATOR;
        } else {
            return CreateInventory.Page.TEMPLATES;
        }
    }

    public enum Page {
        PREDEFINED, TEMPLATES, GENERATOR
    }

    private static class TemplateFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && !file.isHidden();
        }
    }
}
