/*
 * Copyright (c) 2023, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package de.eintosti.buildsystem.player;

import com.cryptomorin.xseries.XMaterial;
import de.eintosti.buildsystem.BuildSystem;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.util.InventoryUtils;
import de.eintosti.buildsystem.version.customblocks.CustomBlock;
import de.eintosti.buildsystem.version.util.MinecraftVersion;
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

public class BlocksInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryUtils inventoryUtils;

    public BlocksInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryUtils = plugin.getInventoryUtil();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, Messages.getString("blocks_title"));
        fillGuiWithGlass(player, inventory);

        setCustomBlock(inventory, 1, CustomBlock.FULL_OAK_BARCH);
        setCustomBlock(inventory, 2, CustomBlock.FULL_SPRUCE_BARCH);
        setCustomBlock(inventory, 3, CustomBlock.FULL_BIRCH_BARCH);
        setCustomBlock(inventory, 4, CustomBlock.FULL_JUNGLE_BARCH);
        setCustomBlock(inventory, 5, CustomBlock.FULL_ACACIA_BARCH);
        setCustomBlock(inventory, 6, CustomBlock.FULL_DARK_OAK_BARCH);

        setCustomBlock(inventory, 10, CustomBlock.RED_MUSHROOM);
        setCustomBlock(inventory, 11, CustomBlock.BROWN_MUSHROOM);
        setCustomBlock(inventory, 12, CustomBlock.FULL_MUSHROOM_STEM);
        setCustomBlock(inventory, 13, CustomBlock.MUSHROOM_STEM);
        setCustomBlock(inventory, 14, CustomBlock.MUSHROOM_BLOCK);

        setCustomBlock(inventory, 19, CustomBlock.SMOOTH_STONE);
        setCustomBlock(inventory, 20, CustomBlock.DOUBLE_STONE_SLAB);
        setCustomBlock(inventory, 21, CustomBlock.SMOOTH_SANDSTONE);
        setCustomBlock(inventory, 22, CustomBlock.SMOOTH_RED_SANDSTONE);

        setCustomBlock(inventory, 28, CustomBlock.POWERED_REDSTONE_LAMP);
        setCustomBlock(inventory, 29, CustomBlock.BURNING_FURNACE);
        setCustomBlock(inventory, 30, CustomBlock.PISTON_HEAD);
        setCustomBlock(inventory, 31, CustomBlock.COMMAND_BLOCK);
        setCustomBlock(inventory, 32, CustomBlock.BARRIER);
        setCustomBlock(inventory, 33, CustomBlock.INVISIBLE_ITEM_FRAME);

        setCustomBlock(inventory, 37, CustomBlock.MOB_SPAWNER);
        setCustomBlock(inventory, 38, CustomBlock.NETHER_PORTAL);
        setCustomBlock(inventory, 39, CustomBlock.END_PORTAL);
        setCustomBlock(inventory, 40, CustomBlock.DRAGON_EGG);

        return inventory;
    }

    private void setCustomBlock(Inventory inventory, int position, CustomBlock customBlock) {
        if (MinecraftVersion.getCurrent().isEqualOrHigherThan(customBlock.getVersion())) {
            inventoryUtils.addUrlSkull(inventory, position, Messages.getString(customBlock.getKey()), customBlock.getSkullUrl());
        }
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        int[] glassSlots = {0, 8, 9, 17, 18, 26, 27, 35, 36, 44};
        for (int i : glassSlots) {
            inventoryUtils.addGlassPane(plugin, player, inventory, i);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtils.checkIfValidClick(event, "blocks_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        PlayerInventory playerInventory = player.getInventory();
        switch (event.getSlot()) {
            case 1:
                giveCustomBlock(CustomBlock.FULL_OAK_BARCH, playerInventory);
                break;
            case 2:
                giveCustomBlock(CustomBlock.FULL_SPRUCE_BARCH, playerInventory);
                break;
            case 3:
                giveCustomBlock(CustomBlock.FULL_BIRCH_BARCH, playerInventory);
                break;
            case 4:
                giveCustomBlock(CustomBlock.FULL_JUNGLE_BARCH, playerInventory);
                break;
            case 5:
                giveCustomBlock(CustomBlock.FULL_ACACIA_BARCH, playerInventory);
                break;
            case 6:
                giveCustomBlock(CustomBlock.FULL_DARK_OAK_BARCH, playerInventory);
                break;

            case 10:
                giveCustomBlock(CustomBlock.RED_MUSHROOM, playerInventory);
                break;
            case 11:
                giveCustomBlock(CustomBlock.BROWN_MUSHROOM, playerInventory);
                break;
            case 12:
                giveCustomBlock(CustomBlock.FULL_MUSHROOM_STEM, playerInventory);
                break;
            case 13:
                giveCustomBlock(CustomBlock.MUSHROOM_STEM, playerInventory);
                break;
            case 14:
                giveCustomBlock(CustomBlock.MUSHROOM_BLOCK, playerInventory);
                break;

            case 19:
                giveCustomBlock(CustomBlock.SMOOTH_STONE, playerInventory);
                break;
            case 20:
                giveCustomBlock(CustomBlock.DOUBLE_STONE_SLAB, playerInventory);
                break;
            case 21:
                giveCustomBlock(CustomBlock.SMOOTH_SANDSTONE, playerInventory);
                break;
            case 22:
                giveCustomBlock(CustomBlock.SMOOTH_RED_SANDSTONE, playerInventory);
                break;

            case 28:
                giveCustomBlock(CustomBlock.POWERED_REDSTONE_LAMP, playerInventory);
                break;
            case 29:
                giveCustomBlock(CustomBlock.BURNING_FURNACE, playerInventory);
                break;
            case 30:
                giveCustomBlock(CustomBlock.PISTON_HEAD, playerInventory);
                break;
            case 31:
                giveCustomBlock(CustomBlock.COMMAND_BLOCK, playerInventory);
                break;
            case 32:
                giveCustomBlock(CustomBlock.BARRIER, playerInventory, inventoryUtils.getItemStack(XMaterial.BARRIER, Messages.getString(CustomBlock.BARRIER.getKey())));
                break;
            case 33:
                ItemStack itemStack = inventoryUtils.getItemStack(XMaterial.ITEM_FRAME, Messages.getString(CustomBlock.INVISIBLE_ITEM_FRAME.getKey()));
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
                // Inline imports to allow backwards compatibility
                itemMeta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "invisible-itemframe"), org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                itemStack.setItemMeta(itemMeta);
                giveCustomBlock(CustomBlock.INVISIBLE_ITEM_FRAME, playerInventory, itemStack);
                break;

            case 37:
                giveCustomBlock(CustomBlock.MOB_SPAWNER, playerInventory);
                break;
            case 38:
                giveCustomBlock(CustomBlock.NETHER_PORTAL, playerInventory);
                break;
            case 39:
                giveCustomBlock(CustomBlock.END_PORTAL, playerInventory);
                break;
            case 40:
                giveCustomBlock(CustomBlock.DRAGON_EGG, playerInventory);
                break;
        }
    }

    private void giveCustomBlock(CustomBlock customBlock, Inventory inventory, ItemStack itemStack) {
        if (MinecraftVersion.getCurrent().isEqualOrHigherThan(customBlock.getVersion())) {
            inventory.addItem(itemStack);
        }
    }

    private void giveCustomBlock(CustomBlock customBlock, Inventory inventory) {
        giveCustomBlock(customBlock, inventory, inventoryUtils.getUrlSkull(Messages.getString(customBlock.getKey()), customBlock.getSkullUrl()));
    }
}