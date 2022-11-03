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
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.manager.InventoryManager;
import com.eintosti.buildsystem.version.util.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author einTosti
 */
public class BlocksInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public BlocksInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, Messages.getString("blocks_title"));
        fillGuiWithGlass(player, inventory);

        inventoryManager.addUrlSkull(inventory, 1, Messages.getString("blocks_full_oak_barch"), "22e4bb979efefd2ddb3f8b1545e59cd360492e12671ec371efc1f88af21ab83");
        inventoryManager.addUrlSkull(inventory, 2, Messages.getString("blocks_full_spruce_barch"), "966cbdef8efb914d43a213be66b5396f75e5c1b9124f76f67d7cd32525748");
        inventoryManager.addUrlSkull(inventory, 3, Messages.getString("blocks_full_birch_barch"), "a221f813dacee0fef8c59f76894dbb26415478d9ddfc44c2e708a6d3b7549b");
        inventoryManager.addUrlSkull(inventory, 4, Messages.getString("blocks_full_jungle_barch"), "1cefc19380683015e47c666e5926b15ee57ab33192f6a7e429244cdffcc262");
        inventoryManager.addUrlSkull(inventory, 5, Messages.getString("blocks_full_acacia_barch"), "96a3bba2b7a2b4fa46945b1471777abe4599695545229e782259aed41d6");
        inventoryManager.addUrlSkull(inventory, 6, Messages.getString("blocks_full_dark_oak_barch"), "cde9d4e4c343afdb3ed68038450fc6a67cd208b2efc99fb622c718d24aac");

        inventoryManager.addUrlSkull(inventory, 10, Messages.getString("blocks_red_mushroom"), "732dbd6612e9d3f42947b5ca8785bfb334258f3ceb83ad69a5cdeebea4cd65");
        inventoryManager.addUrlSkull(inventory, 11, Messages.getString("blocks_brown_mushroom"), "fa49eca0369d1e158e539d78149acb1572949b88ba921d9ee694fea4c726b3");
        inventoryManager.addUrlSkull(inventory, 12, Messages.getString("blocks_full_mushroom_stem"), "f55fa642d5ebcba2c5246fe6499b1c4f6803c10f14f5299c8e59819d5dc");
        inventoryManager.addUrlSkull(inventory, 13, Messages.getString("blocks_mushroom_stem"), "84d541275c7f924bcb9eb2dbbf4b866b7649c330a6a013b53d584fd4ddf186ca");
        inventoryManager.addUrlSkull(inventory, 14, Messages.getString("blocks_mushroom_block"), "3fa39ccf4788d9179a8795e6b72382d49297b39217146eda68ae78384355b13");

        inventoryManager.addUrlSkull(inventory, 19, Messages.getString("blocks_smooth_stone"), "8dd0cd158c2bb6618650e3954b2d29237f5b4c0ddc7d258e17380ab6979f071");
        inventoryManager.addUrlSkull(inventory, 20, Messages.getString("blocks_double_stone_slab"), "151e70169ea00f04a9439221cf33770844159dd775fc8830e311fd9b5ccd2969");
        inventoryManager.addUrlSkull(inventory, 21, Messages.getString("blocks_smooth_sandstone"), "38fffbb0b8fdec6f62b17c451ab214fb86e4e355b116be961a9ae93eb49a43");
        inventoryManager.addUrlSkull(inventory, 22, Messages.getString("blocks_smooth_red_sandstone"), "a2da7aa1ae6cc9d6c36c18a460d2398162edc2207fdfc9e28a7bf84d7441b8a2");

        inventoryManager.addUrlSkull(inventory, 28, Messages.getString("blocks_powered_redstone_lamp"), "7eb4b34519fe15847dbea7229179feeb6ea57712d165dcc8ff6b785bb58911b0");
        inventoryManager.addUrlSkull(inventory, 29, Messages.getString("blocks_burning_furnace"), "d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c");
        inventoryManager.addUrlSkull(inventory, 30, Messages.getString("blocks_piston_head"), "aa868ce917c09af8e4c350a5807041f6509bf2b89aca45e591fbbd7d4b117d");
        inventoryManager.addUrlSkull(inventory, 31, Messages.getString("blocks_command_block"), "8514d225b262d847c7e557b474327dcef758c2c5882e41ee6d8c5e9cd3bc914");
        inventoryManager.addUrlSkull(inventory, 32, Messages.getString("blocks_barrier"), "3ed1aba73f639f4bc42bd48196c715197be2712c3b962c97ebf9e9ed8efa025");
        if (MinecraftVersion.getCurrent().isEqualOrHigherThan(MinecraftVersion.CAVES_17)) {
            inventoryManager.addUrlSkull(inventory, 33, Messages.getString("blocks_invisible_item_frame"), "8122a503d7a6f57802b03af7624194a4c4f5077a99ae21dd276ce7db88bc38ae");
        }

        inventoryManager.addUrlSkull(inventory, 37, Messages.getString("blocks_mob_spawner"), "db6bd9727abb55d5415265789d4f2984781a343c68dcaf57f554a5e9aa1cd");
        inventoryManager.addUrlSkull(inventory, 38, Messages.getString("blocks_nether_portal"), "b0bfc2577f6e26c6c6f7365c2c4076bccee653124989382ce93bca4fc9e39b");
        inventoryManager.addUrlSkull(inventory, 39, Messages.getString("blocks_end_portal"), "7840b87d52271d2a755dedc82877e0ed3df67dcc42ea479ec146176b02779a5");
        inventoryManager.addUrlSkull(inventory, 40, Messages.getString("blocks_dragon_egg"), "3c151fb54b21fe5769ffb4825b5bc92da73657f214380e5d0301e45b6c13f7d");

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        int[] glassSlots = {0, 8, 9, 17, 18, 26, 27, 35, 36, 44};
        for (int i : glassSlots) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "blocks_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        PlayerInventory playerInventory = player.getInventory();
        switch (event.getSlot()) {
            case 1:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_full_oak_barch"), "22e4bb979efefd2ddb3f8b1545e59cd360492e12671ec371efc1f88af21ab83"));
                break;
            case 2:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_full_spruce_barch"), "966cbdef8efb914d43a213be66b5396f75e5c1b9124f76f67d7cd32525748"));
                break;
            case 3:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_full_birch_barch"), "a221f813dacee0fef8c59f76894dbb26415478d9ddfc44c2e708a6d3b7549b"));
                break;
            case 4:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_full_jungle_barch"), "1cefc19380683015e47c666e5926b15ee57ab33192f6a7e429244cdffcc262"));
                break;
            case 5:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_full_acacia_barch"), "96a3bba2b7a2b4fa46945b1471777abe4599695545229e782259aed41d6"));
                break;
            case 6:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_full_dark_oak_barch"), "cde9d4e4c343afdb3ed68038450fc6a67cd208b2efc99fb622c718d24aac"));
                break;

            case 10:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_red_mushroom"), "732dbd6612e9d3f42947b5ca8785bfb334258f3ceb83ad69a5cdeebea4cd65"));
                break;
            case 11:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_brown_mushroom"), "fa49eca0369d1e158e539d78149acb1572949b88ba921d9ee694fea4c726b3"));
                break;
            case 12:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_full_mushroom_stem"), "f55fa642d5ebcba2c5246fe6499b1c4f6803c10f14f5299c8e59819d5dc"));
                break;
            case 13:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_mushroom_stem"), "84d541275c7f924bcb9eb2dbbf4b866b7649c330a6a013b53d584fd4ddf186ca"));
                break;
            case 14:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_mushroom_block"), "3fa39ccf4788d9179a8795e6b72382d49297b39217146eda68ae78384355b13"));
                break;

            case 19:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_smooth_stone"), "8dd0cd158c2bb6618650e3954b2d29237f5b4c0ddc7d258e17380ab6979f071"));
                break;
            case 20:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_double_stone_slab"), "151e70169ea00f04a9439221cf33770844159dd775fc8830e311fd9b5ccd2969"));
                break;
            case 21:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_smooth_sandstone"), "38fffbb0b8fdec6f62b17c451ab214fb86e4e355b116be961a9ae93eb49a43"));
                break;
            case 22:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_smooth_red_sandstone"), "a2da7aa1ae6cc9d6c36c18a460d2398162edc2207fdfc9e28a7bf84d7441b8a2"));
                break;

            case 28:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_powered_redstone_lamp"), "7eb4b34519fe15847dbea7229179feeb6ea57712d165dcc8ff6b785bb58911b0"));
                break;
            case 29:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_burning_furnace"), "d17b8b43f8c4b5cfeb919c9f8fe93f26ceb6d2b133c2ab1eb339bd6621fd309c"));
                break;
            case 30:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_piston_head"), "aa868ce917c09af8e4c350a5807041f6509bf2b89aca45e591fbbd7d4b117d"));
                break;
            case 31:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_command_block"), "8514d225b262d847c7e557b474327dcef758c2c5882e41ee6d8c5e9cd3bc914"));
                break;
            case 32:
                playerInventory.addItem(inventoryManager.getItemStack(XMaterial.BARRIER, "§bBarrier"));
                break;
            case 33:
                if (MinecraftVersion.getCurrent().isLowerThan(MinecraftVersion.CAVES_17)) {
                    return;
                }
                ItemStack itemStack = inventoryManager.getItemStack(XMaterial.ITEM_FRAME, "§bInvisible Item Frame");
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                // Inline imports to allow backwards compatibility
                itemMeta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "invisible-itemframe"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                itemStack.setItemMeta(itemMeta);
                playerInventory.addItem(itemStack);
                break;

            case 37:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_mob_spawner"), "db6bd9727abb55d5415265789d4f2984781a343c68dcaf57f554a5e9aa1cd"));
                break;
            case 38:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_nether_portal"), "b0bfc2577f6e26c6c6f7365c2c4076bccee653124989382ce93bca4fc9e39b"));
                break;
            case 39:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_end_portal"), "7840b87d52271d2a755dedc82877e0ed3df67dcc42ea479ec146176b02779a5"));
                break;
            case 40:
                playerInventory.addItem(inventoryManager.getUrlSkull(Messages.getString("blocks_dragon_egg"), "3c151fb54b21fe5769ffb4825b5bc92da73657f214380e5d0301e45b6c13f7d"));
                break;
        }
    }
}