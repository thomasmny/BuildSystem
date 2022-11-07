/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.world.modification;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.navigator.world.FilteredWorldsInventory.Visibility;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.util.PaginatedInventory;
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.data.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileFilter;
import java.util.AbstractMap;
import java.util.UUID;

/**
 * @author einTosti
 */
public class CreateInventory extends PaginatedInventory implements Listener {

    private final BuildSystem plugin;
    private final WorldManager worldManager;
    private final InventoryUtil inventoryUtil;

    private int numTemplates = 0;
    private Visibility visibility;
    private boolean createPrivateWorld;

    public CreateInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryUtil = plugin.getInventoryUtil();
        this.worldManager = plugin.getWorldManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player, Page page) {
        Inventory inventory = Bukkit.createInventory(null, 45, Messages.getString("create_title"));
        fillGuiWithGlass(player, inventory, page);

        addPageItem(inventory, page, Page.PREDEFINED, inventoryUtil.getUrlSkull(Messages.getString("create_predefined_worlds"), "2cdc0feb7001e2c10fd5066e501b87e3d64793092b85a50c856d962f8be92c78"));
        addPageItem(inventory, page, Page.GENERATOR, inventoryUtil.getUrlSkull(Messages.getString("create_generators"), "b2f79016cad84d1ae21609c4813782598e387961be13c15682752f126dce7a"));
        addPageItem(inventory, page, Page.TEMPLATES, inventoryUtil.getUrlSkull(Messages.getString("create_templates"), "d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c"));

        switch (page) {
            case PREDEFINED:
                addPredefinedWorldItem(player, inventory, 29, WorldType.NORMAL, Messages.getString("create_normal_world"));
                addPredefinedWorldItem(player, inventory, 30, WorldType.FLAT, Messages.getString("create_flat_world"));
                addPredefinedWorldItem(player, inventory, 31, WorldType.NETHER, Messages.getString("create_nether_world"));
                addPredefinedWorldItem(player, inventory, 32, WorldType.END, Messages.getString("create_end_world"));
                addPredefinedWorldItem(player, inventory, 33, WorldType.VOID, Messages.getString("create_void_world"));
                break;
            case GENERATOR:
                inventoryUtil.addUrlSkull(inventory, 31, Messages.getString("create_generators_create_world"), "3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
                break;
            case TEMPLATES:
                // Template stuff is done during inventory open
                break;
        }

        return inventory;
    }

    public void openInventory(Player player, Page page, Visibility visibility) {
        this.visibility = visibility;
        this.createPrivateWorld = visibility == Visibility.PRIVATE;

        if (page == Page.TEMPLATES) {
            addTemplates(player, page);
            player.openInventory(inventories[getInvIndex(player)]);
        } else {
            player.openInventory(getInventory(player, page));
        }
    }

    private void addPageItem(Inventory inventory, Page currentPage, Page page, ItemStack itemStack) {
        if (currentPage == page) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        inventory.setItem(page.getSlot(), itemStack);
    }

    private void addPredefinedWorldItem(Player player, Inventory inventory, int position, WorldType worldType, String displayName) {
        XMaterial material = inventoryUtil.getCreateItem(worldType);

        if (!player.hasPermission("buildsystem.create.type." + worldType.name().toLowerCase())) {
            material = XMaterial.BARRIER;
            displayName = "§c§m" + ChatColor.stripColor(displayName);
        }

        inventoryUtil.addItemStack(inventory, position, material, displayName);
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
                inventoryUtil.addItemStack(inventory, i, XMaterial.BARRIER, Messages.getString("create_no_templates"));
            }
            return;
        }

        if (templateFiles == null) {
            return;
        }

        for (File templateFile : templateFiles) {
            inventoryUtil.addItemStack(inventory, columnTemplate++, XMaterial.FILLED_MAP, Messages.getString("create_template", new AbstractMap.SimpleEntry<>("%template%", templateFile.getName())));
            if (columnTemplate > maxColumnTemplate) {
                columnTemplate = 29;
                inventory = getInventory(player, page);
                inventories[++index] = inventory;
            }
        }
    }

    private void fillGuiWithGlass(Player player, Inventory inventory, Page page) {
        for (int i = 0; i <= 28; i++) {
            inventoryUtil.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 34; i <= 44; i++) {
            inventoryUtil.addGlassPane(plugin, player, inventory, i);
        }

        switch (page) {
            case GENERATOR:
                inventoryUtil.addGlassPane(plugin, player, inventory, 29);
                inventoryUtil.addGlassPane(plugin, player, inventory, 30);
                inventoryUtil.addGlassPane(plugin, player, inventory, 32);
                inventoryUtil.addGlassPane(plugin, player, inventory, 33);
                break;
            case TEMPLATES:
                UUID playerUUID = player.getUniqueId();
                if (numTemplates > 1 && invIndex.get(playerUUID) > 0) {
                    inventoryUtil.addUrlSkull(inventory, 38, Messages.getString("gui_previous_page"), "f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2");
                } else {
                    inventoryUtil.addGlassPane(plugin, player, inventory, 38);
                }

                if (numTemplates > 1 && invIndex.get(playerUUID) < (numTemplates - 1)) {
                    inventoryUtil.addUrlSkull(inventory, 42, Messages.getString("gui_next_page"), "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158");
                } else {
                    inventoryUtil.addGlassPane(plugin, player, inventory, 42);
                }
                break;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtil.checkIfValidClick(event, "create_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
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
            openInventory(player, newPage, this.visibility);
            XSound.ENTITY_CHICKEN_EGG.play(player);
            return;
        }

        Inventory inventory = event.getClickedInventory();
        if (inventory == null) {
            return;
        }

        int slot = event.getSlot();

        switch (Page.getCurrentPage(inventory)) {
            case PREDEFINED: {
                WorldType worldType = null;

                switch (slot) {
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

                if (worldType == null || !player.hasPermission("buildsystem.create.type." + worldType.name().toLowerCase())) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }

                worldManager.startWorldNameInput(player, worldType, null, createPrivateWorld);
                XSound.ENTITY_CHICKEN_EGG.play(player);
                break;
            }

            case GENERATOR: {
                if (slot == 31) {
                    worldManager.startWorldNameInput(player, WorldType.CUSTOM, null, createPrivateWorld);
                    XSound.ENTITY_CHICKEN_EGG.play(player);
                }
                break;
            }

            case TEMPLATES: {
                ItemStack itemStack = event.getCurrentItem();
                if (itemStack == null) {
                    return;
                }

                XMaterial xMaterial = XMaterial.matchXMaterial(itemStack);
                switch (xMaterial) {
                    case FILLED_MAP:
                        worldManager.startWorldNameInput(player, WorldType.TEMPLATE, itemStack.getItemMeta().getDisplayName(), createPrivateWorld);
                        break;
                    case PLAYER_HEAD:
                        if (slot == 38) {
                            decrementInv(player);
                        } else if (slot == 42) {
                            incrementInv(player);
                        }
                        openInventory(player, CreateInventory.Page.TEMPLATES, visibility);
                        break;
                    default:
                        return;
                }
                XSound.ENTITY_CHICKEN_EGG.play(player);
                break;
            }
        }
    }

    public enum Page {
        PREDEFINED(12),
        GENERATOR(13),
        TEMPLATES(14);

        private final int slot;

        Page(int slot) {
            this.slot = slot;
        }

        public static Page getCurrentPage(Inventory inventory) {
            for (Page page : Page.values()) {
                ItemStack itemStack = inventory.getItem(page.getSlot());
                if (itemStack != null && itemStack.containsEnchantment(Enchantment.DURABILITY)) {
                    return page;
                }
            }
            return Page.PREDEFINED;
        }

        public int getSlot() {
            return slot;
        }
    }

    private static class TemplateFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && !file.isHidden();
        }
    }
}