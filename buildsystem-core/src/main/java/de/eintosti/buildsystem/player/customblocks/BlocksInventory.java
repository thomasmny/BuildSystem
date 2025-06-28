/*
 * Copyright (c) 2018-2025, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.player.customblocks;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import de.eintosti.buildsystem.BuildSystemPlugin;
import de.eintosti.buildsystem.Messages;
import de.eintosti.buildsystem.util.inventory.BuildSystemHolder;
import de.eintosti.buildsystem.util.inventory.InventoryHandler;
import de.eintosti.buildsystem.util.inventory.InventoryManager;
import de.eintosti.buildsystem.util.inventory.InventoryUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BlocksInventory implements InventoryHandler {

    private final BuildSystemPlugin plugin;
    private final InventoryManager inventoryManager;

    public BlocksInventory(BuildSystemPlugin plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
    }

    public void openInventory(Player player) {
        Inventory inventory = getInventory(player);
        this.inventoryManager.registerInventoryHandler(inventory, this);
        player.openInventory(inventory);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = new BlocksInventoryHolder(player).getInventory();
        fillGuiWithGlass(player, inventory);

        setCustomBlock(inventory, player, 1, CustomBlock.FULL_OAK_BARCH);
        setCustomBlock(inventory, player, 2, CustomBlock.FULL_SPRUCE_BARCH);
        setCustomBlock(inventory, player, 3, CustomBlock.FULL_BIRCH_BARCH);
        setCustomBlock(inventory, player, 4, CustomBlock.FULL_JUNGLE_BARCH);
        setCustomBlock(inventory, player, 5, CustomBlock.FULL_ACACIA_BARCH);
        setCustomBlock(inventory, player, 6, CustomBlock.FULL_DARK_OAK_BARCH);

        setCustomBlock(inventory, player, 10, CustomBlock.RED_MUSHROOM);
        setCustomBlock(inventory, player, 11, CustomBlock.BROWN_MUSHROOM);
        setCustomBlock(inventory, player, 12, CustomBlock.FULL_MUSHROOM_STEM);
        setCustomBlock(inventory, player, 13, CustomBlock.MUSHROOM_STEM);
        setCustomBlock(inventory, player, 14, CustomBlock.MUSHROOM_BLOCK);

        setCustomBlock(inventory, player, 19, CustomBlock.SMOOTH_STONE);
        setCustomBlock(inventory, player, 20, CustomBlock.DOUBLE_STONE_SLAB);
        setCustomBlock(inventory, player, 21, CustomBlock.SMOOTH_SANDSTONE);
        setCustomBlock(inventory, player, 22, CustomBlock.SMOOTH_RED_SANDSTONE);

        setCustomBlock(inventory, player, 28, CustomBlock.POWERED_REDSTONE_LAMP);
        setCustomBlock(inventory, player, 29, CustomBlock.BURNING_FURNACE);
        setCustomBlock(inventory, player, 30, CustomBlock.PISTON_HEAD);
        setCustomBlock(inventory, player, 31, CustomBlock.COMMAND_BLOCK);
        setCustomBlock(inventory, player, 32, CustomBlock.BARRIER);
        setCustomBlock(inventory, player, 33, CustomBlock.INVISIBLE_ITEM_FRAME);

        setCustomBlock(inventory, player, 37, CustomBlock.MOB_SPAWNER);
        setCustomBlock(inventory, player, 38, CustomBlock.NETHER_PORTAL);
        setCustomBlock(inventory, player, 39, CustomBlock.END_PORTAL);
        setCustomBlock(inventory, player, 40, CustomBlock.DRAGON_EGG);
        setCustomBlock(inventory, player, 41, CustomBlock.DEBUG_STICK);

        return inventory;
    }

    private void setCustomBlock(Inventory inventory, Player player, int position, CustomBlock customBlock) {
        inventory.setItem(position, InventoryUtils.createSkull(Messages.getString(customBlock.getKey(), player), Profileable.detect(customBlock.getSkullUrl())));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        int[] glassSlots = {0, 8, 9, 17, 18, 26, 27, 35, 36, 44};
        for (int i : glassSlots) {
            InventoryUtils.addGlassPane(player, inventory, i);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlocksInventoryHolder)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        switch (event.getSlot()) {
            case 1 -> giveCustomBlock(player, CustomBlock.FULL_OAK_BARCH);
            case 2 -> giveCustomBlock(player, CustomBlock.FULL_SPRUCE_BARCH);
            case 3 -> giveCustomBlock(player, CustomBlock.FULL_BIRCH_BARCH);
            case 4 -> giveCustomBlock(player, CustomBlock.FULL_JUNGLE_BARCH);
            case 5 -> giveCustomBlock(player, CustomBlock.FULL_ACACIA_BARCH);
            case 6 -> giveCustomBlock(player, CustomBlock.FULL_DARK_OAK_BARCH);

            case 10 -> giveCustomBlock(player, CustomBlock.RED_MUSHROOM);
            case 11 -> giveCustomBlock(player, CustomBlock.BROWN_MUSHROOM);
            case 12 -> giveCustomBlock(player, CustomBlock.FULL_MUSHROOM_STEM);
            case 13 -> giveCustomBlock(player, CustomBlock.MUSHROOM_STEM);
            case 14 -> giveCustomBlock(player, CustomBlock.MUSHROOM_BLOCK);

            case 19 -> giveCustomBlock(player, CustomBlock.SMOOTH_STONE);
            case 20 -> giveCustomBlock(player, CustomBlock.DOUBLE_STONE_SLAB);
            case 21 -> giveCustomBlock(player, CustomBlock.SMOOTH_SANDSTONE);
            case 22 -> giveCustomBlock(player, CustomBlock.SMOOTH_RED_SANDSTONE);

            case 28 -> giveCustomBlock(player, CustomBlock.POWERED_REDSTONE_LAMP);
            case 29 -> giveCustomBlock(player, CustomBlock.BURNING_FURNACE);
            case 30 -> giveCustomBlock(player, CustomBlock.PISTON_HEAD);
            case 31 -> giveCustomBlock(player, CustomBlock.COMMAND_BLOCK);
            case 32 -> giveCustomBlock(player, InventoryUtils.createItem(XMaterial.BARRIER, Messages.getString(CustomBlock.BARRIER.getKey(), player)));
            case 33 -> {
                ItemStack itemStack = InventoryUtils.createItem(XMaterial.ITEM_FRAME, Messages.getString(CustomBlock.INVISIBLE_ITEM_FRAME.getKey(), player));
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.addEnchant(XEnchantment.UNBREAKING.get(), 1, true);
                itemMeta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "invisible-itemframe"), PersistentDataType.BYTE, (byte) 1
                );
                itemStack.setItemMeta(itemMeta);
                giveCustomBlock(player, itemStack);
            }

            case 37 -> giveCustomBlock(player, CustomBlock.MOB_SPAWNER);
            case 38 -> giveCustomBlock(player, CustomBlock.NETHER_PORTAL);
            case 39 -> giveCustomBlock(player, CustomBlock.END_PORTAL);
            case 40 -> giveCustomBlock(player, CustomBlock.DRAGON_EGG);
            case 41 -> giveCustomBlock(player, InventoryUtils.createItem(XMaterial.DEBUG_STICK, Messages.getString(CustomBlock.DEBUG_STICK.getKey(), player)));
        }
    }

    private void giveCustomBlock(Player player, CustomBlock customBlock) {
        giveCustomBlock(player, InventoryUtils.createSkull(Messages.getString(customBlock.getKey(), player), Profileable.detect(customBlock.getSkullUrl())));
    }

    private void giveCustomBlock(Player player, ItemStack itemStack) {
        player.getInventory().addItem(itemStack);
    }

    private static class BlocksInventoryHolder extends BuildSystemHolder {

        public BlocksInventoryHolder(Player player) {
            super(45, Messages.getString("blocks_title", player));
        }
    }
}